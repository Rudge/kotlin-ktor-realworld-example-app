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
