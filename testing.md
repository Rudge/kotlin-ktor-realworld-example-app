# Testing

How CI runs tests and how to reproduce the same checks locally.

For JDK setup and running the server, see [local-development-environment.md](local-development-environment.md).

---

## Gradle CI (`.github/workflows/gradle.yml`)

Runs on every **push** and **pull request** to `master`.

### What it does

| Step | Action |
|------|--------|
| Matrix | JDK **17** and **21** (Eclipse Temurin), `fail-fast: false` |
| Cache | `gradle/gradle-build-action@v3` caches Gradle dependencies between runs |
| Build | `./gradlew build --no-daemon` |
| Test | `./gradlew test --no-daemon` |
| Report | `dorny/test-reporter` publishes JUnit XML from `build/test-results/test/*.xml` as GitHub check annotations |

### Run locally

Same commands CI uses (without the matrix — pick your installed JDK):

```bash
./gradlew build test
```

JUnit XML reports are written to `build/test-results/test/`.

---

## RealWorld API spec tests (`.github/workflows/spec-api.yml`)

Bonus workflow: runs the [RealWorld Postman collection](spec-api/Conduit.postman_collection.json) against a live server via [newman](https://github.com/postmanlabs/newman).

**Triggers:** push/PR to `master`, or manual run via **Actions → RealWorld API Spec Tests → Run workflow** (`workflow_dispatch`).

**CI behavior:** the job uses `continue-on-error: true` because many RealWorld endpoints are still stubs in this codebase — failures are informational and do not block the main Gradle CI job.

### What CI does

1. Build the application (`./gradlew build`)
2. Start the server in the background (`./gradlew run` on port **8080**)
3. Wait until `GET /tags` responds
4. Run `spec-api/run-api-tests.sh` with `APIURL=http://localhost:8080`

### Important: no `/api` prefix

This app serves routes at the root (e.g. `/users`, `/articles`), not under `/api`. Use:

```bash
APIURL=http://localhost:8080
```

The upstream [spec-api README](spec-api/README.md) uses `http://localhost:3000/api` — that does not apply to this fork.

### Run locally

**Prerequisites:** JDK 21 (or compatible), Node.js (for `npx newman`).

```bash
./gradlew build
./gradlew run &
```

Wait until the server is up:

```bash
curl -sf http://localhost:8080/tags
```

Run the spec tests:

```bash
chmod +x spec-api/run-api-tests.sh
APIURL=http://localhost:8080 spec-api/run-api-tests.sh
```

Stop the server when finished (`fg` then Ctrl+C, or kill the Gradle run process).
