# KtorAOT - Copilot instructions

## Build system

This is a **Kotlin Toolchain** project (JetBrains, formerly Amper), not Gradle or Maven. Configuration lives in `project.yaml` (root, lists modules) and `module.yaml` (per module). Use the wrapper, not a globally installed CLI:

- Windows: `.\kotlin.bat <command>`
- Unix: `./kotlin <command>`

The wrapper pins the CLI version (currently `0.13.0`) and verifies its SHA256. On first run it downloads a JRE and the CLI into `%LOCALAPPDATA%\JetBrains\Kotlin\cli`.

### Commands

```powershell
.\kotlin.bat build
.\kotlin.bat run -m WebApi
.\kotlin.bat package -m WebApi -f executable-jar   # -> build/tasks/_WebApi_executableJarJvm/WebApi-jvm-executable.jar
.\kotlin.bat test
.\kotlin.bat check
```

Run a single test (fully qualified name, optionally `/`-separated for nested classes and with parameter types):

```powershell
.\kotlin.bat test -m ConsoleClient --include-test="PersonApiClientTest.returns null on 404"
.\kotlin.bat test --include-classes=PersonApiClientTest
```

Filters: `-m/--include-module`, `--include-classes`, `--include-test`, `--include-tag`/`--exclude-tag`, `-p/--platform`. Run `.\kotlin.bat test --help` for the full list.

## Modules and architecture

Root `project.yaml` includes four modules. Dependencies flow inward toward `Domain`:

- **Domain** (`jvm/lib`): `Person`, `PersonDetails` (both `@Serializable`) and the `IPersonRepository` interface. Only depends on kotlinx-serialization-json. Holds no persistence code.
- **Persistence** (`jvm/lib`): depends on `../Domain` and Exposed. `PersonTable` (`IdTable<Uuid>`, table `persons`, columns `ID`, `NAME`, `AGE`, `DETAILS` as JSON) and `PersonRepository(database)`, which implements `IPersonRepository`.
- **WebApi** (`jvm/app`, main class `ApplicationKt`): Ktor 3.6 CIO server on port 9000 with the Ktor compiler plugin (OpenAPI generation enabled). Depends on Persistence and Domain. Routes live under `/personApi`, with Swagger UI at `swagger`. Routes and the entry point are in `WebApi/src/main.kt`.
- **ConsoleClient** (`jvm/app`): a Ktor client (`PersonApiClient`, CIO engine) that calls the WebApi. Its `Person` type is a separate flat model (`address`, `phoneNumber`) and does not reuse the Domain `Person`. Tests use `ktor-client-mock`.

Each module has a single source root: `src/` for main code and `test/` for tests (ConsoleClient). Source files have no `package` declarations and use the default package.

## Runtime and database

- Oracle Free (`container-registry.oracle.com/database/free`), service `FREEPDB1`, user `ktor`/`ktor`, port 1521.
- `WebApi/src/main.kt` hardcodes the JDBC connection and reads the host from the `ORACLE_HOST` env var (default `localhost`). Docker Compose sets it to `oracle`.
- Schema is in `scripts/01_tablespace.sql` and `scripts/02_persons_ddl.sql`. The `details` JSON column has a `VALIDATE` check constraint that requires `address` and `phoneNumber`.
- On startup, WebApi inserts two sample persons with fixed IDs (`Uuid.fromLongs(0,1)`, `(0,2)`). Re-inserting them on every start can collide with existing rows, so check this before changing the seed logic.

Docker:

- Build from the repo root, since the Dockerfile copies the whole project: `docker build -f WebApi/Dockerfile -t ktor-service .`
- The Dockerfile strips CRLF from `kotlin` before running it. Keep this step when editing the Dockerfile.
- `docker compose up` starts `webapi` (9000) and `oracle` (1521). Oracle has a 120s healthcheck start period.

## Conventions

- Dependency versions come from `libs.versions.toml`. In `module.yaml`, reference them as `$libs.<name>` with dashes turned into dots (for example `$libs.kotlinx.serialization.json`).
- Entities use `kotlin.uuid.Uuid` IDs. Repository methods wrap each DB call in their own `transaction(database) { }`.
- Each OpenAPI-documented route sets a `describe { summary = ... }` block.
- `@file:OptIn(ExperimentalKtorApi::class)` is needed at the top of files that use experimental Ktor APIs (see `WebApi/src/main.kt`).
- `ConsoleClient/module.yaml` lists `$libs.logback.classic` twice. Remove the duplicate if you touch that file.
