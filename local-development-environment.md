# Local Development Environment

How to set up and run this project on your machine.

## Requirements

| Tool | Version | Notes |
|------|---------|-------|
| **JDK** | 21 | Used to run Gradle and compile the project (JVM target 17) |
| **Gradle** | 8.5 (via wrapper) | Always use `./gradlew` from the project root |

---

## Nix shell (recommended)

The repo includes [`shell.nix`](shell.nix), which provides JDK 21 and sets `JAVA_HOME` for you.

### Install Nix

`nix-shell` is included with the [Nix package manager](https://nixos.org/download/).

**Linux (multi-user install):**

```bash
sh <(curl -L https://nixos.org/nix/install) --daemon
```

Follow the prompts, then open a new terminal (or `source` the profile snippet the installer prints).

**Verify:**

```bash
nix --version
```

### Enter the development shell

From the project root:

```bash
cd kotlin-ktor-realworld-example-app
nix-shell
```

On first run, Nix downloads Azul Zulu JDK 21. You should see:

```
java 21, vim and zellij are now available
JAVA_HOME=/nix/store/...-zulu-ca-jdk-21...
```

### Verify Java

```bash
echo $JAVA_HOME
java -version
```

Both should reference JDK 21.

### Build and run

```bash
./gradlew clean build    # compile and run tests
./gradlew run            # start the API server on port 8080
```

Install the app distribution locally (optional):

```bash
./gradlew clean install  # output under build/install/api/
```

Run RealWorld API spec tests (server must be running):

```bash
./gradlew run &
APIURL=http://localhost:8080 ./spec-api/run-api-tests.sh
```

Leave the shell with `exit`.

---

## Manual JDK setup (without Nix)

Install **JDK 21** (e.g. [Azul Zulu 21](https://www.azul.com/downloads/?package=jdk#zulu) or Eclipse Temurin 21), then:

```bash
export JAVA_HOME=/path/to/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"

java -version
./gradlew clean build
./gradlew run
```

---

## `JAVA_HOME`

`./gradlew` uses **`JAVA_HOME` before `PATH`** when choosing a JVM:

1. If `JAVA_HOME` is set → `$JAVA_HOME/bin/java`
2. Otherwise → `java` from `PATH`

Because of that, `java -version` and `./gradlew` can use different JDKs if `JAVA_HOME` points somewhere else (IDE, shell profile, etc.).

**In nix-shell**, `shell.nix` sets both consistently:

```bash
export JAVA_HOME="${pkgs.zulu21}"
export PATH="$JAVA_HOME/bin:$PATH"
```

**Without nix-shell**, set `JAVA_HOME` yourself before running Gradle.

If you change JDK versions, stop existing Gradle daemons so they pick up the new Java:

```bash
./gradlew --stop
./gradlew clean build
```

---

## Quick reference

| Task | Command |
|------|---------|
| Enter dev environment | `nix-shell` |
| Check Java | `echo $JAVA_HOME && java -version` |
| Clean build + tests | `./gradlew clean build` |
| Run server | `./gradlew run` |
| Install distribution | `./gradlew clean install` |
| Stop Gradle daemons | `./gradlew --stop` |
| API spec tests | `./gradlew run &` then `APIURL=http://localhost:8080 ./spec-api/run-api-tests.sh` |

Server URL: [http://localhost:8080](http://localhost:8080)
