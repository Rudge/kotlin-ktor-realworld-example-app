# Feature Specification: Popular Articles Feed

**Feature Branch**: `001-popular-articles-feed`

**Created**: 2026-07-01

**Status**: Draft

**Input**: User description: "Add a Popular articles feed to the RealWorld API: an authenticated `GET /api/articles/feed/popular` endpoint that returns articles ranked by how many times they have been favorited (most-favorited first), with limit/offset pagination, in the standard RealWorld multiple-articles response shape. Because the repo is a scaffold with only User and Tag layers implemented, this feature also depends on building the article + favorites persistence and service groundwork (articles storage, a favorites relationship, and working create-article and favorite/unfavorite behavior) needed to serve the ranking and produce test data."

## Clarifications

### Session 2026-07-01

- Q: Should the popular feed return a personalized article representation? → A: Yes — return the full RealWorld article object with per-requesting-user `favorited` and `author.following` flags.
- Q: What tie-breaker when articles have equal favorite counts? → A: Most recently created first (newest-first), matching RealWorld's default ordering.
- Q: What pagination defaults and bounds should the feed use? → A: Default limit 20, offset 0 (matching RealWorld); no enforced maximum limit.
- Q: What fields does creating an article require? → A: Full RealWorld shape — `title`, `description`, `body` required; `tagList` optional; `slug` derived from title.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Discover most-favorited articles (Priority: P1)

As an authenticated reader, I open the popular feed and receive a list of articles
ordered by how many times each has been favorited, with the most-favorited articles
first, so I can quickly find the content the community values most.

**Why this priority**: This is the core value of the feature — surfacing popular
content. It is the primary reason the endpoint exists; every other story exists to
support or refine it.

**Independent Test**: With a set of articles that have been favorited different
numbers of times, request the popular feed with valid credentials and confirm the
response lists articles in descending order of favorite count and reports the total
count of articles. Delivers immediate discovery value on its own.

**Acceptance Scenarios**:

1. **Given** several articles that have been favorited different numbers of times,
   **When** an authenticated reader requests the popular feed, **Then** the response
   succeeds and articles are ordered by favorite count, highest first, and includes
   the total count of articles.
2. **Given** a request with no valid authentication, **When** the popular feed is
   requested, **Then** the request is rejected as unauthorized and no feed data is
   returned.
3. **Given** no articles exist, **When** an authenticated reader requests the popular
   feed, **Then** the response succeeds with an empty article list and a total count
   of zero (not treated as a missing resource).

---

### User Story 2 - Page through the feed (Priority: P2)

As a client consuming the popular feed, I can request a limited window of results and
skip a number of leading results, so I can page through the ranking without receiving
the entire dataset at once.

**Why this priority**: Pagination makes the feed usable at scale and keeps responses
bounded, but the feed already delivers value for small datasets without it.

**Independent Test**: With more articles than a chosen page size, request successive
windows using a size limit and a starting offset, and confirm each window returns the
correct slice while the overall ordering stays consistent across pages.

**Acceptance Scenarios**:

1. **Given** more articles than the requested window size, **When** the reader
   requests the feed with a result limit and a starting offset, **Then** only the
   articles within that window are returned and the descending favorite-count ordering
   is preserved across pages.
2. **Given** a request whose pagination parameters are not valid numbers, **When** the
   feed is requested, **Then** the API responds consistently with how existing article
   listing endpoints handle invalid pagination parameters (either a validation error
   or a safe default), rather than failing unexpectedly.

---

### User Story 3 - Supporting data operations (Priority: P3)

As an authenticated user, I can create an article and favorite or unfavorite an
article, so that the popular feed has real articles and favorite activity to rank and
so the feed can be exercised end to end.

**Why this priority**: These operations are prerequisites for producing and testing
ranked data. They are foundational groundwork rather than the headline capability, so
they rank below the feed itself.

**Independent Test**: Create an article, favorite it as one or more users, then
unfavorite it, and confirm the article's favorite count rises and falls accordingly
and that this activity is reflected when the popular feed is requested.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they create an article with the required
   details, **Then** the article is persisted and becomes eligible to appear in the
   popular feed.
2. **Given** an existing article, **When** an authenticated user favorites it, **Then**
   the article's favorite count increases and reflects that user's favorite.
3. **Given** an article the user has already favorited, **When** they unfavorite it,
   **Then** the article's favorite count decreases and no longer reflects that user's
   favorite.
4. **Given** the same user favoriting the same article more than once, **When** the
   favorite is recorded, **Then** the article's favorite count reflects that user only
   once (no double counting).

---

### Edge Cases

- When multiple articles have the same favorite count, results are ordered
  most-recently-created first, giving a stable, repeatable order across identical
  requests so that pagination does not skip or repeat articles.
- When an offset is larger than the number of available articles, the feed returns an
  empty article list with the correct total count rather than an error.
- When an article that appears in the feed is unfavorited by all users, it drops toward
  the bottom of the ranking (favorite count of zero) on the next request.
- When a favorite/unfavorite action targets an article that does not exist, the action
  is reported as not found rather than silently succeeding.
- When a reader without valid authentication attempts any of the supporting data
  operations (create article, favorite, unfavorite), the action is rejected as
  unauthorized.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide a popular articles feed that returns articles
  ordered by their favorite count in descending order (most-favorited first).
- **FR-002**: The system MUST require valid authentication to access the popular feed
  and reject unauthenticated requests as unauthorized.
- **FR-003**: The system MUST return the feed in the standard multiple-articles
  response shape, including the list of articles and a total count of articles.
- **FR-004**: The system MUST support pagination of the feed via a result limit and a
  starting offset, returning only the requested window of articles. When these
  parameters are omitted, the system MUST default to a limit of 20 and an offset of 0,
  and MUST NOT enforce a maximum limit, consistent with existing article listing.
- **FR-005**: The system MUST preserve descending favorite-count ordering consistently
  across paginated requests and MUST keep the order of equally-favorited articles
  stable across identical requests. When favorite counts are equal, articles MUST be
  ordered most-recently-created first as the tie-breaker.
- **FR-006**: When no articles exist, or when the requested window is beyond the
  available results, the system MUST return a successful response with an empty article
  list and the correct total count, not a missing-resource error.
- **FR-007**: The system MUST handle invalid or non-numeric pagination parameters for the
  popular feed by falling back to the default window (limit 20, offset 0) and returning a
  successful response — a safe default — rather than failing. This is the chosen
  resolution (see Clarifications) of the option to mirror existing article listing
  behavior, and the system MUST NOT return a server error for malformed client pagination
  input.
- **FR-008**: The system MUST allow an authenticated user to create an article by
  supplying a title, description, and body (required) and an optional list of tags; the
  system MUST derive a unique slug from the title. The created article then becomes
  eligible to appear in the popular feed.
- **FR-009**: The system MUST allow an authenticated user to favorite an article,
  increasing that article's favorite count and recording the user's favorite.
- **FR-010**: The system MUST allow an authenticated user to unfavorite an article they
  previously favorited, decreasing that article's favorite count and removing the
  user's favorite.
- **FR-011**: The system MUST count each user's favorite of a given article at most
  once, so repeated favorite actions by the same user do not inflate the count.
- **FR-012**: The system MUST persist articles and favorite relationships so that
  favorite counts and rankings survive across requests.
- **FR-013**: The system MUST report favorite or unfavorite actions on a non-existent
  article as not found.
- **FR-014**: Each article entry in the feed MUST include its favorite count so clients
  can display and understand the ranking.
- **FR-015**: Each article entry in the feed MUST be personalized to the requesting
  user, indicating whether that user has favorited the article and whether that user
  follows the article's author, consistent with the standard article representation
  used elsewhere in the system.

### Key Entities *(include if feature involves data)*

- **Article**: A piece of content authored by a user that can be favorited and can
  appear in the popular feed. Key attributes: a title, a description, a body, an
  optional set of tags, a slug derived from the title (unique), authorship, creation
  time, and a derived favorite count. Relates to the users who favorite it and to its
  author.
- **Article Favorite**: The relationship recording that a specific user has favorited a
  specific article. Each user-article pair is unique. Aggregating these relationships
  per article yields the favorite count used for ranking.
- **User**: An authenticated actor who can create articles and favorite or unfavorite
  articles. Reuses the existing user/authentication concept already implemented in the
  system.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A reader requesting the popular feed receives articles correctly ordered
  by favorite count in 100% of requests where favorite counts differ.
- **SC-002**: Requests to the popular feed without valid authentication are rejected as
  unauthorized in 100% of cases.
- **SC-003**: When no articles exist, the popular feed returns a successful, empty
  result with a zero total count in 100% of cases (never a missing-resource error).
- **SC-004**: Paginated requests return exactly the requested window of articles and
  preserve ranking order across pages in 100% of tested pagination scenarios.
- **SC-005**: Favoriting and unfavoriting an article changes its favorite count by
  exactly one per distinct user action, with no double counting on repeat favorites.
- **SC-006**: New automated tests covering the popular feed happy path, the
  unauthorized case, and the invalid-parameter case all pass, while pre-existing
  skipped test suites remain skipped (unchanged).

## Assumptions

- The existing user and authentication concept is reused; no new authentication scheme
  is introduced for this feature.
- The popular feed uses the same standard multiple-articles response shape and the same
  pagination convention (result limit and starting offset) already used by existing
  article listing behavior, defaulting to a limit of 20 and an offset of 0 with no
  enforced maximum limit.
- Article creation and favorite/unfavorite are included only to the extent needed to
  produce and rank feed data and to test the feed end to end; broader article
  management (update, delete) and unrelated features are out of scope.
- Comments, profile-follow, and un-skipping unrelated existing test suites are out of
  scope for this feature.
- Ranking is based purely on total favorite count; no time-decay, personalization, or
  tag filtering is applied to the popular feed in this version.
- Where the feature description leaves invalid-input behavior open, the feed mirrors the
  established behavior of existing article listing rather than defining new rules.
