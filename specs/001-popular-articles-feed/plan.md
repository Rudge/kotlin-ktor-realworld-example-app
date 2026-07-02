# Implementation Plan: Popular Articles Feed

**Branch**: `001-popular-articles-feed` | **Date**: 2026-07-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-popular-articles-feed/spec.md`

## Summary

Add an authenticated `GET /articles/feed/popular` endpoint that returns articles ranked
by favorite count (descending, newest-first tie-break) in the standard RealWorld
multiple-articles shape with `limit`/`offset` pagination. Because the repo is a scaffold
where the article/favorite layers do not exist, this feature first builds the missing
persistence + service layer — an `Articles` table, an `ArticleFavorites` join table, tag
persistence, and working create-article and favorite/unfavorite behavior — following the
existing `UserRepository`/`UserService`/Kodein pattern, then wires the popular feed on top.

## Technical Context

**Language/Version**: Kotlin 1.6.21 (JVM, toolchain Java 21)

**Primary Dependencies**: Ktor 1.6.8 (server-cio/netty, jackson, auth-jwt), Kodein-DI
6.1.0, Exposed 0.41.1 (core/dao/jdbc), HikariCP 5.1.0, H2 2.2.224

**Storage**: H2 in-memory database (`jdbc:h2:mem:`) via Exposed, pooled with HikariCP

**Testing**: JUnit 4.13.2 + Unirest 1.4.9 integration tests driven through `AppRule`
(boots the real server) and `HttpUtil`

**Target Platform**: JVM server, HTTP on port 8080

**Project Type**: Single-module web service (RealWorld API)

**Performance Goals**: Popular feed served with set-based SQL (no N+1); ranking + counts
computed via grouped join; pagination bounds every response

**Constraints**: Reuse existing JWT auth and RealWorld DTO shapes; pooled `DataSource`
only (no ad-hoc connections); new tests must be ACTIVE while pre-existing `@Ignore`d
suites stay skipped

**Scale/Scope**: One new endpoint plus its supporting article/favorite/tag persistence
and service layer; single-node embedded DB (demo/scaffold scale)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution v1.0.0 — evaluated against the four core principles:

| Principle | Assessment | Status |
|-----------|------------|--------|
| I. Code Quality & Consistency | New `ArticleRepository`/`ArticleService`/`ArticleController` follow the `User`/`Tag` layered pattern; wired via Kodein; controller carries no persistence/business logic; `ArticleController`'s real service-injected constructor is un-commented instead of returning placeholder DTOs. | PASS |
| II. Testing Standards (NON-NEGOTIABLE) | A new **active** (non-`@Ignore`d) integration test class covers the popular feed (happy path + 401 + invalid-param + pagination + empty). Pre-existing `@Ignore`d suites remain skipped and untouched. No test weakened to force green. | PASS |
| III. User Experience Consistency | Returns standard `ArticlesDTO` (`articles` + total `articlesCount`); auth via existing JWT `authenticate {}`; personalized `favorited`/`author.following` consistent with RealWorld; errors mapped to correct status codes (`NotFoundException`→404, etc.) via `ErrorExceptionMapping`/`StatusPages` (task T007a — the scaffold's default maps everything to 500). | PASS |
| IV. Performance Requirements | Ranking via a single grouped `LEFT JOIN` count query; per-user `favorited`/`following` resolved with set-based queries (no per-row lookups); mandatory `limit`/`offset`; pooled HikariCP `DataSource` reused. | PASS |

**Initial gate: PASS** (no violations).

**Post-Design re-check (after Phase 1): PASS** — the data model and contracts introduce no
new frameworks, keep the layered pattern, and preserve set-based access + pagination.
Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/001-popular-articles-feed/
├── plan.md              # This file (/speckit.plan command output)
├── spec.md              # Feature specification
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── popular-feed.openapi.yaml
└── checklists/
    └── requirements.md  # /speckit.specify quality checklist
```

### Source Code (repository root)

```text
src/main/kotlin/io/realworld/app/
├── config/
│   └── ModulesConfig.kt          # MODIFY: bind ArticleController(service), ArticleService, ArticleRepository
├── domain/
│   ├── Article.kt                # REUSE/EXTEND: Article, ArticleDTO, ArticlesDTO (+ create/validation DTO)
│   ├── repository/
│   │   └── ArticleRepository.kt  # NEW: Articles + ArticleFavorites + ArticleTags tables, popular query, favorite/unfavorite
│   └── service/
│       └── ArticleService.kt     # NEW: create, favorite, unfavorite, findPopular (builds personalized DTOs)
└── web/
    ├── Router.kt                 # MODIFY: add GET articles/feed/popular under authenticate
    └── controllers/
        └── ArticleController.kt  # MODIFY: un-comment service constructor; implement create/favorite/unfavorite/popularFeed

src/test/kotlin/io/realworld/app/web/
├── util/HttpUtil.kt              # MODIFY: helpers targeting real /articles paths + favorite-as-user
└── controllers/
    └── PopularArticlesFeedControllerTest.kt  # NEW (ACTIVE): feed ordering, 401, invalid-param, pagination, empty
```

**Structure Decision**: Single-module web service. The feature extends the existing
`domain/repository` -> `domain/service` -> `web/controllers` layering wired by
`config/ModulesConfig.kt`, exactly mirroring the implemented `User` and `Tag` slices. No
new module or project type is introduced. Endpoints are mounted under the existing
prefix-less router (`/articles/...`), consistent with the live `User`/`Tag` routes (see
research.md for the `/api` discrepancy decision).

## Complexity Tracking

> No constitution violations — no entries required.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| (none)    | -          | -                                    |
