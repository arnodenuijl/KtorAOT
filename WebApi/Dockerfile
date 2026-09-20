# syntax=docker/dockerfile:1
#
# Multi-stage Dockerfile that compiles the WebApi module into a GraalVM Native
# Image executable (Linux/amd64, G1 GC) and packages it into a minimal runtime
# image.
#
# IMPORTANT: build context must be the repository ROOT (this is a multi-module
# Gradle project), e.g.:
#   docker build -f WebApi/Dockerfile -t ktor-service .
#
# G1 GC in Native Image is only available in Oracle GraalVM (not GraalVM
# Community Edition) on Linux amd64/aarch64, which is why the official Oracle
# GraalVM native-image container is used as the build image.

#############################################
# Stage 1: warm the Gradle wrapper/dependency cache
# (kept separate so this expensive layer is cached as long as the build
# scripts themselves don't change, even when application sources change)
#############################################
FROM container-registry.oracle.com/graalvm/native-image:25 AS gradle-cache
WORKDIR /workspace

COPY gradlew gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY WebApi/build.gradle.kts WebApi/build.gradle.kts
COPY Domain/build.gradle.kts Domain/build.gradle.kts
COPY Persistence/build.gradle.kts Persistence/build.gradle.kts
COPY SerializableProcessor/build.gradle.kts SerializableProcessor/build.gradle.kts

RUN chmod +x gradlew \
    && ./gradlew --no-daemon --version

#############################################
# Stage 2: build the native executable
#############################################
FROM gradle-cache AS build
WORKDIR /workspace

COPY Domain Domain
COPY Persistence Persistence
COPY SerializableProcessor SerializableProcessor
COPY WebApi WebApi

# --gc=G1, --no-fallback, etc. are already configured in WebApi/build.gradle.kts
RUN ./gradlew --no-daemon :WebApi:nativeCompile
RUN cat /workspace/WebApi/build/native/generated/generateResourcesConfigFile/resource-config.json
#############################################
# Stage 3: minimal runtime image
#############################################
FROM container-registry.oracle.com/os/oraclelinux:10-slim AS runtime

RUN groupadd -r ktor && useradd -r -g ktor -s /sbin/nologin ktor

WORKDIR /app
COPY --from=build --chown=ktor:ktor \
    /workspace/WebApi/build/native/nativeCompile/ktor-service /app/ktor-service

USER ktor
EXPOSE 8080
ENTRYPOINT ["/app/ktor-service"]
