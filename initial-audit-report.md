# Codebase Audit Report

## 1. Audit Metadata
- **Target:** `{{TARGET_URI}}` placeholder unresolved → audited current working directory (rule 3).
- **Resolved path:** `/Users/humbertobello/development/ai/gsd-pi/kotlin-ktor-realworld-example-app`
- **Commit:** `d2a39a32c4f33b0dc3b4c6602fc0435216c79a86` (branch `master`, working tree clean before and after the audit)
- **Audit date:** 2026-11-05
- **Method:** Read-only. All builds/dependency resolution ran in a disposable copy (`<scratchpad>/copy`, rsync without `.git`/`build`/`.gradle`); the copy's `build.gradle` was patched there only (Exposed 0.17.14, forced stdlib) to probe further build errors.
- **Stack:** Kotlin 1.3.+ / JVM, Ktor 1.2.3 (CIO default, Netty optional), Kodein 6.1.0, Exposed 0.14.1, H2 2.2.224 + HikariCP 5.1.0, Jackson (via ktor-jackson), java-jwt (transitive), JUnit 4 + Unirest 1.4.9. Gradle 4.10 wrapper (Groovy DSL). CI: GitHub Actions (JDK 16), Travis (JDK 8–11). ~27 main/7 test Kotlin files, ~1.4k LOC.
- **Excluded paths:** `.git/`, `.gradle/`, `build/`, `.agents/`, `.claude/`, `.gsd/`, `.bg-shell/`, `skills-lock.json` (agent tooling, not application code), `spec-api/Conduit.postman_collection.json` (third-party spec fixture), `gradle/wrapper/gradle-wrapper.jar` (binary).

## 2. Executive Summary
The project cannot be built from a clean checkout: Exposed 0.14.1 only lived on the shut-down JCenter, and the nearest resolvable Exposed (0.17.14) is binary-incompatible with the Kotlin 1.3 toolchain. The whole stack (Ktor 1.2.3, Kotlin 1.3, Gradle 4.10) is end-of-life and resolves Jackson 2.9.9 and Netty 4.1.37 with dozens of known High/Critical advisories. The only security primitive is a hardcoded HMAC key that both signs JWTs and "hashes" passwords, so tokens are forgeable by anyone with the source. Input validation for register/login/update is logically inverted (valid input is rejected), and all five test classes are `@Ignore`d, which hides this. Only User and Tag features are implemented; Article/Profile/Comment are stubs with commented-out code.

| Category | High | Medium | Low | Total |
|---|---|---|---|---|
| 1. Build breakers | 1 | 1 | 1 | 3 |
| 2. Missing test coverage | 1 | 1 | 0 | 2 |
| 3. Dead code | 0 | 1 | 2 | 3 |
| 4. Duplicated code | 0 | 0 | 1 | 1 |
| 5. Outdated dependencies | 1 | 1 | 1 | 3 |
| 6. Vulnerable code | 2 | 4 | 3 | 9 |
| 7. Design deviations | 1 | 2 | 1 | 4 |
| **Total** | **6** | **10** | **9** | **25** |

**Top 5 priority findings:** BUILD-001, SEC-001, SEC-002, DESIGN-001, TEST-001.

## 3. Findings

### 3.1 Build Breakers

### [BUILD-001] Build fails: Exposed 0.14.1 unresolvable; JCenter sunset; no compatible fallback
- **Category:** Build breakers
- **Severity:** High
- **Confidence:** Confirmed
- **Location(s):** `build.gradle:11` (`exposed_version = "0.14.1"`), `build.gradle:20,28` (`jcenter()`), `build.gradle:4` (`kotlin_version = "1.3.+"`)
- **Description:** `./gradlew build` fails at dependency resolution; the artifact is absent from Maven Central and JCenter now redirects (HTTP 301). The newest `org.jetbrains.exposed:exposed` on Central is 0.17.14, but it was compiled with Kotlin 1.5 and drags `kotlin-stdlib` 1.5.30, which the 1.3 compiler cannot read, so a simple version bump does not fix the build.
- **Evidence:**
  ```
  > Task :compileKotlin FAILED
  > Could not find org.jetbrains.exposed:exposed:0.14.1.
     - https://repo.maven.apache.org/maven2/.../exposed-0.14.1.pom
     - https://jcenter.bintray.com/.../exposed-0.14.1.pom
  # JDK 8 and JDK 17: same failure.
  # Copy patched to 0.17.14:
  e: .../UserController.kt: (23, 21): Class 'kotlin.Unit' was compiled with an incompatible version of Kotlin. The binary version of its metadata is 1.5.1, expected version is 1.1.16.
  ```
- **Recommended Mitigation:** Upgrade as one unit: Kotlin (≥1.9/2.x), Ktor (2.x/3.x), Exposed (`exposed-core/dao/jdbc` 0.5x+), Gradle (≥8) and remove `jcenter()`. Pin exact versions (no `1.3.+`). Add a CI job that builds from a clean cache.

### [BUILD-002] CI definitions are inconsistent with the application and each other
- **Category:** Build breakers
- **Severity:** Medium
- **Confidence:** Likely
- **Location(s):** `.travis.yml:2-6,8`, `.github/workflows/gradle.yml:25-29`, `build.gradle:9` (HikariCP 5.1.0), `src/main/kotlin/io/realworld/app/config/AppConfig.kt:35`
- **Description:** Travis runs `APIURL=http://localhost:7000/api` while the server listens on 8080 with routes lacking `/api`; the script starts immediately after `./gradlew run &` (race). Travis matrix includes JDK 8/9/10 although HikariCP 5.x requires Java 11+ (from HikariCP's published requirements, not observed here). GitHub Actions only runs `./gradlew build` (which runs zero tests, see TEST-001) and never the spec-api suite.
- **Evidence:** `.travis.yml:8: ./gradlew run & APIURL=http://localhost:7000/api ./spec-api/run-api-tests.sh` vs `AppConfig.kt:35: const val SERVER_PORT = 8080`.
- **Recommended Mitigation:** Drop Travis or align port/path; wait for readiness before running the spec suite; run spec-api in GitHub Actions; set the JDK matrix to what dependencies support.

### [BUILD-003] Non-reproducible dependency resolution
- **Category:** Build breakers
- **Severity:** Low
- **Confidence:** Confirmed
- **Location(s):** `build.gradle:4`, `build.gradle:18,26` (`mavenLocal()` first)
- **Description:** `kotlin_version = "1.3.+"` is a dynamic version, and `mavenLocal()` precedes Central so developer-machine artifacts can mask missing ones. Resolution already drifted: stdlib resolved to 1.5.30 in the patched copy.
- **Evidence:** `kotlin-stdlib-jdk8:1.3.+ -> 1.5.30` (from `gradle dependencies`, patched copy).
- **Recommended Mitigation:** Pin exact versions, enable Gradle dependency locking, remove `mavenLocal()`.

### 3.2 Missing Test Coverage

### [TEST-001] All tests are `@Ignore`d; zero executed tests, including auth
- **Category:** Missing test coverage
- **Severity:** High
- **Confidence:** Confirmed
- **Location(s):** `src/test/kotlin/io/realworld/app/web/controllers/UserControllerTest.kt:13` and the four other `*ControllerTest.kt` (class-level `@Ignore`)
- **Description:** `./gradlew test` exercises nothing. `UserService`, `JwtProvider`, `Cipher`, `UserRepository`, `UserDTO` validation and the JWT `validate` block have no active test. Several tests are also commented out (e.g. `UserControllerTest.kt:19-20,58-59`). Coverage tooling could not run (BUILD-001); coverage is effectively 0%.
- **Evidence:** `UserControllerTest.kt:13: @Ignore` (5 of 5 test classes); no JaCoCo/Kover plugin in `build.gradle`.
- **Recommended Mitigation:** Re-enable tests once the build works, add unit tests for validation, hashing, JWT verify/expiry/audience, and negative paths (401/422); add JaCoCo/Kover with a coverage gate.

### [TEST-002] Existing tests encode defects and rely on global state
- **Category:** Missing test coverage
- **Severity:** Medium
- **Confidence:** Confirmed
- **Location(s):** `UserControllerTest.kt:55`, `src/test/kotlin/io/realworld/app/web/rules/AppRule.kt:10-12`
- **Description:** The register test asserts the response echoes the plaintext password (see SEC-004). All tests boot a real server on fixed port 8080 with shared in-memory DB and a 500 ms sleep, which is order-dependent and flaky. No unit-level tests exist (domain logic only reachable through HTTP).
- **Evidence:** `assertEquals(response.body.user?.password, userDTO.user?.password)`; `TimeUnit.MILLISECONDS.sleep(500)`.
- **Recommended Mitigation:** Fix assertions to the spec; use Ktor `withTestApplication` (no fixed port) and unit tests per layer.

### 3.3 Dead Code

### [DEAD-001] Stub controllers with commented-out implementations
- **Category:** Dead code
- **Severity:** Medium
- **Confidence:** Confirmed
- **Location(s):** `ArticleController.kt:9,17-19,26-28,34-36,42-44,51-53,59,64-66,72-74`; `CommentController.kt:8,12-14,20-22,29`; `ProfileController.kt:6,9-10,15-16,21-22`; `AppConfig.kt:30` imports; unused locals (`ArticleController.kt:12-16,24-25`, `CommentController.kt:10,27-28`)
- **Description:** Three of five features are placeholders: service calls commented out, unused variables, return placeholders (`ArticlesDTO(listOf(), 1)`). `Article`, `Comment`, `Profile` DTOs are used only by these stubs. The Router ignores the controllers' return values, so these endpoints never write a response.
- **Evidence:** `ArticleController.kt:17: //        articleService.findBy(tag, author, ...`
- **Recommended Mitigation:** Implement the services/repositories or remove the routes and files; delete commented code (history keeps it).

### [DEAD-002] Unused symbols and config
- **Category:** Dead code
- **Severity:** Low
- **Confidence:** Confirmed
- **Location(s):** `web/ErrorExceptionMapping.kt:5` (empty `object`), `utils/JwtProvider.kt:19` (`decodeJWT`, no callers), `src/main/resources/application.conf:1-13`
- **Description:** `application.conf` is never loaded (the app uses `embeddedServer(...)` in code); its `jwt` block, port and `modules = [...setup]` (not an Application module) are dead and diverge from `JwtProvider` (`audience` "jwt-audience" vs "ktor-audience").
- **Evidence:** `grep decodeJWT|ErrorExceptionMapping src/main` → only definitions.
- **Recommended Mitigation:** Delete, or adopt `application.conf` as the single config source (see DUP-001).

### [DEAD-003] Netty engine path never selected
- **Category:** Dead code
- **Severity:** Low
- **Confidence:** Likely
- **Location(s):** `AppConfig.kt:39-41`, `build.gradle:30`
- **Description:** `setup(isCio = true)` is called with the default from `App.kt:6` and `AppRule.kt:10`; the Netty branch and `ktor-server-netty` dependency are unused at runtime but still bring vulnerable Netty (SEC-006).
- **Evidence:** `fun setup(isCio: Boolean = true)`; no caller passes `false`.
- **Recommended Mitigation:** Remove Netty or make the engine configurable.

### 3.4 Duplicated Code

### [DUP-001] Repeated logic and duplicated configuration
- **Category:** Duplicated code
- **Severity:** Low
- **Confidence:** Confirmed
- **Location(s):** `UserService.kt:19,25` (hash expression); `User.kt:8-14,18-22,27-35` (validators); `UserRepository.kt:51-65` (`findByEmail`/`findByUsername`), `113-135` (`follow`/`unfollow`); `JwtProvider.kt:14-19` (verifier built twice); `AppConfig.kt:35` vs `application.conf:3` (port)
- **Description:** Near-identical blocks that raise change risk (the validators already diverge incorrectly, see DESIGN-001).
- **Evidence:** `String(base64Encoder.encode(Cipher.encrypt(user.password)))` appears twice.
- **Recommended Mitigation:** Extract a `PasswordHasher` port, a shared lookup helper, and a single config source.

### 3.5 Outdated Dependencies

### [DEPS-001] End-of-life framework stack
- **Category:** Outdated dependencies
- **Severity:** High
- **Confidence:** Likely
- **Location(s):** `build.gradle:4-12`, `gradle/wrapper/gradle-wrapper.properties:6`
- **Description:** Ktor 1.2.3 (Central's 1.x line ends at 1.6.8; current 3.6.1), Kotlin 1.3 (current stable 2.4.21), Gradle 4.10 (2018), Exposed 0.14.1 (unobtainable), Kodein 6.1.0. These no longer receive security patches.
- **Evidence:** Central metadata: `ktor-server-netty 3.6.1`, `ktor-jackson 1.6.8` (last 1.x), `kotlin-stdlib-jdk8 2.4.21` (latest stable by version filter).
- **Recommended Mitigation:** Migrate to Ktor 3.x, Kotlin 2.x, Gradle 8.x, Exposed 0.5x+, Kodein 7 (or Koin).

### [DEPS-002] Deprecated/major-lagging libraries
- **Category:** Outdated dependencies
- **Severity:** Medium
- **Confidence:** Likely
- **Location(s):** `build.gradle:15` (Unirest 1.4.9), `build.gradle:23` (JUnit 4.13.2), transitive `com.auth0:java-jwt:3.4.1` used directly in `Cipher.kt:3`, `JwtProvider.kt:3-5`
- **Description:** `com.mashape.unirest` 1.4.9 is the final release of the retired coordinates; java-jwt is two years of majors behind (3.4.1 → 4.6.2) and imported without being declared; JUnit 4 is legacy.
- **Evidence:** `gradle dependencies`: `com.auth0:java-jwt:3.4.1`; Central: java-jwt 4.6.2.
- **Recommended Mitigation:** Declare java-jwt explicitly at a current version; replace Unirest with Ktor client/`java.net.http`; move to JUnit 5.

### [DEPS-003] Minor-version lag
- **Category:** Outdated dependencies
- **Severity:** Low
- **Confidence:** Confirmed
- **Location(s):** `build.gradle:6-8,10` (Kodein 6.1.0→6.5.5; H2 2.2.224→2.5.252; slf4j-simple 2.0.9→2.0.20; HikariCP 5.1.0→7.1.0)
- **Description:** Behind latest stable on Central.
- **Evidence:** Central `maven-metadata.xml` queries (see Appendix).
- **Recommended Mitigation:** Let Dependabot (already configured) bump them after the platform upgrade.

### 3.6 Vulnerable Code (Security)

### [SEC-001] Hardcoded HMAC key signs JWTs → token forgery
- **Category:** Vulnerable code
- **Severity:** High
- **Confidence:** Confirmed
- **Location(s):** `utils/Cipher.kt:6`, `utils/JwtProvider.kt:15,28`, `AppConfig.kt:71-81`
- **Description:** The key (`some****-here`, redacted) is committed and used for both signing and verification. Anyone with the repo can mint a token with any `email` claim and the audience `ktor-audience` and authenticate as any user.
- **Evidence:** `val algorithm = Algorithm.HMAC256("<redacted literal>")`; `validate` only checks audience and looks up the user by the `email` claim.
- **Recommended Mitigation:** Load a random ≥256-bit secret from environment/secret manager; fail startup if absent; rotate the leaked key; consider asymmetric signing.

### [SEC-002] Jackson-databind 2.9.9 with 59 known advisories
- **Category:** Vulnerable code
- **Severity:** High
- **Confidence:** Likely
- **Location(s):** `build.gradle:19` (`ktor-jackson`), `AppConfig.kt:67-69`
- **Description:** OSV reports 59 advisories for the resolved version (12 Critical, 40 High, 7 Moderate, e.g. CVE-2019-14379, CVE-2020-8840). Request bodies are deserialized by Jackson (`ctx.receive`). Gadget-based polymorphic-typing RCEs likely need default typing, which is not enabled here; the DoS/parsing advisories are reachable. Severity kept at High because High/Critical CVEs are on a reachable parser.
- **Evidence:** OSV querybatch `com.fasterxml.jackson.core:jackson-databind@2.9.9` → 59 IDs (e.g. `GHSA-4w82-r329-3q67`).
- **Recommended Mitigation:** Upgrade via Ktor 3.x (Jackson ≥2.18); add request size limits.

### [SEC-003] Weak password storage; plaintext on update
- **Category:** Vulnerable code
- **Severity:** Medium
- **Confidence:** Confirmed
- **Location(s):** `UserService.kt:19,25,44-46`, `Cipher.kt:8-9`, `UserRepository.kt:86-88`
- **Description:** Passwords are "hashed" as unsalted HMAC-SHA256 under the same hardcoded key as the JWTs (fast, no work factor), compared with non-constant-time `==`. `update()` writes the supplied password unhashed, so updated passwords are stored in plaintext and then no longer match at login. Medium because exploitation needs a separate data-read path.
- **Evidence:** `userRepository.update(email, user)` passes `user.password` straight to `row[password] = user.password`.
- **Recommended Mitigation:** Use Argon2id/bcrypt/scrypt with per-user salt and constant-time verify; hash in `update`; migrate existing hashes.

### [SEC-004] Password material returned in API responses
- **Category:** Vulnerable code
- **Severity:** Medium
- **Confidence:** Confirmed
- **Location(s):** `UserService.kt:20` (echoes plaintext password), `UserService.kt:26,34` and `UserRepository.kt:31` (returns stored hash), `UserController.kt:36`, `UserControllerTest.kt:55`
- **Description:** `User` carries `password` and is serialized directly; register echoes plaintext, login/current-user return the hash.
- **Evidence:** `return user.copy(token = generateJwtToken(user))` (retains `password`).
- **Recommended Mitigation:** Introduce a response DTO without credentials; never serialize the domain `User`.

### [SEC-005] Exception details leaked; domain errors become HTTP 500
- **Category:** Vulnerable code
- **Severity:** Medium
- **Confidence:** Confirmed
- **Location(s):** `AppConfig.kt:82-89`, `web/ErrorExceptionMapping.kt`, `UserController.kt:28-33`
- **Description:** The catch-all returns `this.toString()` (class name + message) to clients; `NotFoundException`/`UnauthorizedException`/`IllegalArgumentException` all map to 500, so bad credentials return 500 not 401. An unknown email in a valid token also yields 500 in `validate`.
- **Evidence:** `ErrorResponse(mapOf("error" to listOf("detail", this.toString())))`.
- **Recommended Mitigation:** Map each domain exception to 401/404/422 with generic messages; log details server-side only.

### [SEC-006] Netty 4.1.37 with known advisories (not default engine)
- **Category:** Vulnerable code
- **Severity:** Medium
- **Confidence:** Likely
- **Location(s):** `build.gradle:30`; resolved `netty-codec-http/-http2/-handler/-common` 4.1.37.Final
- **Description:** OSV: codec-http 23 (1 Critical: GHSA-cqqj-4p63-rrmm request smuggling), http2 11, handler 8, common 2. Only reachable if `setup(isCio=false)`; default is CIO, so Medium.
- **Evidence:** OSV querybatch results for `io.netty:*@4.1.37.Final`.
- **Recommended Mitigation:** Remove the Netty dependency or upgrade with Ktor.

### [SEC-007] H2 PostgreSQL-protocol server with empty `sa` password
- **Category:** Vulnerable code
- **Severity:** Low
- **Confidence:** Possible
- **Location(s):** `DbConfig.kt:10`, `AppConfig.kt:40`
- **Description:** `Server.createPgServer().start()` opens a DB listener on every run (default port) with user `sa`/empty password. H2 defaults to local-only connections, so this is hardening (I did not run the server to confirm the bind address).
- **Evidence:** `AppConfig.kt:40: DbConfig.setup("jdbc:h2:mem:...", "sa", "")`.
- **Recommended Mitigation:** Remove the PG server (or dev-only flag); source credentials from env.

### [SEC-008] Email regex with overlapping quantifiers
- **Category:** Vulnerable code
- **Severity:** Low
- **Confidence:** Possible
- **Location(s):** `ext/String.kt:3-10`
- **Description:** `([a-zA-Z]+[\w-]+\.)+` has adjacent overlapping quantifiers (polynomial backtracking possible) and the regex is recompiled per call; TLD limited to 2–4 chars rejects valid addresses. Not tested for ReDoS.
- **Evidence:** `([a-zA-Z]+[\w-]+\.)+[a-zA-Z]{2,4})$`
- **Recommended Mitigation:** Precompile a simpler pattern with input length cap, or use a validated library.

### [SEC-009] Unpinned GitHub Actions and unvalidated wrapper
- **Category:** Vulnerable code
- **Severity:** Low
- **Confidence:** Confirmed
- **Location(s):** `.github/workflows/gradle.yml:25,27,31`
- **Description:** Actions pinned to mutable tags (`checkout@v3`, `setup-java@v3`, `gradle-build-action@v2`, the latter deprecated); no wrapper-jar validation.
- **Evidence:** `uses: gradle/gradle-build-action@v2`
- **Recommended Mitigation:** Pin by commit SHA; use `gradle/actions/setup-gradle` and wrapper validation.

### 3.7 Deviations from Best Practices

### [DESIGN-001] Inverted validation makes register/login/update reject valid input
- **Category:** Design deviations
- **Severity:** High
- **Confidence:** Confirmed (by reading; not executed because of BUILD-001)
- **Location(s):** `domain/User.kt:11-12,21,30-33`
- **Description:** `require(... && user.password.isNullOrBlank() && user.username.isNullOrBlank())` demands blank password/username/bio/image. Any real payload throws `IllegalArgumentException` → HTTP 500. Core flows are unusable and the `@Ignore`d tests hide it. Rated High as a critical business-logic defect (rubric's closest match).
- **Evidence:** `user.password.isNullOrBlank()` inside `validLogin()` at `User.kt:21`.
- **Recommended Mitigation:** Invert to `!isNullOrBlank()`; return 422 with field errors; cover with unit tests first.

### [DESIGN-002] Routing/layering defects
- **Category:** Design deviations
- **Severity:** Medium
- **Confidence:** Confirmed
- **Location(s):** `web/Router.kt:43-69,32-37`, `AppConfig.kt:91-96`
- **Description:** All `articles` routes sit inside a mandatory `authenticate` block (Router.kt:45), making the nested `authenticate(optional = true)` ineffective and public reads (`GET articles`, comments) require auth; handlers ignore controller return values; no `/api` prefix though RealWorld spec, tests and Travis expect it; `get("feed")` shares the group with `{slug}`.
- **Evidence:** `route("articles") { authenticate { get("feed") ... route("{slug}") { ... authenticate(optional = true) {` .
- **Recommended Mitigation:** Mount under `/api`, restructure auth per endpoint, have routes call `respond`.

### [DESIGN-003] Tight coupling; no ports; side effects in constructors
- **Category:** Design deviations
- **Severity:** Medium
- **Confidence:** Confirmed
- **Location(s):** `UserService.kt:12`, `ModulesConfig.kt:18-45`, `AppConfig.kt:60-64,77`, `UserRepository.kt:44-49`, `TagRepository.kt:14-18`, `UserController.kt:11-48`
- **Description:** Services depend on concrete `JwtProvider` object and repository classes; controllers take Ktor `ApplicationCall`; a global `object ModulesConfig` acts as service locator; repositories create schema in `init`; every authenticated request re-queries the DB and mints a new JWT (`getByEmail`), and the principal carries the password hash.
- **Evidence:** `UserService(private val jwtProvider: JwtProvider, private val userRepository: UserRepository)`.
- **Recommended Mitigation:** Define repository/token/hash interfaces (ports), keep Ktor out of controllers/domain, move schema creation to startup/migrations, make the principal a slim claims object.

### [DESIGN-004] Localized idiom and correctness issues
- **Category:** Design deviations
- **Severity:** Low
- **Confidence:** Likely
- **Location(s):** `UserRepository.kt:100-110` (follow-check constraint appears inverted – Possible), `116-120` (no duplicate-follow guard → PK violation 500), `AppConfig.kt:53` (`watchPaths` dev reload in prod), `UserController.kt:13-17` (`apply` used to return values), `ext/String.kt:10` (null-check on non-null receiver), `ErrorResponse` key `"error"` vs spec `"errors"`
- **Description:** Small deviations; no linter/formatter configured to prevent them.
- **Evidence:** `Follows.user eq Users.id and (Follows.follower eq userIdToFollow)`.
- **Recommended Mitigation:** Add ktlint/detekt, fix per item with tests.

## 4. Coverage Gaps and Limitations
- **Could not run:** unit/e2e tests, coverage (JaCoCo/Kover), `gradle dependencyUpdates`, compile-based unused warnings. The build fails (BUILD-001) and tests are ignored. The JDK 11 build run produced no captured output, so its result is unverified.
- **Not installed/not run:** semgrep, osv-scanner, gitleaks, trivy, jscpd, detekt, ktlint (not present; installation not attempted). Fallbacks: OSV HTTP API for CVEs on versions resolved in the patched copy; Maven Central `maven-metadata.xml` for latest versions; manual `grep` for secrets (one hardcoded key found); manual duplication/dead-code review.
- **CVE reachability** is reasoned from code, not exploited. Resolved versions come from the patched copy (only Exposed differs), so other versions should match the original.
- **Reviewed:** 100% of `src/main` (27 files) and CI/build files. Tests: `UserControllerTest.kt`, `AppRule.kt` read in detail; `Article/Comment/Profile/TagControllerTest.kt`, `HttpUtil.kt` only grep-sampled; `spec-api` collection not reviewed.
- **"Possible" findings needing human confirmation:** SEC-007, SEC-008, DESIGN-004 (follow-check inversion).

## 5. Appendix: Commands Executed
(Exit codes are shown where observed; shell-level exit codes for piped commands were not always captured.)
1. `git rev-parse HEAD; git status; git ls-files; find src -name '*.kt' | xargs wc -l; cat build.gradle …; java -version; which gradle semgrep osv-scanner gitleaks trivy jscpd detekt ktlint` — exit 1 (tools not installed); recon data collected.
2. `cat` of all `src/main` files with line numbers — exit 0.
3. `ls /usr/libexec/java_home -V` (JDK inventory: 8, 11, 14, 17, 22, 25) — exit 0.
4. `rsync -a --exclude .git --exclude build --exclude .gradle ./ <scratchpad>/copy/` — exit 0 (disposable copy).
5. `./gradlew build --no-daemon` (JDK 8, copy) — BUILD FAILED: Exposed 0.14.1 not found.
6. `curl maven-metadata.xml` for exposed; `curl -I jcenter` → HTTP 301.
7. `JAVA_HOME=jdk17 ./gradlew build --no-daemon` — BUILD FAILED, same error. JDK 11 run: no output captured.
8. `sed` patch of `exposed_version` to 0.17.14 in copy + `./gradlew build` — BUILD FAILED: Kotlin metadata 1.5.1 vs expected 1.1.16.
9. `./gradlew dependencies --configuration compileClasspath|runtimeClasspath --no-daemon` (copy) — exit 0; resolved versions recorded.
10. Appended `resolutionStrategy.force` to copy's `build.gradle` + rebuild — BUILD FAILED (Exposed 0.17.14 classes still incompatible).
11. `curl` Maven Central `maven-metadata.xml` for 14 artifacts — exit 0.
12. `curl -X POST https://api.osv.dev/v1/querybatch` + `python3 -I` summarizer + per-vuln `GET /v1/vulns/<id>` — exit 0; 59/23/11/8/2 advisories (jackson/netty modules), 0 for others.
13. `grep -rn` for secrets, `decodeJWT|ErrorExceptionMapping|Cipher`, `@Ignore`, `7000` — exit 0.
14. `git status --short | wc -l` → `0` (no changes to the audited tree).
