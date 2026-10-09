# KtorAOT

A small Kotlin sample project for testing **Kotlin**, **Ktor**, **JetBrains Exposed**, **Oracle Database** together.

The project exposes a JSON REST API for managing persons. It is a multi-module Gradle build and includes a KSP processor that generates serialization support for the domain and web API modules.

## Technology stack

- Kotlin 2.4
- Ktor 3.6 with the CIO engine
- JetBrains Exposed 1.5
- Oracle Database Free / Oracle 23ai-compatible container
- Shadow (fat) JAR
- Gradle Wrapper
- Docker Compose

## Project layout

| Module | Purpose |
| --- | --- |
| `Domain` | Serializable domain models and repository interface |
| `Persistence` | Exposed tables and Oracle-backed repository implementation |
| `SerializableProcessor` | KSP processor for generated serialization registration |
| `WebApi` | Ktor application, routes, OpenAPI/Swagger UI, and shadow JAR packaging |
| `scripts` | Oracle initialization scripts |

## Prerequisites

For a local JVM build:

- JDK 25
- Docker, if using the Oracle database container

For the containerized workflow, Docker is sufficient. The Dockerfile builds a shadow JAR with Temurin JDK 25 and runs it on Temurin JRE 25.

## Run with Docker Compose

From the repository root:

```powershell
docker compose up --build
```

This starts:

- Oracle Database on `localhost:1521`
- The Ktor API on `http://localhost:8080`

For a direct image build:

```powershell
docker build -f WebApi/Dockerfile -t ktor-service .
```

The API container connects to the database using `ORACLE_HOST=oracle-23ai`. The application uses the following database connection defaults:

| Setting | Value |
| --- | --- |
| Host | `localhost` locally, `oracle-23ai` in Compose |
| Port | `1521` |
| Service | `FREEPDB1` |
| User | `ktor` |
| Password | `ktor` |

The Oracle container is initialized from the SQL files in `scripts`. The database may take a few minutes to become healthy on its first start.

Stop the services with:

```powershell
docker compose down
```

## Build and run locally

Start Oracle separately:

```powershell
docker compose up -d oracle-23ai
```

Then build the project:

```powershell
.\gradlew.bat build
```

Run the WebApi module:

```powershell
.\gradlew.bat :WebApi:run
```

When Oracle is not running on `localhost`, set `ORACLE_HOST` before starting the application:

```powershell
$env:ORACLE_HOST = "your-oracle-host"
.\gradlew.bat :WebApi:run
```

## Build a shadow JAR

```powershell
.\gradlew.bat :WebApi:shadowJar
```

The JAR is generated at `WebApi/build/libs/ktor-service.jar`; run it with:

```powershell
java -jar WebApi\build\libs\ktor-service.jar
```

## API

The server listens on port `8080`.

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/` | Returns a greeting |
| `GET` | `/greet/{name}` | Returns a personalized greeting |
| `GET` | `/listDir` | Lists the application directory |
| `GET` | `/persons` | Lists all persons |
| `GET` | `/persons/{id}` | Gets one person |
| `POST` | `/persons` | Creates a person |
| `PUT` | `/persons/{id}` | Updates a person |
| `DELETE` | `/persons/{id}` | Deletes one person |
| `DELETE` | `/persons` | Deletes all persons |
| `GET` | `/swagger` | Opens the generated Swagger UI |

Example request:

```powershell
curl.exe -X POST http://localhost:8080/persons `
  -H "Content-Type: application/json" `
  -d '{"id":0,"name":"Ada Lovelace","age":36,"details":{"address":"London","phoneNumber":"+44 20 0000 0000"}}'
```

## Notes

- The application inserts two example persons during startup.
- The JDBC URL targets the `FREEPDB1` pluggable database.
- Generated build output is ignored by Git; use the Gradle tasks above to recreate it.
