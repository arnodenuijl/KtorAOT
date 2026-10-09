# KtorAOT

A small Kotlin sample project for testing **Kotlin**, **Ktor**, **JetBrains Exposed**, **Oracle Database** together.

The project exposes a JSON REST API for managing persons. It is a multi-module project built with the [Kotlin Toolchain](https://kotlin-toolchain.org/) (declarative `project.yaml` / `module.yaml`, no Gradle).

## Technology stack

- Kotlin 2.4
- Ktor 3.6 with the CIO engine
- JetBrains Exposed 1.5
- Oracle Database Free / Oracle 23ai-compatible container
- Kotlin Toolchain (`./kotlin` wrapper)
- Executable JAR packaging
- Docker Compose

## Project layout

| Module | Purpose |
| --- | --- |
| `Domain` | Serializable domain models and repository interface |
| `Persistence` | Exposed tables and Oracle-backed repository implementation |
| `WebApi` | Ktor application, routes, OpenAPI/Swagger UI, and executable JAR packaging |
| `scripts` | Oracle initialization scripts |

## Prerequisites

For a local build (the `kotlin` wrapper downloads the Toolchain and provisions the JDK automatically):

- Docker, if using the Oracle database container

For the containerized workflow, Docker is sufficient. The Dockerfile builds an executable JAR with the Kotlin Toolchain on Temurin JDK 25 and runs it on Temurin JRE 25.

## Run with Docker Compose

From the repository root:

```powershell
docker compose up --build
```

This starts:

- Oracle Database on `localhost:1521`
- The Ktor API on `http://localhost:9000`

For a direct image build:

```powershell
docker build -f WebApi/Dockerfile -t ktor-service .
```

The API container connects to the database using `ORACLE_HOST=oracle`. The application uses the following database connection defaults:

| Setting | Value |
| --- | --- |
| Host | `localhost` locally, `oracle` in Compose |
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
docker compose up -d oracle
```

Then build the project:

```powershell
.\kotlin.bat build
```

Run the WebApi module:

```powershell
.\kotlin.bat run -m WebApi
```

When Oracle is not running on `localhost`, set `ORACLE_HOST` before starting the application:

```powershell
$env:ORACLE_HOST = "your-oracle-host"
.\kotlin.bat run -m WebApi
```

## Package an executable JAR

```powershell
.\kotlin.bat package -m WebApi -f executable-jar
```

The JAR is generated at `build/tasks/_WebApi_executableJarJvm/WebApi-jvm-executable.jar`; run it with:

```powershell
java -jar build\tasks\_WebApi_executableJarJvm\WebApi-jvm-executable.jar
```

Run the tests with `.\kotlin.bat test` and all checks with `.\kotlin.bat check`.

## API

The server listens on port `9000`.

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/personApi/` | Returns a greeting |
| `GET` | `/personApi/greet/{name}` | Returns a personalized greeting |
| `GET` | `/personApi/listDir` | Lists the application directory |
| `GET` | `/personApi/persons` | Lists all persons |
| `GET` | `/personApi/persons/{id}` | Gets one person |
| `POST` | `/personApi/persons` | Creates a person |
| `PUT` | `/personApi/persons/{id}` | Updates a person |
| `DELETE` | `/personApi/persons/{id}` | Deletes one person |
| `DELETE` | `/personApi/persons` | Deletes all persons |
| `GET` | `/personApi/swagger` | Opens the generated Swagger UI |

Example request:

```powershell
curl.exe -X POST http://localhost:9000/personApi/persons `
  -H "Content-Type: application/json" `
  -d '{"id":0,"name":"Ada Lovelace","age":36,"details":{"address":"London","phoneNumber":"+44 20 0000 0000"}}'
```

## Notes

- The application inserts two example persons during startup.
- The JDBC URL targets the `FREEPDB1` pluggable database.
- Generated build output is ignored by Git; use the `kotlin` commands above to recreate it.
