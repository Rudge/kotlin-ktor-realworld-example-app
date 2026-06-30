# FDE Candidate Technical Exercise

## Overview

This exercise evaluates your ability to work with an existing codebase, add meaningful features, write tests using AI-powered tooling, and set up CI/CD — skills central to the Field Developer Engineer role.

**Time Budget:** ~90 minutes  
**Repository:** [Rudge/kotlin-ktor-realworld-example-app](https://github.com/Rudge/kotlin-ktor-realworld-example-app)

---

## Background

This is a [RealWorld](https://github.com/gothinkster/realworld) spec implementation using **Kotlin + Ktor + Exposed + H2**. It demonstrates CRUD, authentication (JWT), routing, and pagination for a Medium-like blogging platform.

### Tech Stack
- Kotlin, Ktor (web framework), Kodein (DI), Exposed (SQL ORM), H2 (in-memory DB)
- Existing tests use JUnit + Unirest (HTTP client)
z
### Quick Start
```bashz
./gradlew --stop           # stop existing gradle daemons running
rm -fR ~/.gradle/caches/*  # make sure caches are clean
./gradlew clean build      # Build
./gradlew run              # Start server on port 8080
./gradlew test             # Run tests
```

---

## Instructions

### Part 1: Fork & Setup

1. Fork this repository to your personal GitHub account.
2. Clone your fork locally and verify the project builds (`./gradlew clean build`).
3. Create a feature branch (e.g., `feature/article-favorites-count`).

---

### Part 2: Add a Feature

Implement **one** of the following features (or propose your own of similar scope):

#### Option A: Article Favorites Count Endpoint
Add a `GET /api/articles/feed/popular` endpoint that returns articles sorted by number of favorites (most favorited first). Include pagination support (`limit` and `offset` query params).

#### Option B: User Activity Stats
Add a `GET /api/profiles/:username/stats` endpoint that returns:
```json
{
  "stats": {
    "articlesCount": 5,
    "commentsCount": 12,
    "favoritesCount": 3
  }
}
```

#### Option C: Article Search
Add a `GET /api/articles/search?q=<term>` endpoint that searches articles by title and body content. Return results in the standard article list format.

**Requirements:**
- Follow the existing code patterns (controller → service → repository layers).
- Ensure the feature requires authentication where appropriate.
- Handle error cases (404, 401, 422) consistently with existing error handling.

---

### Part 3: Write Tests Using Copilot Agent Harness

Use the **GitHub Copilot coding agent** (via GitHub Issue or Copilot Chat in your IDE) to generate and iterate on tests for your new feature.

**What we want to see:**

1. **Create an issue** in your forked repo describing the tests needed for your feature. Assign it to Copilot (or use `@copilot` in the issue body) to generate a PR with tests.
2. **Review the agent's output** — refine the generated tests, fix any issues, and ensure they:
   - Cover the happy path (valid request → expected response).
   - Cover at least 2 error cases (e.g., unauthenticated, not found, invalid input).
   - Follow the existing test style in `src/test/kotlin/io/realworld/app/web/controllers/`.
3. **Document your experience** — In your PR description, briefly note:
   - What prompt(s) you gave the agent.
   - What worked well vs. what you had to fix manually.
   - How you'd improve the agent workflow for a customer.

---

### Part 4: GitHub Actions CI/CD

Update the CI pipeline (`.github/workflows/`) to include:

1. **Build & Test** — Run `./gradlew build` and `./gradlew test` on every push and PR to `main`/`master`.
2. **Multi-JDK Matrix** — Test against JDK 17 and JDK 21.
3. **Test Reporting** — Publish JUnit test results as a check annotation (use an action like `dorny/test-reporter` or `mikepenz/action-junit-report`).
4. **Caching** — Ensure Gradle dependencies are cached between runs.
5. **(Bonus)** Add a workflow that runs the RealWorld API spec tests (`spec-api/run-api-tests.sh`) against the running server.

---

## Deliverables

Submit a **single Pull Request** to your fork's `main` branch containing:

- [ ] The new feature implementation (Part 2)
- [ ] Tests generated/refined with the Copilot agent (Part 3)
- [ ] Updated GitHub Actions workflow(s) (Part 4)
- [ ] A clear PR description covering your approach, decisions, and agent experience

---

## Evaluation Criteria

| Area | What We're Looking For |
|------|----------------------|
| **Code Quality** | Clean, idiomatic Kotlin. Follows existing patterns. Proper error handling. |
| **Testing** | Meaningful coverage. Tests actually pass. Good assertions. |
| **Agent Fluency** | Effective use of Copilot agent. Thoughtful critique of its output. Clear documentation of prompts and iteration. |
| **CI/CD** | Working pipeline. Correct matrix setup. Good use of caching and reporting. |
| **Communication** | Clear PR description. Good commit messages. Explains trade-offs. |

---

## Tips

- The existing test infrastructure in `src/test/kotlin/.../rules/AppRule.kt` boots the full app for integration testing — use this pattern.
- Look at `UserControllerTest.kt` for a good example of the testing style.
- The H2 database is in-memory and resets between test runs — no external DB setup needed.
- If you get stuck on the build, JDK 16+ is required (the project uses `jvmTarget = "16"`).

---

## Questions?

If anything is unclear, reach out to your interviewer. Part of this exercise is about how you communicate and unblock yourself — don't hesitate to ask.

Good luck! 🚀