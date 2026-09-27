---
name: find-symbols
description: "Find symbols (type-aware): callers of a method, references to a type, usages of an annotation, implementers of an interface, every symbol in a file. Resolves through the LST type system."
---

# Find Symbols

This skill is for an agent working in a single repository's working tree via the moderne MCP server.

Five LST-typed primitives let you ask precise structural questions about the code. Each understands the type system; when it finds anything, it returns a `runId` whose data tables you can query via the `query-datatable` skill.

| tool | answers |
|---|---|
| `find_types` | "where is type `X` referenced?" (imports, fields, parameters, returns, casts, `instanceof`, annotations, generics) |
| `find_methods` | "where is method `X.bar(...)` called from?" — matched via AspectJ-style patterns |
| `find_annotations` | "where do we use `@X`?" — won't false-match javadoc or aliased imports |
| `find_implementations` | "what implements / extends `X`?" — walks the type hierarchy, finds indirect impls too |
| `symbols_overview` | enumerate every class / interface / enum / method / field in a file or repo, with kind / signature / visibility |

## When to use this skill

- "Find every reference to `com.example.Foo`" → `find_types`
- "Find every caller of `org.springframework.web.client.RestTemplate getForObject(..)`" → `find_methods`
- "List every `@RestController` site" → `find_annotations`
- "What classes implement `org.openrewrite.Recipe`?" → `find_implementations`
- "Give me all public methods in `*Controller.java` files" → `symbols_overview` + `query_datatable`
- Evaluating blast radius before a type rename, method rename, or interface change

## When NOT to use this skill

- Pure text searches (TODO comments, find `/api/v1/` in source, find a literal string) → `search-code` skill (`trigrep_search`).
- "Where is type `Foo` defined?" — those are single-target file reads, not type lookups.
- Applying a uniform transformation across matches → `edit-code` (recipe path is atomic) or `change-symbols` (for type / method-name renames) skill.

## Examples

Find every reference to a Spring type (any subpackage):

```
find_types(fullyQualifiedTypeName = "org.springframework..*")
```

Find every call site of `RestTemplate.getForObject(...)`:

```
find_methods(methodPattern = "org.springframework.web.client.RestTemplate getForObject(..)")
```

Find every `@Deprecated`:

```
find_annotations(annotationPattern = "@java.lang.Deprecated")
```

Find all classes that implement `org.openrewrite.Recipe` (incl. indirect):

```
find_implementations(typeName = "org.openrewrite.Recipe")
```

After `find_methods`, count call sites per calling file via `query_datatable`:

```sql
SELECT sourceFile, COUNT(*) AS calls
FROM MethodCalls
GROUP BY sourceFile
ORDER BY calls DESC;
```

## Anti-patterns

- **`trigrep_search` for `@Foo` is fragile** — it false-matches javadoc references, doesn't follow aliased imports (`import com.x.MyFoo as Foo`), and treats `@Deprecated` on a class identically to `@Deprecated` on a method. Use `find_annotations` instead.
- **`trigrep_search` for method names is too ambiguous** — `get()` matches `Optional.get`, `Map.get`, your-own-class.get, .... Use `find_methods` with the fully-qualified AspectJ pattern.
- **`trigrep_search` for type names false-matches** — `Logger` matches `java.util.logging.Logger`, `org.slf4j.Logger`, javadoc references, …. Use `find_types`.
- **`trigrep_search` for `implements Foo` / `extends Foo` misses indirect impls**, generic-bound implementations, and anonymous-class implementations. Use `find_implementations`.
- **Fully-qualified names are required.** Don't abbreviate. `find_methods(methodPattern = "getUser(..)")` is wrong; `find_methods(methodPattern = "com.example.UserService getUser(java.lang.String)")` is right.
- **LST must be ready.** Call `lst_status` (via the `inspect-status` skill) first if results look incomplete. If `state != READY`, fall back to `trigrep_search` / `grep` for the immediate need; the LST builds in the background.
- **Don't post-filter manually.** After any of these tools returns, use `query_datatable` (pass the returned `runId` as `run`) to filter / aggregate / group. Don't re-scan source files to count.
- **Anonymous and lambda-form implementations** are reported by their containing class, not as standalone implementers. Adjust expectations for `find_implementations` results.
- **Reflection and string-name dispatch are invisible.** `Class.forName("...")`, `method.invoke(...)`, DI configs, Spring SpEL, and annotation-value strings are not detected. If those matter, follow up with `search-code` (`grep` or `trigrep_search`).
