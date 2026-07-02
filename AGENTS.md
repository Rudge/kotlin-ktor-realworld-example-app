# AGENTS.md

This file describes the conventions and documentation structure used by AI agents (e.g., GitHub Copilot) working in this repository.

## Repository Overview

A Kotlin + Ktor scaffold of the [RealWorld](https://github.com/gothinkster/realworld) spec. The routing, auth, and DTO layers are in place; the Article/Favorites layer is now implemented. Stack:

- **Kotlin** — primary language
- **Ktor** — web framework
- **Kodein** — dependency injection
- **Exposed** — SQL ORM
- **H2** — embedded database
- **JUnit + Unirest** — testing

## Current Implementation State

### Fully implemented (as of PR #32 / `feature/article-favorites-count`)

- **User** layer — registration, login, JWT auth, profile follow/unfollow.
- **Tag** layer — tag listing.
- **Article** layer — `ArticleRepository`, `ArticleService`, `ArticleController`:
  - `POST /articles` — create article (requires auth; title/description/body required; slug derived from title with collision suffix).
  - `POST /articles/{slug}/favorite` — favorite (idempotent composite PK).
  - `DELETE /articles/{slug}/favorite` — unfavorite.
  - `GET /articles/feed/popular` — articles ranked by favorites count desc, newest-first tie-break, `limit`/`offset` pagination (defaults 20/0), returns personalized `favorited` + `author.following` flags. **Requires auth.**
- **Error mapping** — `NotFoundException`→404, `UnauthorizedException`→401, `IllegalArgumentException`→422, generic→500 (stable non-leaking message).

### Stubbed / not implemented

- `GET /articles`, `GET /articles/feed` — stub handlers; return empty list.
- `GET /articles/{slug}`, `PUT /articles/{slug}`, `DELETE /articles/{slug}` — stubs.
- `CommentController`, `ProfileController` — stubs; handlers return placeholder DTOs.

### Tests

All five original controller test classes are `@Ignore`d — "green" does not mean coverage.
New integration tests for the Article/Favorites/popular-feed feature are expected via **Copilot coding agent PRs** (issues #10/#15/#18/#19 assigned to Copilot).

> **Important for Copilot test agents:** PRs must target `master` (post-PR #32 merge) to have the Article layer available. Assert against **prefix-less paths** (`/articles/...`, not `/api/articles/...`) — the router uses no `/api` prefix. Use `AppRule` + `HttpUtil`. Keep test classes **active** (no `@Ignore`). Correct status codes: 401 (unauth), 404 (not found), 422 (validation).

## Route convention

All routes are mounted **prefix-less** — no `/api` wrapper:

```
/users          /user           /profiles/{username}
/articles       /articles/feed  /articles/feed/popular
/articles/{slug}/favorite       /tags
```

The exercise brief uses `/api/...` but the running app and all working tests (UserControllerTest, HttpUtil) do not. Adding `/api` would break the existing convention.

## Session documentation

Session notes live in the **gitignored `work/` directory** (not committed):

| File | Purpose |
|------|---------|
| `work/PROCESS.md` | Step-by-step log of actions taken during the session |
| `work/LESSONS_LEARNED.md` | Issues encountered and their resolutions |
| `work/NEXT_STEPS.md` | Handoff doc summarizing outstanding work for future sessions |
| `work/GAP_ANALYSIS.md` | Exercise requirements vs. current state |

## Build & Test Commands

```bash
# Build and test
./gradlew clean build

# Run the server (port 8080)
./gradlew run

# Run targeted test class
./gradlew test --tests '*PopularArticlesFeedControllerTest'

# Run RealWorld API spec tests (server must be running; some stubs will fail)
APIURL=http://localhost:8080 ./spec-api/run-api-tests.sh
```

## Known issues / lessons learned

- **`deleteWhere` `eq` unresolved (Exposed 0.37.3+):** wrap the predicate in `with(it) { ... }`.
- **`ctx.parameters` vs `ctx.request.queryParameters`:** `ctx.parameters` only returns path params; query-string values require `ctx.request.queryParameters`.
- **H2 `GROUP BY`:** select only grouped/aggregated columns in the `findPopular` query to avoid H2 "column must appear in GROUP BY" errors.
- **Slug uniqueness:** `create()` derives a slug from the title and suffixes `-N` on collision inside the same transaction.
- **`UserDTO.validRegister()/validLogin()` (fixed):** original code had inverted guards requiring *blank* credentials. Fixed to `!isNullOrBlank()`.

## Code Structure

```
src/main/kotlin/io/realworld/app/
├── App.kt                  # Entry point
├── config/
│   ├── AppConfig.kt        # Ktor setup, JWT auth, StatusPages, routing
│   ├── DbConfig.kt         # HikariCP + H2 setup
│   └── ModulesConfig.kt    # Kodein DI bindings
├── domain/
│   ├── Article.kt          # Article, ArticleDTO, ArticlesDTO + validCreate()
│   ├── User.kt             # User, UserDTO + validRegister/Login/Update()
│   ├── Profile.kt          # Profile, ProfileDTO
│   ├── Tag.kt              # TagDTO
│   ├── exceptions/         # NotFoundException, UnauthorizedException
│   ├── repository/
│   │   ├── ArticleRepository.kt  # Articles, ArticleFavorites, ArticleTags tables
│   │   ├── UserRepository.kt     # Users, Follows tables
│   │   └── TagRepository.kt      # Tags table
│   └── service/
│       ├── ArticleService.kt     # create, favorite, unfavorite, findPopular
│       ├── UserService.kt        # create, authenticate, getByEmail, follow/unfollow
│       └── TagService.kt         # findAll
├── ext/
│   └── String.kt           # isEmailValid(), toSlug()
├── utils/
│   ├── JwtProvider.kt      # JWT create/verify
│   └── Cipher.kt           # HMAC algorithm
└── web/
    ├── controllers/        # Route handlers (ArticleController, UserController, etc.)
    ├── Router.kt           # Route definitions (all prefix-less)
    └── ErrorExceptionMapping.kt  # StatusPages exception→HTTP status mapping
```
