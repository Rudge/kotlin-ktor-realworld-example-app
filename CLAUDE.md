# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build and run happen in Docker (JDK 17 images pinned by digest in `Dockerfile`); no local JDK needed. Gradle 8.14 wrapper, Kotlin 2.4, Ktor 3.6 (CIO), Exposed 1.6, Kodein 7. Dependencies are locked in `gradle.lockfile` (refresh with `./gradlew dependencies --write-locks`).

```bash
docker build --target build -t realworld-build .        # compile + run tests (clean build)
docker build -t realworld . && docker run --rm -p 8080:8080 realworld   # build image and start server on :8080
APIURL=http://localhost:8080 ./spec-api/run-api-tests.sh                # RealWorld Postman spec against a running server
```

Single test (inside the build image's workdir): `./gradlew test --tests "io.realworld.app.web.controllers.UserControllerTest"`. All test classes are currently `@Ignore`d, so `build` runs zero tests. No linter/formatter is configured.

## Architecture

Entry: `App.kt` → `config.setup()` (in `config/AppConfig.kt`) → connects DB, builds a Ktor embedded server (CIO only), module `Application.mainModule()`.

- `mainModule()` resolves controllers from the Kodein container (`config/ModulesConfig.kt`, one `DI.Module` per feature), installs JWT auth (scheme `Token`, not `Bearer`), `StatusPages`, Jackson, then routes from `web/Router.kt` (extension functions `Routing.users/profiles/articles/tags`).
- Layering: `web/controllers` → `domain/service` → `domain/repository` (JetBrains Exposed tables + queries, wrapped in `transaction {}`). Domain models/DTOs live in `domain/*.kt`; `domain/exceptions` are mapped to HTTP errors.
- DB: in-memory H2 via Hikari, set up in `config/DbConfig.kt`; it also starts an H2 Postgres-protocol server, so tests/app binding twice in one JVM can conflict.
- Controllers take `ApplicationCall` directly (not typed request params).
- JWT: `utils/JwtProvider.kt`; config values in `application.conf` (`ktor.jwt`). Password hashing in `utils/Cipher.kt`.

### Implementation status (important)

Only **User** and **Tag** are fully wired (controller + service + repository + Kodein bindings). `ArticleController`, `ProfileController`, `CommentController` are stubs with the service calls commented out and return placeholder data; they are bound in Kodein with no dependencies. Expect Article/Profile/Comment tests and spec-api checks to fail or be incomplete until those layers are built following the User/Tag pattern.

## Tests

JUnit 4 integration tests in `src/test/kotlin/.../web/controllers`. `web/rules/AppRule` (an `ExternalResource`) boots the real server via `setup()` and exposes `HttpUtil` (Unirest) for HTTP calls — tests hit the actual endpoints, not mocks.
