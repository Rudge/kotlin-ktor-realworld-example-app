<!--
SYNC IMPACT REPORT
==================
Version change: (template, unversioned) → 1.0.0
Bump rationale: Initial ratification — first concrete constitution replacing the
unfilled template. MAJOR baseline per semantic versioning for a new governing document.

Principles (template slot → concrete):
  - PRINCIPLE_1 → I. Code Quality & Consistency
  - PRINCIPLE_2 → II. Testing Standards (NON-NEGOTIABLE)
  - PRINCIPLE_3 → III. User Experience Consistency
  - PRINCIPLE_4 → IV. Performance Requirements
  - PRINCIPLE_5 → (removed; user requested four focused principles)

Sections:
  - SECTION_2 → Technology & Quality Constraints
  - SECTION_3 → Development Workflow & Quality Gates

Templates status:
  - .specify/templates/plan-template.md ✅ compatible (Constitution Check gate is
    generic; gates now resolve to the four principles below)
  - .specify/templates/spec-template.md ✅ no constitution-specific content
  - .specify/templates/tasks-template.md ✅ no constitution-specific content
  - .github/agents/speckit.*.agent.md ✅ generic references only

Deferred TODOs: none
-->

# Kotlin Ktor RealWorld API Constitution

## Core Principles

### I. Code Quality & Consistency

New and modified code MUST follow the established layered pattern proven by the
`User` and `Tag` implementations: `domain/repository` (Exposed tables + persistence)
→ `domain/service` (business logic) → `web/controllers` (route handlers), wired
through Kodein dependency injection.

- Every new feature touching articles, comments, or profiles MUST build the missing
  repository + service layer first; controllers MUST NOT contain persistence or
  business logic, and MUST NOT return hard-coded placeholder DTOs.
- Constructors MUST receive collaborators via Kodein injection; no direct
  instantiation of repositories/services inside controllers.
- Code MUST be idiomatic Kotlin (null-safety, data classes for DTOs, extension
  functions in `ext/` for cross-cutting helpers) and MUST compile warning-free.
- The build (`./gradlew clean build`) MUST succeed before any change is considered
  complete.

**Rationale**: A single, copy-able pattern keeps an intentionally partial scaffold
maintainable and lets contributors extend it predictably rather than inventing
divergent structures.

### II. Testing Standards (NON-NEGOTIABLE)

Behavior MUST be verified by executable tests, and those tests MUST actually run.

- Any controller/endpoint that is implemented MUST have its test class ACTIVE — the
  `@Ignore` annotation MUST be removed once the corresponding feature is built. A
  "green" build achieved only by skipping tests is a violation.
- New persistence and service layers MUST ship with tests following the
  `UserRepository`/`UserService` reference tests; happy path plus authn/authz and
  validation error paths MUST be covered.
- Endpoints MUST be covered by JUnit + Unirest integration tests and, where a
  RealWorld contract exists, remain compatible with `spec-api/run-api-tests.sh`.
- Tests MUST NOT be weakened, deleted, or re-`@Ignore`d to make a build pass.

**Rationale**: The baseline ships with all controller tests ignored; without this
rule "passing" tests give false confidence and regressions ship silently.

### III. User Experience Consistency

The API is the product's user interface and MUST behave consistently with the
RealWorld specification and existing endpoints.

- Request/response shapes MUST match the RealWorld spec and the DTO conventions
  already used by the `User` and `Tag` endpoints (envelope keys, field names, casing).
- Authentication MUST use the existing JWT mechanism; protected endpoints MUST reject
  missing/invalid tokens consistently.
- Errors MUST flow through `web/ErrorExceptionMapping.kt` and return the RealWorld
  error format with appropriate HTTP status codes; endpoints MUST NOT invent ad-hoc
  error bodies.
- Validation (e.g., email format via `ext/`) MUST be applied uniformly across
  endpoints handling the same data.

**Rationale**: Consumers rely on uniform contracts; inconsistent envelopes, auth, or
error formats break clients and violate the spec the project targets.

### IV. Performance Requirements

Endpoints MUST be efficient and MUST NOT introduce avoidable database or memory cost.

- Database access MUST avoid N+1 query patterns; related data (e.g., tags, favorites,
  authors) MUST be fetched with set-based/joined Exposed queries rather than per-row
  lookups in a loop.
- List/feed endpoints MUST support pagination (limit/offset) and MUST NOT load entire
  tables into memory.
- Connection handling MUST use the configured pooled `DataSource` (HikariCP); code
  MUST NOT open ad-hoc connections per request.
- Any change that alters query shape on a hot path SHOULD be justified in the PR when
  it could regress latency, and MUST NOT remove existing pagination or indexing.

**Rationale**: The RealWorld feeds and article lists grow with data; set-based access
and pagination keep response times bounded as the dataset scales.

## Technology & Quality Constraints

- Stack is fixed: Kotlin + Ktor (web), Kodein (DI), Exposed (ORM), H2 (embedded DB),
  JUnit + Unirest (tests). Adding or swapping frameworks requires an amendment.
- Build & test commands are authoritative: `./gradlew clean build` to build/test,
  `./gradlew run` to serve on port 8080, and
  `APIURL=http://localhost:8080 ./spec-api/run-api-tests.sh` for API integration tests.
- Toolchain: Java 21 + Gradle 8.7 as configured in the wrapper.
- Session documentation standards from `AGENTS.md` (`PROCESS.md`, `LESSONS_LEARNED.md`,
  `NEXT_STEPS.md`) MUST be maintained across a working session.

## Development Workflow & Quality Gates

- Before a change is "done": code compiles, the relevant tests are ACTIVE and pass,
  and no test was ignored/deleted to achieve green.
- Pull requests MUST verify compliance with all four core principles; deviations MUST
  be justified explicitly in the PR description.
- Features spanning articles/comments/profiles MUST land the persistence + service
  layer and its tests in the same change set as the controller wiring, not as a stub.
- Complexity that departs from the reference layered pattern MUST be justified; absent
  justification, the simpler pattern wins (YAGNI).

## Governance

This constitution supersedes ad-hoc practices for this repository. All pull requests
and reviews MUST verify compliance with the four core principles and the constraints
above.

- Amendments MUST be proposed via PR that updates this file, states the rationale, and
  bumps the version per the policy below.
- Versioning policy (semantic): MAJOR for backward-incompatible principle removals or
  redefinitions; MINOR for a newly added principle/section or materially expanded
  guidance; PATCH for clarifications and non-semantic wording fixes.
- Compliance is reviewed at PR time; unjustified violations MUST block merge.
- Runtime and contributor guidance lives in `AGENTS.md`; where it conflicts with this
  constitution, the constitution governs and `AGENTS.md` MUST be reconciled.

**Version**: 1.0.0 | **Ratified**: 2026-07-01 | **Last Amended**: 2026-07-01
