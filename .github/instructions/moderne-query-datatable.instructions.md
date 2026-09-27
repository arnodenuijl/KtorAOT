---
name: query-datatable
description: "Query DataTable: SELECT / SHOW / DESCRIBE SQL against tables emitted by run_recipe, find_*, or symbols_overview. Filter / group / count without re-scanning files."
---

# Query DataTable

This skill is for an agent working in a single repository's working tree via the moderne MCP server.

DataTables are tabular outputs (rows + typed columns) emitted by recipes and LST primitives. They are stored in an in-memory DuckDB instance and addressed by `runId`. `query_datatable` is how you read them.

## What it does

Executes SQL (`SELECT`, `SHOW`, `DESCRIBE` only — read-only) against the DataTables produced by a previous recipe or LST primitive run. Returns `{columns, rows, rowCount}`. Results are capped at 500 rows; past that, `hasMore: true` is set and you page with `LIMIT` / `OFFSET` in the SQL.

## When to use

- Right after `run_recipe`, `find_methods`, `find_types`, `find_annotations`, `find_implementations`, or `symbols_overview` returns a `runId` and you want a precise breakdown.
- You need to answer "how many?", "group by what?", "filter by file / package / called type?" without re-scanning source files.
- You want a count before deciding whether to run a transformation across all matches.

## When NOT to use

- Before any run-producing tool has been called — there are no tables to query yet.
- For free-text search of source code — that's `trigrep_search` / `grep`.

## Examples

Inspect what tables and columns exist for a recent run:

```sql
SHOW TABLES;
DESCRIBE MethodCalls;
```

Count call sites per calling file after `find_methods`:

```sql
SELECT sourceFile, COUNT(*) AS calls
FROM MethodCalls
GROUP BY sourceFile
ORDER BY calls DESC;
```

Filter results by file path after `find_types`:

```sql
SELECT * FROM TypeUses
WHERE sourceFile LIKE 'src/main/%'
AND concreteType LIKE 'org.springframework.%';
```

Common tables to know:

- `SearchResults` / `SourcesFileResults` (columns include `sourcePath`, `afterSourcePath`, `recipe`) — produced by recipe runs that find or change something.
- `MethodCalls` — produced by `find_methods`. Columns: `sourceFile`, `method`, `className` (the called method's declaring type, not the caller), `methodName`, `argumentTypes`.
- `TypeUses` — produced by `find_types`. Columns: `sourceFile`, `code`, `concreteType`.
- `find_annotations` has no table of its own; its matches are in `SearchResults`.

## Anti-patterns

- **Guessing column names.** If you don't know the schema, run `SHOW TABLES` then `DESCRIBE <tablename>` before composing a SELECT. The error response also lists every loaded table with full columns — read it instead of guessing again.
- **Omitting `run`.** Without it, the most recent run is used — fine for single-task flows but wrong if multiple recipes ran in the same session. Pass the `runId` from the recipe response as `run`.
- **Re-reading files** when you already have the answer in a table. The whole point is structured queries instead of file scans.
