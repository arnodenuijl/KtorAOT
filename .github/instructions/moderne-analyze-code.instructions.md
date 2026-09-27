---
name: analyze-code
description: "Analyze code: find usages, locate callers, trace references, audit annotations, build an impact picture — read-only, repo-wide, recipe-driven."
---

# Analyze Code

This skill is for an agent working in a single repository's working tree via the moderne MCP server.

You are about to analyze code patterns across the repository without modifying it. The moderne MCP server's recipe-driven analysis path is more thorough than ad-hoc grepping because curated analysis recipes encode expert search logic — transitively-reachable call sites, framework-aware annotation surveys, cross-module dependency reports — that single-pattern grep misses.

Use the recipe path. The full path is three calls:

1. **`analyze_code`** — Search the OpenRewrite marketplace catalog for a curated analysis recipe. Phrase the query as a natural-language analysis goal (e.g. "find usages of types", "callers of a method", "impact of a change", "audit annotations"). Returns a paginated list of recipe names ranked by relevance, each annotated with `recipeCount`.

2. **`learn_recipe`** — Take a top hit and inspect its options and the DataTable schemas it emits. Do NOT guess or infer recipe names; only use names you have verified via `analyze_code`.

3. **`run_recipe`** — Execute the recipe. The result includes `searchResults` (per-file match descriptions) and, when the recipe found anything, a `runId` and `dataTables` summaries. Pass the `runId` to `query_datatable` (the `query-datatable` skill) to inspect the structured rows in detail.

## Picking the right recipe from `analyze_code` results

- Prefer recipes whose `displayName` begins with **"Find"**, **"List"**, **"Locate"**, **"Inventory"**, **"Audit"**, **"Report"** — these analysis recipes report without modifying.
- Avoid recipes whose `displayName` begins with **"Migrate"**, **"Change"**, **"Replace"**, **"Remove"**, **"Add"** — those mutate the codebase (use the `edit-code` skill instead if your goal is to apply a change).

## When NOT to use this skill

- For quick "where is this one type used?" or "callers of this one method?" lookups, the direct LST-typed primitives — `find_types`, `find_methods`, `find_implementations`, `find_annotations` (via the `find-symbols` skill) — are usually faster than the recipe catalog. Reach for `analyze_code` when you want a curated, multi-step analysis or a structured cross-cutting report.
- For applying a transformation (not analysis), use the `edit-code` skill instead.

## Anti-patterns

- **Guessing recipe names.** Never invent or abbreviate recipe names. Only use names that appeared verbatim in an `analyze_code` result.
- **Skipping `query_datatable`.** Analysis recipes emit their richest output as structured DataTables, not as free-form text in `searchResults`. After `run_recipe`, query the table via the `query-datatable` skill to filter, group, or count — don't re-read source files.
- **Re-searching the same intent.** If `analyze_code` returned a candidate set and `learn_recipe` told you the top match isn't what you want, pivot to a different phrasing or to a direct primitive. Don't keep refining the same query.
