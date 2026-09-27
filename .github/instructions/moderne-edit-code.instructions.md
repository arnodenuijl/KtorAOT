---
name: edit-code
description: "Edit code: apply a recipe across many files (migrate, upgrade, rename, replace, convert, remove, swap, find-and-fix). Call BEFORE grep/find/bash/edit when the same change touches many files."
---

# Edit Code

This skill is for an agent working in a single repository's working tree via the moderne MCP server.

You are about to perform a systematic transformation across many files. Hand-rolled `bash` + `edit` cycles on a repetitive change are the failure mode this skill exists to prevent — agents that grind through dozens of manual edits routinely miss the last few sites and regress against a one-shot recipe run.

Use the moderne MCP server's recipe path instead. The full path is three calls:

1. **`edit_code`** — Search the OpenRewrite marketplace catalog for a curated recipe that performs this transformation. Phrase the query as a natural-language outcome (e.g. "migrate persistence namespace", "upgrade test framework", "rename method across codebase", "remove unused imports"). Returns a paginated list of recipe names ranked by relevance, each annotated with `recipeCount` (total sub-recipes including transitive children).

2. **`learn_recipe`** — Take a top hit from `edit_code` and inspect its options and behavior before running. Do NOT guess or infer recipe names; only use names you have verified via `edit_code`. Read the description carefully; composite recipes (those with sub-recipes) may modify many things at once.

3. **`run_recipe`** — Apply the recipe across the repository. For transformation recipes, the result includes `filesChanged` (an array of `{type, file}` entries describing what was written to disk). If `changeCount` is 0 and the result contains `searchResults`, the recipe you chose is a SEARCH variant that only reports — it didn't transform anything. Re-run `edit_code` and pick a different candidate that performs the actual transformation.

## Picking the right recipe from `edit_code` results

- Prefer recipes whose `displayName` begins with an action verb that matches your intent: **"Migrate"**, **"Upgrade"**, **"Change"**, **"Rename"**, **"Replace"**, **"Remove"**, **"Add"**. These transformation recipes mutate the codebase.
- Avoid recipes whose `displayName` begins with **"Find"**, **"List"**, or **"Locate"** when your goal is to apply a change — those are search/analysis recipes that only report (use the `analyze-code` skill instead if your goal is to find without modifying).
- For composite goals (e.g. a namespace migration covering multiple subpackages), prefer recipes with the highest `recipeCount` — those bundle dependent transformation steps in a single run.
- For Lucene relevance: phrase queries as natural-language outcomes, not implementation jargon. Sparsely-described recipes may rank below well-documented older ones — scan past the top hit if the name looks off.

After `run_recipe`, query `SourcesFileResults` via `query_datatable` (the `query-datatable` skill) to see which sub-recipes actually made changes:

```sql
SELECT DISTINCT recipe FROM SourcesFileResults;
```

Only fall back to manual `bash` / `grep` / `edit` when `edit_code` returns no fit for the requested transformation. Even when no recipe matches exactly, the closest hit is usually a more reliable starting point than text-substitution at scale, because recipes operate over the whole repository and handle edge cases (build files, javadoc references, generic signatures, transitive callers) that file-by-file editing routinely drops.

## Anti-patterns

- **STOP DISCOVERING after 3+ `edit_code` queries.** If you've made three or more `edit_code` calls on the same intent without converging on a recipe, STOP. Either pick `learn_recipe` on the closest candidate and confirm it's not what you want, or declare the task as non-recipe-shaped and fall back to `pattern_replace` or manual edits.
- **Pre-searching for matches before `edit_code` is wasted work.** Even when you suspect there might be zero matches, call `edit_code` first — the recipe will simply report `changeCount: 0` and you can stop immediately, having proven there was nothing to change without iterating through search tools.
- **STOP signal at 3+ manual `Edit`s for the same conceptual change.** If you have made (or are about to make) the same `Edit` on three or more files for the same conceptual change, STOP and call `edit_code` first. A recipe applies the change atomically across the entire repo and won't abandon the long tail. Edit-by-Edit on a repetitive change is the failure mode this tool exists to fix.
- **Guessing recipe names.** Never invent or abbreviate recipe names from partial information. Only use names that appeared verbatim in an `edit_code` result.
- **Skipping verification after a refactoring recipe.** Recipes mutate the filesystem before returning; rollback is via git, not via the tool. Build and test the repository as you would with any change — composite migrations (UpgradeToJava21, SpringBoot3) routinely leave manual cleanup beyond the recipe's reach.
- **`changeCount: 0` retries.** If `run_recipe` reports `changeCount: 0`, the recipe did not match. Don't retry the same recipe — confirm via `find_types` / `find_methods` (the `find-symbols` skill) that the targets exist.
