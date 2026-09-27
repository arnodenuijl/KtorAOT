---
name: inspect-status
description: "Inspect status: confirm the LST, trigram index, or build tool is ready before calling a recipe / type-aware search / structural rewrite. Use to disambiguate empty results."
---

# Inspect Status

This skill is for an agent working in a single repository's working tree via the moderne MCP server.

Three introspection tools tell you whether the data layer behind the other moderne tools is ready. Use these BEFORE acting on a tool whose results would be unreliable if the underlying index / LST hasn't built yet.

| tool | reports | needed before |
|---|---|---|
| `build_info` | build tool (Gradle/Maven/Bazel/…) + canonical compile + test commands + wrapper presence | running any `./gradlew` / `mvn` / similar from `Bash` |
| `build_status` | trigram-index state under `searchIndex`: `available`, `fileCount`, `shardCount`, `symbolCount` | `trigrep_search`, `trigrep_structural_search`, symbol-aware queries |
| `lst_status` | LST state (`READY` / `BUILDING` / `PARTIALLY_READY` / `NOT_STARTED` / `FAILED`), source file count, pending incremental updates | any recipe-backed tool (`find_*`, `change_*`, `pattern_replace`, `run_recipe`) |

## When to use this skill

- About to call `trigrep_search` or `trigrep_structural_search` — call `build_status` first.
- About to call any `find_*` / `change_*` / `pattern_replace` / `run_recipe` tool — call `lst_status` first.
- About to invoke a build / test command via `Bash` — call `build_info` once.
- A previous tool returned empty or suspicious results and you need to distinguish "no matches" from "index/LST not ready".
- You want to know whether symbol search (`sym:name`) will work — it depends on `build_status.searchIndex.symbolCount > 0`.

## When NOT to use this skill

- Calling once per turn or per search is wasteful. Cache the answer for the rest of the task.
- For per-file enumeration of symbols, use `symbols_overview` (via `find-symbols` skill), not `lst_status`.
- If a search returned the answer you needed, don't ask the status retroactively.

## Examples

Routine: at the start of a task that will use moderne tools:

```
lst_status()       # → {state: READY, sourceFileCount: 1234}  (plus fileWatcher.pendingChanges while edits are pending)
build_status()     # → {searchIndex: {available: true, fileCount: 4221, shardCount: 3, symbolCount: 18432}}
build_info()       # → {buildTool: "gradle", compileCommand: "./gradlew classes", testCommand: "./gradlew test"}
```

If `lst_status.state` is not `READY`:

```
# Fall back to grep / trigrep for the immediate need; LST builds in the background.
trigrep_search(query = "...")
```

If `build_status.searchIndex.symbolCount == 0`:

```
# Use plain text / regex queries; skip sym: queries.
trigrep_search(query = "TODO lang:java")
```

## Anti-patterns

- **Calling status checks per-search.** Call once at the start of a task, cache the answer.
- **Treating an empty result as "no matches" without checking status.** Empty + `lst_status != READY` = LST not built. Empty + `lst_status == READY` = truly no matches.
- **`@Annotation` lookups through `trigrep_search`.** `@Foo` is always a text match, so it also hits Javadoc, strings, and same-named annotations from other packages. Use `annotated:` or `find_annotations` (via the `find-symbols` skill).
- **Guessing the build command.** Use `build_info.compileCommand` and `build_info.testCommand` verbatim — they encode wrapper detection (`./gradlew` vs system `gradle`) and project conventions.
- **Polyglot repos.** `build_info` reports the FIRST detected build tool. For polyglot repos, you may need to walk subdirectories manually.
- **Stale indexes.** Both the trigram index and the LST can be slightly stale within seconds of large file changes. Treat very-recent edits with caution.
