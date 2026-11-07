# syntax=docker/dockerfile:1
# Base images pinned by digest. Re-resolve and re-scan (docker scout / trivy) when bumping.
ARG JDK_IMAGE=eclipse-temurin:17-jdk-noble@sha256:ec2c8050c856a00986776ddd94d4facc9e872430c27799108ca318a26f9f1887
ARG JRE_IMAGE=eclipse-temurin:17-jre-alpine@sha256:e2ba4e947bd5e236a9866fbdda03e4936036cb9e9d16ea0d84d103d6634a5c21

# ---- build: compiles, runs tests, produces the distribution ----
FROM ${JDK_IMAGE} AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle gradle.lockfile ./
COPY gradle gradle
RUN ./gradlew --no-daemon --version
COPY src src
RUN ./gradlew --no-daemon clean build installDist

# ---- runtime ----
FROM ${JRE_IMAGE} AS runtime
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/build/install/api/ ./
USER app
EXPOSE 8080
ENTRYPOINT ["/app/bin/api"]
