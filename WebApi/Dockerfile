# syntax=docker/dockerfile:1
#
# Multi-stage Dockerfile that builds the WebApi module into an executable JAR
# with Kotlin Toolchain and runs it on a Temurin JRE image.
#
# IMPORTANT: build context must be the repository ROOT (multi-module project), e.g.:
#   docker build -f WebApi/Dockerfile -t ktor-service .
#############################################
# Stage 1: warm the Kotlin Toolchain cache
#############################################
FROM eclipse-temurin:25-jdk AS toolchain-cache
RUN apt-get update && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /workspace
COPY kotlin project.yaml libs.versions.toml ./
COPY WebApi/module.yaml WebApi/module.yaml
COPY Domain/module.yaml Domain/module.yaml
COPY Persistence/module.yaml Persistence/module.yaml
# Strip CRLF in case the wrapper was checked out with Windows line endings
RUN sed -i 's/\r$//' kotlin \
    && chmod +x kotlin \
    && ./kotlin --version
#############################################
# Stage 2: build the executable JAR
#############################################
FROM toolchain-cache AS build
WORKDIR /workspace
COPY Domain Domain
COPY Persistence Persistence
COPY WebApi WebApi
RUN ./kotlin package -m WebApi -f executable-jar \
    && cp build/tasks/_WebApi_executableJarJvm/WebApi-jvm-executable.jar /workspace/webapi.jar
#############################################
# Stage 3: runtime image
#############################################
FROM eclipse-temurin:25-jre AS runtime
RUN groupadd -r ktor && useradd -r -g ktor -s /sbin/nologin ktor
WORKDIR /app
COPY --from=build --chown=ktor:ktor /workspace/webapi.jar /app/webapi.jar
USER ktor
EXPOSE 9000
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75", "-jar", "/app/webapi.jar"]
