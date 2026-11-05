# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with this repository.

## Project

Kotlin + Ktor implementation of the [RealWorld](https://github.com/gothinkster/realworld) (Conduit) backend API. Stack: Ktor 1.2.3 (CIO engine by default, Netty optional), Kodein 6 DI, Exposed 0.14 (SQL), H2 in-memory DB via HikariCP, Jackson, java-jwt, JUnit 4 + Unirest. Gradle (Groovy DSL), Kotlin 1.3.x — old library versions, so check APIs against these versions rather than current Ktor/Exposed docs.

## Commands

```bash
./gradlew clean build          # compile + tests
./gradlew run                  # start server on :8080 (mainClass io.realworld.app.AppKt)
./gradlew test                 # tests only
./gradlew test --tests 'io.realworld.app.web.controllers.UserControllerTest.<method>'   # single test
./gradlew run & APIURL=http://localhost:8080 ./spec-api/run-api-tests.sh   # official RealWorld Postman spec (spec-api/)
```

CI (`.github/workflows/gradle.yml`) runs `./gradlew build` on JDK 16; `.travis.yml` also runs the spec-api script after build. No linter/formatter is configured.

## Architecture

Entry: `App.kt` → `config.setup()` (in `config/AppConfig.kt`) connects the DB (`DbConfig`: H2 in-memory, also starts an H2 Postgres-protocol server) and builds an embedded server whose module is `Application.mainModule()`.

`mainModule()` pulls controllers out of the Kodein container (`config/ModulesConfig.kt`, one `Kodein.Module` per feature: USER, ARTICLE, PROFILE, COMMENT, TAG), then installs CallLogging, Jackson ContentNegotiation, JWT auth (scheme `Token`, verifier/audience from `utils/JwtProvider`; the authenticated principal is looked up via `UserController.getUserByEmail`), a catch-all StatusPages handler (500), and routing.

Layering: `web/Router.kt` (route tree per feature, `authenticate` / `authenticate(optional = true)` blocks) → `web/controllers/*Controller` → `domain/service/*Service` → `domain/repository/*Repository` (Exposed tables are defined in the repository files, e.g. `Users`, `Follows`). `domain/*.kt` holds the data classes/DTOs; `domain/exceptions` holds exceptions mapped in `web/ErrorExceptionMapping.kt`.

Implementation status is uneven:
- **User and Tag** are fully wired (controller → service → repository).
- **Article, Profile, Comment** controllers are stubs: no-arg constructors, service calls commented out, returning placeholder values. New work there means creating the service/repository and registering them in `ModulesConfig` (follow the User/Tag modules).

Note: route paths in `Router.kt` have no `/api` prefix, while the tests (and `.travis.yml`) use `/api/...` — verify before relying on either.

## Tests

`src/test/.../web/rules/AppRule` is a JUnit `ExternalResource` that boots the real server via `setup()` and exposes `HttpUtil` (Unirest wrapper) for end-to-end HTTP tests. All five controller test classes are currently annotated `@Ignore` and largely commented out, so `./gradlew test` exercises nothing until they are re-enabled. Because the DB is in-memory and the port fixed at 8080, tests start a real server on 8080.

## Repo tooling

`.gsd/`, `.agents/`, `.claude/skills/`, `.bg-shell/`, `skills-lock.json` are GSD workflow tooling artifacts (`.gsd` is gitignored), not application code.
