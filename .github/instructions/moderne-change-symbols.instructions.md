---
name: change-symbols
description: "Change symbols: rename a method (callers + overrides) or move/rename a type (imports + refs + generics) atomically across the repo. Use instead of grep+Edit for fully-qualified renames."
---

# Change Symbols

This skill is for an agent working in a single repository's working tree via the moderne MCP server.

Two atomic refactoring tools for the most common JVM rename operations:

| tool | renames |
|---|---|
| `change_type` | a fully-qualified type → another fully-qualified type. Updates every import, reference, field type, parameter, return type, cast, `instanceof`, annotation, and generic argument. |
| `change_method_name` | a fully-qualified method (AspectJ pattern) → a new method name. Updates every caller, declaration, and override consistently. |

Both apply atomically across the LST — they will not miss a site the way file-by-file editing or `grep`-then-`Edit` routinely does.

## When to use this skill

- The user asks to rename a type / class / interface / enum and have all references updated automatically.
- The user asks to rename a method and have all callers updated automatically.
- You are deprecating an old type or method in favor of a new one and need every caller migrated.
- A library upgrade renamed a type / method and you need to mass-rewrite call sites.

## When NOT to use this skill

- For broad migrations spanning multiple type renames + dependency changes + import rewrites, look for a curated marketplace recipe via `edit_code` (the `edit-code` skill) first — it may bundle the whole migration.
- For renaming a field, local variable, or parameter, `change_method_name` / `change_type` don't apply. Use `pattern_replace` or `Edit`.
- For finding references first (without renaming), use `find_types` / `find_methods` (via the `find-symbols` skill).

## Examples

Move and rename a type:

```
change_type(
  oldFullyQualifiedTypeName = "com.example.OldService",
  newFullyQualifiedTypeName = "com.example.new_pkg.NewService"
)
```

Rename a method on a specific class:

```
change_method_name(
  methodPattern = "com.example.UserService getUser(java.lang.String)",
  newMethodName = "loadUser"
)
```

After running, `query_datatable` (via the `query-datatable` skill) for the per-sub-recipe attribution:

```sql
SELECT DISTINCT recipe FROM SourcesFileResults;
```

## Anti-patterns

- **Abbreviating names.** Pass fully-qualified type / method patterns. `change_type(oldFullyQualifiedTypeName = "OldService", ...)` is wrong; `"com.example.OldService"` is right.
- **`changeCount: 0` retries.** If `change_type` reports `changeCount: 0`, the old type was not found — don't retry. Verify the FQN via `find_types` (via the `find-symbols` skill) before re-attempting.
- **Skipping verification.** These mutate the filesystem before returning. Run the build and tests; rollback is via git.
- **Method-rename collisions.** Verify the new name doesn't collide with an existing method on the same class. Collisions silently produce overload conflicts that break compilation.
- **Reflection and string-name dispatch are invisible.** `Class.forName("com.example.OldService")`, `method.invoke("getUser", …)`, DI configs, Spring SpEL, annotation values, generated bindings, IDE run configurations, and external resource files (XML, properties, YAML) that mention the type/method are **not** updated. Follow up with `search-code` (the `search-code` skill) using `grep` or `trigrep_search` to mop those up.
- **Non-source files.** Javadoc cross-references in unrelated files, `@see` tags pointing at the old name, README mentions — these are not updated.
- **External JAR consumers.** Renaming an interface method renames implementations only when those implementations are visible in the LST. External downstream consumers are not touched.
- **New package missing.** If the new type's package does not exist yet, the imports are added but you may need to verify the destination package matches your project's source-set layout.
