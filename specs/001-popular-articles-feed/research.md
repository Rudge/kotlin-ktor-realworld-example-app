# Phase 0 Research: Popular Articles Feed

All Technical Context items are known from the existing codebase; there were no
`NEEDS CLARIFICATION` markers left by the spec. This document records the design
decisions that resolve ambiguities discovered while reading the scaffold.

## D1. Route path and the `/api` prefix discrepancy

- **Decision**: Mount the endpoint as `GET /articles/feed/popular`, added inside the
  existing `Routing.articles(...)` block under the `authenticate {}` guard, as a sibling
  of the current `get("feed")`.
- **Rationale**: The live, working `User`/`Tag` routes are registered without an `/api`
  prefix (`route("users")`, `route("user")`, `route("tags")`), and the working
  `UserControllerTest` calls `/users`, `/user`. Only commented-out/`@Ignore`d code
  references `/api/...`. Matching the real router keeps the feature consistent with the
  implemented reference layers and avoids restructuring every route.
- **Alternatives considered**:
  - *Introduce an `/api` prefix wrapper* to match the RealWorld spec literally — rejected:
    it would move all existing routes, break the working `User`/`Tag` endpoints and their
    tests, and exceed this feature's scope.
  - *Add only the popular route under `/api`* — rejected: inconsistent with sibling
    article routes and confusing.
- **Note**: Ktor matches literal path segments before parameter segments, so
  `feed/popular` and `feed` resolve correctly alongside `route("{slug}")`.

## D2. Reading `limit` / `offset` query parameters

- **Decision**: Read pagination from `call.request.queryParameters["limit"|"offset"]`,
  not `call.parameters`.
- **Rationale**: In Ktor 1.6 `ApplicationCall.parameters` exposes **path** parameters
  only; query-string values live in `request.queryParameters`. The existing `findBy`
  stub reads `ctx.parameters["limit"]`, which would always be null and silently fall back
  to the default — a latent bug. Correct query-param access is required for FR-004/FR-005
  (pagination must actually work and is tested).
- **Alternatives considered**: Mirror the stub's `ctx.parameters` usage — rejected because
  pagination would not function and the pagination acceptance scenario would fail.

## D3. Invalid pagination parameter handling (safe default)

- **Decision**: Parse with `toIntOrNull()` and fall back to defaults
  (`limit` = 20, `offset` = 0) when a value is missing or non-numeric. Clamp negative
  values to the default/zero.
- **Rationale**: The spec clarification permits mirroring existing behavior via a
  *safe default*. A safe default yields a deterministic `200 + default window` for inputs
  like `?limit=abc`, which is directly testable (acceptance scenario 4 / SC-006). The
  current global `StatusPages` maps every exception to `500`, so a raw `"abc".toInt()`
  (as the stub would do) would produce a 500 — a poor, untestable contract. Safe defaults
  are the cleaner of the two allowed options.
- **Alternatives considered**:
  - *Throw and let StatusPages map it* — rejected: produces 500 (server error) for client
    input and is inconsistent with a well-behaved API.
  - *Return 422* — rejected: would require new exception→status wiring beyond scope; the
    clarification already allows the simpler safe-default path.

## D4. Ranking query — avoiding N+1 (Performance Principle IV)

- **Decision**: Compute the ranking with a single grouped query:
  `Articles LEFT JOIN ArticleFavorites` grouped by article, `COUNT(ArticleFavorites.user)`
  as `favoritesCount`, ordered by `favoritesCount DESC, Articles.createdAt DESC`, with
  `.limit(limit, offset)`. Total count (`articlesCount`) is `Articles.selectAll().count()`
  (all articles, not the page size).
- **Rationale**: A grouped join returns the ranked page in one round-trip instead of
  counting favorites per article in a loop. Exposed supports `.count()`, `groupBy`,
  `orderBy`, and `limit(n, offset)`. Newest-first tie-break comes from the secondary
  `createdAt DESC` sort (clarification Q2).
- **Per-user personalization without N+1**: Resolve the requesting user's `favorited`
  set once via `ArticleFavorites.select { user eq me AND article inList pageIds }`, and
  `author.following` via the existing `Follows`/`Users` join
  (`UserRepository.findIsFollowUser` pattern) — set-based, not per-row.
- **Alternatives considered**: Denormalized `favoritesCount` column on `Articles`
  maintained on favorite/unfavorite — rejected for now: adds write-path complexity and
  consistency risk; the grouped read query is sufficient at scaffold scale and keeps the
  favorite/unfavorite operations simple (YAGNI, Principle I).

## D5. `articlesCount` semantics

- **Decision**: `articlesCount` is the **total** number of matching articles across all
  pages, not the size of the returned page.
- **Rationale**: Matches the RealWorld spec and existing DTO intent. The `ArticleController`
  stub returns `ArticlesDTO(listOf(), 1)` / `articles.size`, which is incorrect; the
  implementation must return the true total so clients can paginate (FR-003, FR-006).

## D6. Authenticated principal → email

- **Decision**: Obtain the current user via `ctx.authentication.principal<User>()?.email`
  and pass the email into the service, matching `UserController.update`.
- **Rationale**: The JWT `validate {}` block installs a `User` principal keyed by the
  `email` claim (`AppConfig.mainModule`). The commented article stub's
  `ctx.attribute("email")` is not a real helper in this codebase. Reusing the principal
  pattern keeps auth consistent (Principle III) and lets `authenticate {}` return `401`
  automatically for missing/invalid tokens (acceptance scenario 2).

## D7. Tag persistence for created articles

- **Decision**: Persist an article's `tagList` via an `ArticleTags` join
  (`article` -> tag name), reusing the existing `Tags` table for the global tag list;
  insert any new tag names into `Tags` on article creation.
- **Rationale**: Create-article requires `tagList` support (clarification Q4), and the
  feed returns each article's `tagList` (RealWorld shape). Reusing `Tags` keeps the
  existing `/tags` endpoint meaningful and avoids a divergent tag store (Principle I).
- **Alternatives considered**: Store tags as a delimited string column — rejected: harder
  to query, and would not populate the shared `Tags` table.

## D8. Testing approach

- **Decision**: Add one **active** `PopularArticlesFeedControllerTest` using `AppRule` +
  `HttpUtil`. Add `HttpUtil` helpers that target the real `/articles` paths (create
  article, favorite as a given user) since the existing `createArticle` helper posts to
  `/api/articles` (won't match the router). Do **not** modify or un-`@Ignore` other suites.
- **Rationale**: Constitution Principle II forbids relying on skipped tests; the new
  endpoint must have live coverage. Keeping other suites `@Ignore`d honors the spec's
  out-of-scope boundary.
- **Coverage**: ordering by favorites desc (happy path), `401` without token, invalid
  `?limit=abc` safe-default, `limit`/`offset` windowing + stable order, empty feed
  (`200`, empty list, count 0).
