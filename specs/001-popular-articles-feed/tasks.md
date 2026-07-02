---
description: "Task list for Popular Articles Feed"
---

# Tasks: Popular Articles Feed

**Input**: Design documents from `/specs/001-popular-articles-feed/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Included — the feature explicitly requires new integration tests (spec SC-006)
and the project constitution (Principle II) mandates active tests for implemented
endpoints. Test tasks are therefore first-class below.

**Organization**: Tasks are grouped by user story. Because the spec states the P3
create/favorite operations are prerequisites to build and test P1, the shared
persistence + service groundwork lives in Phase 2 (Foundational); each story phase then
owns its distinct read path, pagination behavior, or verification.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1 / US2 / US3 (setup, foundational, polish carry no story label)

## Path Conventions

Single-module Kotlin/Ktor web service. Main code under
`src/main/kotlin/io/realworld/app/`, tests under `src/test/kotlin/io/realworld/app/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm a known-good starting point before changing the scaffold.

- [X] T001 Confirm baseline builds and existing (`@Ignore`d) tests skip cleanly by running `./gradlew clean build` from the repo root; record the skipped-suite count so the Polish phase can verify it is unchanged.

**Checkpoint**: Green baseline established.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Article/favorite/tag persistence and service groundwork that ALL user
stories depend on (the spec folds this groundwork into the feature). No feed ranking,
pagination, or story-specific verification here.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T002 [P] Extend article domain types in `src/main/kotlin/io/realworld/app/domain/Article.kt`: add a create-request validation (require non-blank `title`, `description`, `body`; default `tagList` to empty) mirroring `UserDTO.validRegister()`, and a `slug` derivation helper (lowercase + hyphenate title).
- [X] T003 Define Exposed tables in a new `src/main/kotlin/io/realworld/app/domain/repository/ArticleRepository.kt`: `Articles` (id PK, slug unique, title, description, body, authorId FK→Users.id, createdAt, updatedAt), `ArticleFavorites` (composite PK `(article, user)`), `ArticleTags` (composite PK `(article, tag)`); create all three via `SchemaUtils.create(...)` in the repository `init {}` block (pattern from `UserRepository`).
- [X] T004 Implement `ArticleRepository` persistence methods in `src/main/kotlin/io/realworld/app/domain/repository/ArticleRepository.kt` (depends on T002, T003): `create(article, authorId)` (insert article, derive/persist slug, upsert tag names into existing `Tags`, insert `ArticleTags`), `findBySlug(slug)`, `favorite(userId, slug)` (idempotent insert), `unfavorite(userId, slug)` (delete row), `favoritesCount(articleId)`, `isFavorited(userId, articleId)`, and `tagsOf(articleId)` — all inside `transaction {}`.
- [X] T005 Create `src/main/kotlin/io/realworld/app/domain/service/ArticleService.kt` with a constructor taking `ArticleRepository` and `UserRepository` (depends on T004): implement `create(email, article)`, `favorite(email, slug)`, `unfavorite(email, slug)`, each returning a fully-populated `Article` with `favoritesCount`, the requesting user's `favorited`, `tagList`, and `author` `Profile` including `following` (reuse `UserRepository.findIsFollowUser`).
- [X] T006 Update `src/main/kotlin/io/realworld/app/web/controllers/ArticleController.kt` (depends on T005): un-comment the `ArticleController(private val articleService: ArticleService)` constructor, remove placeholder returns, and implement `create`, `favorite`, `unfavorite` using `ctx.authentication.principal<User>()?.email` and `ctx.respond(ArticleDTO(...))`.
- [X] T007 Wire Kodein in `src/main/kotlin/io/realworld/app/config/ModulesConfig.kt` (depends on T005, T006): bind `ArticleController(instance())`, `ArticleService(instance(), instance())`, and `ArticleRepository()` in `articleModule` (resolves the existing `UserRepository` singleton across modules).
- [X] T007a Add exception→HTTP-status mapping so domain errors return correct codes (constitution Principle III): populate `src/main/kotlin/io/realworld/app/web/ErrorExceptionMapping.kt` with a `StatusPages` configuration function (or extend the existing `install(StatusPages)` block in `src/main/kotlin/io/realworld/app/config/AppConfig.kt`) mapping `NotFoundException`→`404`, `UnauthorizedException`→`401`, and `IllegalArgumentException`→`422`, each returning the existing `ErrorResponse` shape, while keeping the generic `Exception`→`500` fallback last. **Blocks the 404/422 expectations in T017, T017a, and T018.**
- [X] T008 [P] Add integration test helpers in `src/test/kotlin/io/realworld/app/web/util/HttpUtil.kt` targeting the real router paths: create an article via `POST /articles`, favorite/unfavorite via `POST|DELETE /articles/{slug}/favorite`, and register+login a distinct second user (so favorites can be attributed to multiple users). Also fix the existing `createArticle` helper, which posts to `/api/articles` — inconsistent with the prefix-less router and the `/users` login helper — to `POST /articles`. Do NOT modify existing `@Ignore`d suites.

**Checkpoint**: Articles can be created and favorited/unfavorited through the service +
existing routes; foundation ready for story work.

---

## Phase 3: User Story 1 - Discover most-favorited articles (Priority: P1) 🎯 MVP

**Goal**: Authenticated readers GET the popular feed and receive articles ordered by
favorite count descending (newest-first on ties), in the standard multiple-articles shape.

**Independent Test**: Create several articles, favorite them unequal numbers of times as
distinct users, then GET `/articles/feed/popular` with a valid token and assert `200`
with articles ordered by `favoritesCount` descending and a correct total `articlesCount`.

### Tests for User Story 1

- [ ] T009 [P] [US1] Create active (NOT `@Ignore`d) `src/test/kotlin/io/realworld/app/web/controllers/PopularArticlesFeedControllerTest.kt` using `AppRule` + `HttpUtil` covering: (a) ranking happy path — articles sorted by `favoritesCount` desc, (b) missing token → `401`, (c) empty feed → `200` with `articles: []` and `articlesCount: 0`. Assert against real path `/articles/feed/popular`.

### Implementation for User Story 1

- [X] T010 [US1] Implement `ArticleRepository.findPopular(limit, offset)` in `src/main/kotlin/io/realworld/app/domain/repository/ArticleRepository.kt` (depends on T004): single grouped `Articles LEFT JOIN ArticleFavorites` query, `COUNT(ArticleFavorites.user)` as favorites, `ORDER BY favoritesCount DESC, Articles.createdAt DESC`, `LIMIT limit OFFSET offset`; plus `countAll()` returning the total article count (not page size).
- [X] T011 [US1] Implement `ArticleService.findPopular(email, limit, offset)` in `src/main/kotlin/io/realworld/app/domain/service/ArticleService.kt` (depends on T010, T005): map the ranked page to personalized `Article` objects (set-based `favorited` for the page's ids and `author.following`), and return `ArticlesDTO(articles, totalCount)` using `countAll()`.
- [X] T012 [US1] Add `popularFeed(ctx)` handler to `src/main/kotlin/io/realworld/app/web/controllers/ArticleController.kt` (depends on T011): resolve email from the JWT principal and `ctx.respond(ArticlesDTO(...))`.
- [X] T013 [US1] Register `get("feed/popular") { articleController.popularFeed(this.context) }` inside the existing `authenticate {}` block of `Routing.articles(...)` in `src/main/kotlin/io/realworld/app/web/Router.kt` (depends on T012). Add an inline code comment directly above the route stating that the endpoint is mounted **prefix-less** (`/articles/feed/popular`) to follow the existing router convention (all working `User`/`Tag` routes omit `/api`), whereas IDEA.md and the RealWorld spec write it as `/api/articles/feed/popular`. This comment is the deliberate prompt for the `/api`-deviation note in the PR description (see Notes).

**Checkpoint**: MVP — the popular feed returns correctly ranked articles behind auth and
its US1 tests pass.

---

## Phase 4: User Story 2 - Page through the feed (Priority: P2)

**Goal**: Clients page the feed with `limit`/`offset`, receiving only the requested
window with stable ordering across pages and consistent handling of invalid params.

**Independent Test**: With more articles than a chosen page size, request successive
windows via `?limit=&offset=` and assert each returns the correct slice with stable
ranking; request `?limit=abc` and assert a `200` safe-default window.

### Tests for User Story 2

- [ ] T014 [P] [US2] Extend `src/test/kotlin/io/realworld/app/web/controllers/PopularArticlesFeedControllerTest.kt` with: (a) `?limit=1&offset=1` returns the second-ranked article only with order stable across pages, and (b) `?limit=abc` returns `200` with the default window (limit 20).

### Implementation for User Story 2

- [X] T015 [US2] In `ArticleController.popularFeed` (`src/main/kotlin/io/realworld/app/web/controllers/ArticleController.kt`, depends on T012): read `limit`/`offset` from `ctx.request.queryParameters`, parse with `toIntOrNull()` defaulting to 20/0, and clamp negatives to 0 (safe-default behavior per research D3).
- [X] T016 [US2] Confirm `findPopular` applies the `limit`/`offset` window while `articlesCount` remains the full total, in `src/main/kotlin/io/realworld/app/domain/repository/ArticleRepository.kt` and `.../service/ArticleService.kt` (depends on T010, T011).

**Checkpoint**: Feed pagination works and invalid params are handled safely; US1 + US2
tests pass.

---

## Phase 5: User Story 3 - Supporting data operations (Priority: P3)

**Goal**: Authenticated users can create articles and favorite/unfavorite them so the
feed has data to rank, with correct favorite-count changes and error handling.

**Independent Test**: Create an article, favorite it (count +1), unfavorite it (count
−1), favorite twice (no double-count), and favorite a missing slug (`404`).

**Note**: The create/favorite/unfavorite mechanics were implemented in Phase 2 because
US1/US2 depend on them; this phase adds the user-facing verification and the not-found
handling that only this story requires.

### Tests for User Story 3

- [ ] T017 [P] [US3] Create active `src/test/kotlin/io/realworld/app/web/controllers/ArticleFavoritesControllerTest.kt` (`AppRule` + `HttpUtil`) covering: create persists and the article appears in the feed; favorite increments `favoritesCount` and sets `favorited=true`; unfavorite decrements it; repeating a favorite by the same user does not double-count; favorite/unfavorite on a non-existent slug returns `404`.
- [ ] T017a [P] [US3] Add a validation-error-path test (constitution Principle II) asserting that `POST /articles` with a missing required field (e.g., blank `title`) returns `422` (not `500`) — in `src/test/kotlin/io/realworld/app/web/controllers/ArticleFavoritesControllerTest.kt` (depends on T007a for the `IllegalArgumentException`→`422` mapping).

### Implementation for User Story 3

- [X] T018 [US3] Ensure `favorite`/`unfavorite` on a non-existent slug raise `NotFoundException` in `src/main/kotlin/io/realworld/app/domain/service/ArticleService.kt` (and/or `ArticleRepository.findBySlug` guard), reusing the existing `domain/exceptions/NotFoundException.kt`; relies on T007a to surface it as `404` (depends on T005, T007a).

**Checkpoint**: All three user stories are independently verifiable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validation and quality gates across the feature.

- [X] T019 Run `./gradlew clean build` from the repo root; confirm the new `PopularArticlesFeedControllerTest` and `ArticleFavoritesControllerTest` pass and that the pre-existing `@Ignore`d suites' skipped count is unchanged from T001.
- [X] T020 [P] Review `ArticleRepository.findPopular` for set-based access (no per-row favorite/follow lookups in a loop) and confirm `articlesCount` returns the total, per constitution Principle IV — in `src/main/kotlin/io/realworld/app/domain/repository/ArticleRepository.kt`.
- [X] T021 [P] Execute the manual smoke test in `specs/001-popular-articles-feed/quickstart.md` against a running `./gradlew run` server and confirm the popular feed ranks the favorited article first.

---

## Phase 7: CI/CD Pipeline (Cross-Cutting — Exercise Part 4)

**Purpose**: Bring the GitHub Actions pipeline up to the exercise requirements. Ships on the
same feature branch / single PR — this is not a separate Spec Kit feature.

- [X] T022 Update `.github/workflows/gradle.yml`: replace the single JDK 16 job with a `strategy.matrix.java: [17, 21]` build using `actions/setup-java@v4` (temurin) and `gradle/actions/setup-gradle@v3` for dependency caching; trigger on `push` and `pull_request` to the default branch (`master`/`main`); run `./gradlew build test`.
- [X] T023 Add JUnit test reporting to the workflow: publish `build/test-results/**/*.xml` via `dorny/test-reporter@v1` (`reporter: java-junit`) with `if: always()` so results annotate the PR/checks page on both matrix legs (depends on T022).
- [X] T024 [P] (Bonus) Add `.github/workflows/spec-api.yml` that boots `./gradlew run`, waits for `http://localhost:8080` to accept requests, then runs `spec-api/run-api-tests.sh` (Newman) against the live server.

**Checkpoint**: CI is green on JDK 17 and 21, JUnit results annotate the PR, and Gradle
caching is active on reruns.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories.
- **User Stories (Phase 3–5)**: All depend on Foundational.
  - US1 (P1) is the MVP and should be completed first.
  - US2 (P2) builds on the US1 feed handler.
  - US3 (P3) verifies the Phase 2 operations and adds not-found handling; independent of
    US1/US2 code paths.
- **Polish (Phase 6)**: Depends on all targeted stories.
- **CI/CD (Phase 7)**: Independent of feature code and can proceed at any time, but the
  final pipeline run should validate the completed test suite. T024 is optional (bonus).

### Task-Level Dependencies

- T002, T003 → T004 → T005 → {T006 → T007}; T007 → T007a
- T004 → T010 → T011 → T012 → T013 (US1)
- T012 → T015; {T010, T011} → T016 (US2)
- T005 → T018; T007a → {T017a, T018} (US3 404/422 codes depend on the error mapping)
- Tests T009 (US1), T014 (US2), T017/T017a (US3) require Phase 2 complete (esp. T008 helpers and, for 404/422 assertions, T007a).
- T019/T020/T021 (Polish) require Phases 3–5 complete.

### Parallel Opportunities

- **Foundational**: T002 and T008 are `[P]` (different files); T003→T004 are sequential
  (same file).
- **Story tests**: T009, T014, T017 target different concerns; T009 and T017 are separate
  files (`[P]`), T014 extends T009's file (sequential after T009).
- **Polish**: T020 and T021 are `[P]`.
- With foundation done, US1 must precede US2 (shared handler file), while US3 can proceed
  in parallel with US1/US2 (touches service + a separate test file).

---

## Parallel Example: Foundational Phase

```bash
# T002 and T008 touch different files and can run together:
Task: "Extend article domain types in domain/Article.kt"
Task: "Add integration test helpers in test/.../util/HttpUtil.kt"
# Then the repository chain runs sequentially (same file):
Task: "Define Exposed tables in domain/repository/ArticleRepository.kt"  # T003
Task: "Implement ArticleRepository persistence methods"                  # T004
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1 Setup → 2. Phase 2 Foundational (CRITICAL) → 3. Phase 3 US1 →
   **STOP and VALIDATE** the popular feed independently → demo.

### Incremental Delivery

1. Setup + Foundational → foundation ready.
2. US1 → ranked feed behind auth (MVP) → validate/demo.
3. US2 → pagination + safe invalid-param handling → validate/demo.
4. US3 → verified create/favorite/unfavorite with 404 handling → validate/demo.

### Parallel Team Strategy

After Foundational: one developer takes US1→US2 (shared feed handler), another takes US3
(service not-found + supporting tests) concurrently.

---

## Notes

- `[P]` = different files, no incomplete dependencies.
- Endpoints use the live prefix-less router (`/articles/...`); the `/api` paths in the
  existing `@Ignore`d tests are aspirational (research D1).
- Read pagination from `request.queryParameters`, not `call.parameters` (research D2).
- New test classes MUST be active (no `@Ignore`); do not enable other suites (constitution
  Principle II).
- Per constitution governance, the PR description MUST document two deviations: (1) the
  prefix-less path choice for `/articles/feed/popular` vs. IDEA.md's `/api/...` — surfaced
  by the T013 endpoint comment; and (2) leaving the legacy `ArticleControllerTest`
  `@Ignore`d because its other endpoints (get/update/delete/findBy/feed) remain out of scope.
- Commit after each task or logical group.
