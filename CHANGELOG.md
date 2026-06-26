# Changelog

## [Unreleased] — feature/article-favorites-count

### Summary

This branch restores the full Realworld API implementation and makes the complete test suite green (25/25 tests passing). The project had been partially implemented — user registration/login worked, but articles, profiles, comments, and tags were all stubs. All 25 integration tests were annotated `@Ignore`.

---

### Dependency fixes (`build.gradle`)

The original dependencies referenced artifacts that are no longer available, blocking `./gradlew build`.

- **Exposed** `0.14.1 → 0.30.2` — the monolithic `org.jetbrains.exposed:exposed` artifact was only published to JCenter (now shut down). Replaced with the Maven Central modular split: `exposed-core`, `exposed-dao`, `exposed-jdbc`.
- **H2** `2.2.224 → 1.4.200` — H2 2.x broke Exposed 0.30.x's schema introspection query against `INFORMATION_SCHEMA.SETTINGS`, causing a `JdbcSQLSyntaxErrorException` on startup. H2 1.4.200 is the last 1.x release and is compatible.
- **JUnit / Unirest** — changed `testCompile` to `testImplementation` (Gradle 7+ API).

---

### Bug fixes

**`UserDTO` validation (`domain/User.kt`)**

`validRegister()`, `validLogin()`, and `validToUpdate()` used `isNullOrBlank()` where they should have used `!isNullOrBlank()` for required fields. This caused every registration and login attempt to return HTTP 500, making all authenticated endpoints unreachable.

- `validRegister()` — now correctly requires password and username to be non-blank.
- `validLogin()` — now correctly requires password to be non-blank.
- `validToUpdate()` — simplified to only require a valid email; all other fields are optional in an update.

**Feed test (`ArticleControllerTest.kt`)**

The `get all articles of feed` test used `appRule.http` (the article creator's client) to call `GET /api/articles/feed`, but the creator follows nobody. The correct client is `http` (the follower's client, `celeb_username`), whose feed contains the followed user's articles.

---

### Route changes (`web/Router.kt`)

All routes now carry the `/api` prefix required by the Realworld spec:

| Before | After |
|---|---|
| `POST /users` | `POST /api/users` |
| `POST /users/login` | `POST /api/users/login` |
| `GET /user` | `GET /api/user` |
| `PUT /user` | `PUT /api/user` |
| `GET /profiles/{username}` | `GET /api/profiles/{username}` |
| `GET /articles` | `GET /api/articles` |
| `GET /tags` | `GET /api/tags` |
| … | … |

The article sub-routes were also restructured to use per-route `authenticate`/`authenticate(optional = true)` blocks instead of a single required-auth wrapper that incorrectly blocked unauthenticated `GET /api/articles` requests.

**`HttpUtil.kt` and `UserControllerTest.kt`** updated to use the `/api/...` paths.

---

### New implementations

#### `domain/repository/ArticleRepository.kt` (new)

Defines three tables:

- `Articles` — id (auto-increment PK), slug (unique index), title, description, body, authorId, createdAt, updatedAt.
- `ArticleTags` — articleId + tag composite PK. Populated on article create; drives `GET /api/tags`.
- `Favorites` — articleId + userId composite PK.

Key operations: `create`, `findBy` (tag/author/favorited filters), `findFeed` (articles from followed users), `findBySlug`, `update`, `delete`, `favorite`, `unfavorite`.

Slug generation: title lowercased, spaces replaced with hyphens, non-alphanumeric characters stripped.

> **H2 compatibility note:** H2 1.4.200 throws a `NullPointerException` inside `MVTable.getConstraints()` when querying a table that has zero constraints (no PK, no FK, no index). Both `ArticleTags` and `Favorites` required an explicit `override val primaryKey = PrimaryKey(...)` to avoid this.

#### `domain/service/ArticleService.kt` (new)

Business logic layer wrapping `ArticleRepository`. Handles slug generation (via `String.toSlug()`) and delegates user lookups to `UserRepository` for author resolution and favorite operations.

#### `domain/repository/CommentRepository.kt` (new)

Defines `Comments` table — id (auto-increment PK), body, authorId, articleSlug, createdAt, updatedAt.

Operations: `add`, `findBySlug`, `delete` (by id + slug).

#### `domain/service/CommentService.kt` (new)

Business logic wrapping `CommentRepository`. Resolves author by email via `UserRepository` before delegating to the repository.

---

### Controller implementations

#### `ProfileController.kt`

Was a no-op stub. Now wired to `UserService` (which already had `getProfileByUsername`, `follow`, and `unfollow`). Constructor updated from `ProfileController()` to `ProfileController(userService: UserService)`.

#### `ArticleController.kt`

Was a stub returning empty/null responses. Now fully implemented with all eight actions (`findBy`, `feed`, `get`, `create`, `update`, `delete`, `favorite`, `unfavorite`). Query parameters (`tag`, `author`, `favorited`, `limit`, `offset`) are read from `ctx.request.queryParameters` (not `ctx.parameters`, which only carries path segments). All methods are `suspend` to call `ctx.respond(...)`.

#### `CommentController.kt`

Was a no-op stub. Now wired to `CommentService` with `add`, `findBySlug`, and `delete`.

---

### Dependency injection (`config/ModulesConfig.kt`)

Added bindings for the new types:

```
articleModule  → ArticleController(ArticleService), ArticleService(ArticleRepository, UserRepository), ArticleRepository()
profileModule  → ProfileController(UserService)          [UserService already bound in userModule]
commentModule  → CommentController(CommentService), CommentService(CommentRepository, UserRepository), CommentRepository()
```

---

### Test infrastructure (`web/rules/AppRule.kt`)

All test classes share the same named H2 in-memory database (`jdbc:h2:mem:DATABASE_TO_UPPER=false`) because `ModulesConfig` is a Kotlin `object` (initialized once per JVM). Without cleanup, each test's `createUser()` / `createArticle()` call would fail with duplicate-key errors on the second run.

Added `cleanupDb()` called inside `before()` (after server start): deletes all rows from every table before each test, giving each test a clean slate without requiring a separate database connection per test class.

---

### Tests unskipped

Removed `@Ignore` from all five test classes:

- `UserControllerTest` (4 tests)
- `ArticleControllerTest` (14 tests)
- `ProfileControllerTest` (3 tests)
- `CommentControllerTest` (3 tests)
- `TagControllerTest` (1 test)

**Result: 25/25 tests pass.**

---

### New feature — `GET /api/articles/feed/popular`

Implements Option A from the exercise spec: an endpoint that returns all articles sorted by number of favorites descending, with `limit` and `offset` pagination. Authentication is optional — when a valid token is present the `favorited` field reflects whether the current user has favorited each article; when no token is present the endpoint is still accessible.

#### Implementation (controller → service → repository)

**`ArticleRepository.findPopular(currentUserId, limit, offset)`**

Loads all article rows, counts favorites for each via `Favorites.select { Favorites.articleId eq id }.count()` inside a single transaction, sorts descending, applies `drop(offset).take(limit)`, then maps each row through the existing `toArticle()` helper (which resolves author, tags, favoritesCount, and the favorited flag).

**`ArticleService.findPopular(currentUserId, limit, offset)`**

Thin delegation layer — calls `articleRepository.findPopular(...)` and returns the result. Follows the same pattern as the existing `findBy` and `findFeed` methods.

**`ArticleController.popular(ctx: ApplicationCall)`**

Reads `limit` (default 20) and `offset` (default 0) from `ctx.request.queryParameters`, resolves the optional principal via `ctx.authentication.principal<User>()`, and responds with `ArticlesDTO(articles, articles.size)`.

**`Router.kt`**

Added `get("feed/popular") { articleController.popular(this.context) }` to the existing `authenticate(optional = true)` block inside `route("api/articles")`. Ktor treats the two-segment literal path `"feed/popular"` as higher quality than the `{slug}` parameter route, so `/api/articles/feed/popular` resolves correctly without conflicting with `/api/articles/feed` or `/api/articles/{slug}`.

#### Tests — `PopularArticleControllerTest.kt` (5 tests)

| Test | What it covers |
|---|---|
| `get popular articles returns empty list when no articles exist` | Boundary case — empty database returns `HTTP 200` with `articles: []` and `articlesCount: 0`; also acts as the unauthenticated baseline (no token sent) |
| `get popular articles sorted by favorites count descending` | Happy path — creates two articles, favorites one, asserts the favorited article is first in the response and `favoritesCount > 0`; the unfavorited article is last with `favoritesCount == 0` |
| `get popular articles without authentication returns 200` | Auth boundary (no token) — confirms optional auth works; unauthenticated callers receive `HTTP 200` with the full article list |
| `get popular articles with malformed token returns 401 not 500` | Hard error case — sends `Authorization: Token invalid.jwt.token`; Ktor 1.2.3 rejects a present-but-invalid token with `HTTP 401` even when auth is optional, and the server must not return a `5xx` |
| `get popular articles respects limit parameter` | Pagination — creates 3 articles, requests `?limit=2`, asserts exactly 2 articles returned and `articlesCount == 2` |

**Result: 30/30 tests pass** (25 original + 5 new).

---

### Build output improvements (`build.gradle`)

Added a `test { }` block with two enhancements:

- **`testLogging { events "passed", "skipped", "failed" }`** — prints each test class and method name with its result during every run. Previously Gradle only printed `BUILD SUCCESSFUL` with no per-test detail.
- **`afterSuite { desc, result -> if (!desc.parent) { println ... } }`** — prints a single summary line after all tests complete: `Total: 30 tests — 30 passed, 0 failed, 0 skipped`. The `!desc.parent` guard prevents a subtotal line from appearing after each test class.

Command to force a visible run (bypasses Gradle's UP-TO-DATE cache):

```bash
./gradlew cleanTest test
```

---

### OpenAPI spec (`openapi.yaml`)

A hand-written OpenAPI 3.0.3 spec covering all 21 endpoints across 5 tags (Users, Profiles, Articles, Comments, Tags). Key details:

- Security scheme defined as `apiKey` in header with name `Authorization`, accurately modeling the `Token <jwt>` prefix used by this app (not the standard `Bearer` scheme).
- Optional-auth endpoints use `security: [{}, {TokenAuth: []}]`; required-auth endpoints use `security: [{TokenAuth: []}]`.
- `GET /api/articles/feed/popular` is marked `★ NEW` and includes a worked example response.
- `articlesCount` annotated to clarify it equals the page size returned, not the total count in the database.

---

### GitHub Actions CI/CD (`.github/workflows/gradle.yml`)

Rewrote the default Gradle starter workflow to satisfy Part 4 requirements (multi-JDK matrix and bonus spec-test run deferred):

| Change | Reason |
|---|---|
| `gradle/gradle-build-action@v2` → `gradle/actions/setup-gradle@v3` | Old action deprecated; new one caches Gradle wrapper and all downloaded dependencies automatically |
| `actions/checkout@v3` / `setup-java@v3` → `v4` | v3 uses deprecated Node 16 runner |
| JDK `16` → `17` | 16 is non-LTS and EOL; 17 is the current LTS |
| `./gradlew build` → `build -x test` then `test` separately | Keeps build and test as distinct steps; ensures JUnit XML is written fresh before the reporter reads it |
| Added `dorny/test-reporter@v1` with `if: always()` | Posts all 30 test names as inline annotations on the PR diff; runs even when tests fail so failures are annotated |
| Added `checks: write` + `pull-requests: write` permissions | Required by `dorny/test-reporter` to create check runs and post PR comments |
