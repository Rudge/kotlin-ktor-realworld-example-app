# Prompt history

Chronological log of prompts used during this assessment.

---

**2026-06-30 15:36**

`./gradlew clean install` is failing — diagnose and fix. Context: JDK 25 is too new; use JDK 21.

---

**2026-06-30 15:43**

Review terminal output from `./gradlew clean install`. Note: first run was attempted outside nix-shell.

---

**2026-06-30 15:52**

Build still fails for both `./gradlew clean install` and `./gradlew clean build`.

---

**2026-06-30 16:01**

Create `local-development-environment.md`: how to run the project cleanly with `nix-shell`, how to install Nix, post-setup commands, and `JAVA_HOME` details.

---

**2026-06-30 16:05**

Update `local-development-environment.md` — drop JDK 25 troubleshooting; document current setup only. Do not dwell on earlier `shell.nix` history.

---

**2026-06-30 16:13**

`./gradlew run` throws a stack trace; nothing listens on port 8080. The server should start with no external service dependencies.

---

**2026-06-30 16:20**

Verify yourself: hit a working REST endpoint — create an entity and read it back. Goal is a working server.

---

**2026-06-30 16:29**

Confirmed working flow:

```bash
# 1. Create user
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"user":{"email":"you@example.com","password":"secret123","username":"you"}}'

# 2. Retrieve user (use token from step 1)
curl http://localhost:8080/user \
  -H "Authorization: Token <token>"
```

---

**2026-06-30 16:41**

`User.kt` line 11: did `UserDTO` previously require an empty password, and now blank passwords are rejected? Was it generating random username/password before — bug or intentional?

**Outcome:** Inverted `isNullOrBlank()` checks (missing `!`) in `validRegister()`, `validLogin()`, and `validToUpdate()`. Tests missed it because `UserControllerTest` is `@Ignore`. Fix aligns with RealWorld spec: register needs email + username + password; login needs email + password.

---

**2026-06-30 16:45**

`AppRule.kt`: prefer the previous version with spelled-out time units if it still works.

---

**2026-06-30 16:48**

Revert that — keep the new two-argument `stop()` syntax. Avoid workarounds for the deprecated three-argument API; the previous call style is cleaner. See `AppRule.kt` lines 11–13.

---

**2026-06-30 16:56**

Implement PLAN.md Option A: article favorites count endpoint. Confirm it does not exist yet. Add tests that run under `./gradlew test`. Read the rest of PLAN.md for anything relevant to Option A.

---

**2026-06-30 17:11**

Implementation returned: 9 files changed, 3 new files. Running tests.

---

**2026-06-30 17:33**

Add comments for all new functions and their arguments:

- **AppConfig.kt** — why `setup()` changed; unique `dbName` and why shared DB failed before; why `server()` was extracted; new `StatusPages` handlers vs old behavior
- **DbConfig.kt** — why schema creation lives in `setup()` (`DbConfig.kt` lines 25–27)
- **ArticleRepository** — document new functions
- **TagRepository.kt** (lines 27–30), **ArticleService.kt** (class + lines 6–16)
- **String.kt** (lines 12–14) — purpose of `toSlug()`
- **ArticleController.kt** — `popularFeed`, `create`, `favorite`, `parseQueryInt`
- **PopularArticleFeedControllerTest.kt** — purpose of each test/helper
- **HttpUtil.kt** (lines 20–22) — why `FAIL_ON_UNKNOWN_PROPERTIES` is needed
- **AppRule.kt** (lines 13–20, 28–29) — dynamic ports and sleep delays

---

**2026-06-30 18:02**

For all staged files: add per-file change comments. Preview the git commit message. Local commit only — do not push.

---

**2026-06-30 18:04**

Implement PLAN.md Part 4: GitHub Actions CI/CD.

---

**2026-06-30 18:12**

CI should trigger on `master` only (no `main`). Update README.md with a link to `testing.md` covering `gradle.yml` and local run notes for `.github/workflows/spec-api.yml`.

---

**2026-06-30 18:20**

`run-api-tests.sh` (lines 5–6): the default cloud URL is stale. Remove it; require `APIURL` and print usage on error. Usage should show local and optional hosted examples.
