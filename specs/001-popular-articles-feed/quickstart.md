# Quickstart & Validation: Popular Articles Feed

A run/validation guide to prove the feature works end to end. Implementation details
live in the plan, data-model, and contract; task breakdown belongs in `tasks.md`.

## Prerequisites

- Java 21 and the Gradle wrapper (Gradle 8.7).
- No external services — the app uses an in-memory H2 database.

## Build & test

```bash
# From repo root — build and run the test suite
./gradlew clean build

# Run only the new feed integration test (once implemented)
./gradlew test --tests '*PopularArticlesFeedControllerTest'
```

Expected: build succeeds; the new `PopularArticlesFeedControllerTest` runs (ACTIVE, not
`@Ignore`d) and passes. Pre-existing `@Ignore`d controller suites remain **skipped**
(they must not be enabled by this feature).

## Run the server manually

```bash
./gradlew run          # serves on http://localhost:8080
```

### Manual smoke test (curl)

```bash
BASE=http://localhost:8080

# 1) Register + login to get a token (User layer already works)
curl -s -X POST "$BASE/users" -H 'Content-Type: application/json' \
  -d '{"user":{"email":"a@ex.com","password":"pw","username":"alice"}}'
TOKEN=$(curl -s -X POST "$BASE/users/login" -H 'Content-Type: application/json' \
  -d '{"user":{"email":"a@ex.com","password":"pw"}}' | sed 's/.*"token":"\([^"]*\)".*/\1/')

# 2) Create two articles
curl -s -X POST "$BASE/articles" -H "Authorization: Token $TOKEN" -H 'Content-Type: application/json' \
  -d '{"article":{"title":"Alpha","description":"d","body":"b","tagList":["x"]}}'
curl -s -X POST "$BASE/articles" -H "Authorization: Token $TOKEN" -H 'Content-Type: application/json' \
  -d '{"article":{"title":"Bravo","description":"d","body":"b","tagList":["y"]}}'

# 3) Favorite one article so it ranks higher
curl -s -X POST "$BASE/articles/bravo/favorite" -H "Authorization: Token $TOKEN"

# 4) Popular feed — Bravo should appear first (higher favoritesCount)
curl -s "$BASE/articles/feed/popular" -H "Authorization: Token $TOKEN"
```

## Acceptance scenarios → how to validate

| Scenario (spec) | Validation | Expected |
|-----------------|-----------|----------|
| P1 ordering | Favorite articles unequally, GET popular feed | `200`; articles sorted by `favoritesCount` desc, newest-first on ties |
| Auth required | GET `/articles/feed/popular` with no `Authorization` | `401` |
| Empty feed | GET popular feed with no articles created | `200`; `articles: []`, `articlesCount: 0` |
| Invalid param | GET `/articles/feed/popular?limit=abc` | `200`; safe default window (limit 20) |
| Pagination | GET with `?limit=1&offset=1` | `200`; second-ranked article only; order stable across pages |
| P3 favorite count | Favorite then unfavorite an article | `favoritesCount` +1 then −1; repeat favorite does not double-count |

## Success criteria mapping

- SC-001..SC-005 are exercised by the ordering, auth, empty, pagination, and
  favorite/unfavorite checks above.
- SC-006: the new test passes while `@Ignore`d suites stay skipped — confirm in the
  `./gradlew test` report (skipped count unchanged for other controller suites).

## References

- Endpoint & schema contract: [contracts/popular-feed.openapi.yaml](./contracts/popular-feed.openapi.yaml)
- Entities & queries: [data-model.md](./data-model.md)
- Design decisions (routes, pagination, N+1): [research.md](./research.md)
