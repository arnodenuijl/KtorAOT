# syntax=docker/dockerfile:1
#
# Multi-stage Dockerfile that builds the WebApi module into a shadow (fat) JAR
# and runs it on a Temurin JRE image.
#
# IMPORTANT: build context must be the repository ROOT (this is a multi-module
# Gradle project), e.g.:
#   docker build -f WebApi/Dockerfile -t ktor-service .
#############################################
# Stage 1: warm the Gradle wrapper cache
#############################################
FROM eclipse-temurin:25-jdk AS gradle-cache
WORKDIR /workspace
COPY gradlew gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY WebApi/build.gradle.kts WebApi/build.gradle.kts
COPY Domain/build.gradle.kts Domain/build.gradle.kts
COPY Persistence/build.gradle.kts Persistence/build.gradle.kts
RUN chmod +x gradlew \
    && ./gradlew --no-daemon --version
#############################################
# Stage 2: build the shadow JAR
#############################################
FROM gradle-cache AS build
WORKDIR /workspace
COPY Domain Domain
COPY Persistence Persistence
COPY WebApi WebApi
RUN ./gradlew --no-daemon :WebApi:shadowJar
#############################################
# Stage 3: runtime image
#############################################
FROM eclipse-temurin:25-jre AS runtime
RUN groupadd -r ktor && useradd -r -g ktor -s /sbin/nologin ktor
WORKDIR /app
COPY --from=build --chown=ktor:ktor \
    /workspace/WebApi/build/libs/ktor-service.jar /app/ktor-service.jar
USER ktor
EXPOSE 9000
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75", "-jar", "/app/ktor-service.jar"]
