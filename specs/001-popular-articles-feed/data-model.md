# Phase 1 Data Model: Popular Articles Feed

Persistence uses JetBrains Exposed `Table` objects created via `SchemaUtils.create(...)`
in each repository's `init {}` block, matching `UserRepository`/`TagRepository`. All
access runs inside `transaction {}` against the pooled HikariCP `DataSource`.

## Entities

### Article

Represents authored content that can be favorited and ranked in the popular feed.

| Field | Type | Rules |
|-------|------|-------|
| id | Long (PK, autoincrement) | Internal identifier |
| slug | String, unique | Derived from `title` (lowercased, hyphenated); unique |
| title | String | Required (non-blank) |
| description | String | Required |
| body | String | Required (non-blank) |
| authorId | Long (FK → Users.id) | Required; the creating user |
| createdAt | DateTime | Set on creation; used as tie-break (newest first) |
| updatedAt | DateTime | Set on creation; updated on edit |

Derived (not stored): `favoritesCount` (aggregated from `ArticleFavorites`), `favorited`
(per requesting user), `author` profile with `following` (per requesting user), `tagList`
(from `ArticleTags`).

**Validation** (create): `title`, `description`, `body` required; `tagList` optional
(defaults to empty). Enforced by a request DTO validation method mirroring
`UserDTO.validRegister()`.

### ArticleFavorite (join)

Records that a user has favorited an article. Aggregating rows per article yields
`favoritesCount`.

| Field | Type | Rules |
|-------|------|-------|
| article | Long (FK → Articles.id) | Part of composite PK |
| user | Long (FK → Users.id) | Part of composite PK |

**Primary key**: composite `(article, user)` — guarantees a user favorites an article at
most once (FR-011), mirroring the existing `Follows` composite-PK pattern. Unfavorite
deletes the matching row; favoriting an already-favorited article is idempotent.

### ArticleTag (join)

Associates tag names with an article and preserves the returned `tagList`.

| Field | Type | Rules |
|-------|------|-------|
| article | Long (FK → Articles.id) | Part of composite PK |
| tag | String | Tag name; part of composite PK; also upserted into existing `Tags` |

Reuses the existing `Tags` table (`TagRepository`) for the global tag list; new tag names
are inserted there on article creation.

### User (existing — reused)

Reused unchanged from `UserRepository` (`Users` table) and `Follows` (follow graph). The
feed reuses `Follows` to compute `author.following` for the requesting user.

## Relationships

```text
Users (1) ──< Articles            (authorId)
Users (M) >──< Articles           via ArticleFavorites  (favorites)
Users (M) >──< Users              via Follows           (follow graph, existing)
Articles (M) >──< Tags(names)     via ArticleTags
```

## Derived query: popular ranking

- Page: `Articles LEFT JOIN ArticleFavorites` GROUP BY article,
  `COUNT(ArticleFavorites.user) AS favoritesCount`,
  ORDER BY `favoritesCount DESC, Articles.createdAt DESC`, `LIMIT limit OFFSET offset`.
- Total (`articlesCount`): count of all articles (independent of page window).
- Per requesting user (set-based, one query each for the page's article ids):
  - `favorited`: article ids present in `ArticleFavorites` for `user = me`.
  - `author.following`: author ids present in `Follows` for `follower = me`.

## State transitions

- **Article**: (none) → *created* → eligible for feed. (Update/delete beyond feed needs
  are out of scope.)
- **Favorite**: *not favorited* → *favorited* (insert row, count +1) → *not favorited*
  (delete row, count −1). Repeat favorite by same user is a no-op (composite PK).

## Mapping to functional requirements

| Requirement | Model support |
|-------------|---------------|
| FR-001, FR-005, FR-014 | Grouped count + `ORDER BY favoritesCount DESC, createdAt DESC` |
| FR-003, FR-006 | `ArticlesDTO(articles, totalCount)`; empty/out-of-range → empty list, real count |
| FR-004 | `LIMIT/OFFSET` with defaults 20/0 |
| FR-008 | `Articles` insert + slug derivation + `ArticleTags` |
| FR-009, FR-010, FR-011 | `ArticleFavorites` insert/delete with composite PK |
| FR-012 | Persisted tables survive across requests |
| FR-013 | Favorite/unfavorite on missing slug → `NotFoundException` |
| FR-015 | Set-based `favorited` and `author.following` per requesting user |
