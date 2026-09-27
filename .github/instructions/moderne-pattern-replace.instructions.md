---
name: pattern-replace
description: "Pattern replace: compile a one-shot Kotlin recipe DSL (`rewrite { } to { }`) to rewrite a structural pattern across the repo when no marketplace recipe matches. Try edit-code first."
---

# Pattern Replace

This skill is for an agent working in a single repository's working tree via the moderne MCP server.

`pattern_replace` compiles a Kotlin recipe-DSL source at runtime (kotlinc + the rewrite-kotlin K2 compiler plugin) and runs the resulting recipe over the repository. It's the "atomic structural rewrite without a marketplace recipe" tool — bigger than `Edit`, smaller than authoring a full Java recipe class.

## What it does

You supply a Kotlin source declaring one or more top-level `val X: Recipe = recipe(...) { ... }` properties. The tool compiles it, runs it over the LST, and returns `filesChanged` (`{type: MODIFY|ADD|DELETE, file}`) plus a `changeCount`. Files are mutated on disk before the call returns.

## When to use this skill

- You're about to repeat the same structural `Edit` on more than ~3 sites (e.g. `Arrays.asList(...)` → `List.of(...)`).
- `edit_code` (via the `edit-code` skill) returned no fit for the rewrite — there's no canned marketplace recipe for what you need.
- The change is too structural for `grep` + `edit` but doesn't justify hand-writing a full recipe class.

## When NOT to use this skill

- Try `edit_code` (via `edit-code` skill) FIRST. Only fall back to `pattern_replace` when no marketplace recipe matches the intent.
- For pure type or method renames, `change_type` / `change_method_name` (via the `change-symbols` skill) are simpler and don't require writing a DSL source.
- For one-off edits on 1-3 specific files, just use `Edit` directly.

## DSL shape (canonical v1)

```kotlin
import org.openrewrite.Recipe
import org.openrewrite.recipe

val ArraysAsListToListOf: Recipe = recipe(
    displayName = "Use `List.of` instead of `Arrays.asList`",
    description = "Replace immutable Arrays.asList with List.of.",
) {
    edit {
        rewrite { a: Any, b: Any -> java.util.Arrays.asList(a, b) } to { a, b -> java.util.List.of(a, b) }
    }
}
```

Notes:

- The whole recipe is one top-level `val` of type `Recipe`. The two `import` lines are required.
- `edit { rewrite { before } to { after } }` is the canonical pattern shape. The `before` lambda's parameters bind the pattern's substitution sources; the `after` lambda receives the same parameters by name.
- To match a method call, write the call **receiver-first** in the before lambda: `{ s: String -> s.length() }`, NOT `{ s: String -> java.lang.String.length(s) }`. The matcher reads the call shape, not the static-form rewrite.
- For Java-targeted recipes (the common case), use **fully-qualified Java types** (`java.util.Arrays`, `java.util.List`) so the K2 compiler resolves them as Java methods. The v2 dispatch then routes the recipe through `JavaTemplate` and matches Java sources.
- Do NOT call Kotlin-stdlib extensions (`s.lowercase()`, `sb.appendLine()`, `enumValues<E>()`) unless you want the recipe to match Kotlin sources only — the dispatcher promotes any `kotlin.*` callee to the Kotlin helper.
- `recipe(...)` needs a display name and a description, passed by name as above or positionally: `recipe("Display name", "Description.") { ... }`.

## Examples

Replace every `String.valueOf(o)` with `Objects.toString(o)`:

```kotlin
import org.openrewrite.Recipe
import org.openrewrite.recipe

val ObjectsToString: Recipe = recipe("Use `Objects.toString`", "Replace `String.valueOf(Object)` with `Objects.toString(Object)`.") {
    edit {
        rewrite { o: Any -> java.lang.String.valueOf(o) } to { o -> java.util.Objects.toString(o) }
    }
}
```

The `after` lambda sees only its own parameters — there is no enclosing class, so a field like `logger` won't resolve.

Pre-flight on a subset (`files` parameter) before running repo-wide; verify the diff matches intent. After running, build and test as you would any change.

## Anti-patterns

- **Skipping `edit_code` first.** If a marketplace recipe exists, use it — it's vetted, has options, and emits richer DataTables. `pattern_replace` is the fallback for the gap case.
- **Multi-statement `before` lambdas.** The before-lambda body must be a single method call or property access. The DSL rejects multi-statement before-blocks.
- **Chain rewrites on Java sources.** The DSL's chain shape (`xs.filter(p).any()`) currently routes through the Kotlin helper only; for chain rewrites on Java sources, write two recipes or call a canonical recipe via `run_recipe`.
- **Ignoring `changeCount`.** `changeCount: 0` triggers a diagnostic block explaining likely causes (no callsite of the matched method, type mismatch, Kotlin-stdlib callee in the lambda body promoting the recipe to Kotlin-only). Read the diagnostic before retrying.
- **Static-form rewrites for instance methods.** `{ s: String -> java.lang.String.length(s) }` won't match `s.length()` calls — the matcher reads call shape. Always write the call as the agent would: receiver-first.
- **Promoting the recipe to Kotlin-only by accident.** Any `kotlin.*` callee in the lambda body (including stdlib extensions) bumps the recipe out of Java-source matching. If the goal is Java rewrites, stay within `java.*` types.
- **Skipping verification.** `pattern_replace` mutates the filesystem before returning. Rollback is via git, not via the tool. Build and run tests on every run.
