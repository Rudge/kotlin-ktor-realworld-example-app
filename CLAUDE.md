# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the project
./gradlew clean build

# Run the server (starts on port 8080)
./gradlew run

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "io.realworld.app.web.controllers.UserControllerTest"

# Run a single test method
./gradlew test --tests "io.realworld.app.web.controllers.UserControllerTest.success login with email and password"

# Run RealWorld API spec tests (requires server running)
./gradlew run & APIURL=http://localhost:8080 ./spec-api/run-api-tests.sh
```

## Architecture

This is a RealWorld spec implementation using Kotlin + Ktor + Kodein + Exposed.

### Layers

- **config/** - Application setup (Ktor server, Kodein DI, H2 database)
- **domain/** - Domain models (Article, Comment, User, Profile, Tag) and business logic
  - **repository/** - Exposed-based persistence with table definitions
  - **service/** - Business logic and data transformation
- **web/** - HTTP layer
  - **controllers/** - Request handlers that receive `ApplicationCall`
  - **Router.kt** - Route definitions organized by feature (users, profiles, articles, tags)
- **utils/** - JWT provider and password encryption (Cipher)

### Key Patterns

- **Dependency Injection**: Kodein modules defined in `ModulesConfig.kt` - each feature (user, article, profile, comment, tag) has its own module binding Controller → Service → Repository
- **Authentication**: JWT-based via `ktor-auth-jwt`, uses "Token" scheme. JwtProvider handles token creation/verification
- **Database**: H2 in-memory database configured in `DbConfig.setup()`, Exposed ORM for queries
- **Routing**: Extension functions on `Routing` in Router.kt (e.g., `Routing.users()`, `Routing.articles()`)

### Testing

Tests use JUnit 4 with `AppRule` (ExternalResource) that starts/stops the embedded server. `HttpUtil` provides HTTP client helpers using Unirest.
