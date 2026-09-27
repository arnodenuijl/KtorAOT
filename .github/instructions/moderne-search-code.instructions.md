---
name: search-code
description: "Search code: Comby structural patterns with balanced-delimiter matching (`catch (:[e]) { }`, `:[t] :[v] = :[e]; return :[v];`), symbol-aware filters (`sym:name`, `ref.type:com.example.Type`) that exclude javadoc and aliased imports, and language-scoped queries (`lang:java file:src/main`). Use INSTEAD OF grep when the question is structural or symbol-aware — grep can't reliably match balanced braces, won't skip javadoc, and false-matches across types of the same simple name. For plain text/regex searches on this repo, grep is fine; reach for this skill only for the structural and symbol cases."
---

# Search Code

This skill is for an agent working in a single repository's working tree via the moderne MCP server.

You have three text-search tools available, ordered by preference:

1. **`trigrep_search`** — pre-built trigram index. Returns in milliseconds regardless of repo size. **Default for any code search.** Supports plain text, regex (must be wrapped in `/.../`), and filters (`lang:`, `file:`, `case:`, `sym:`, `ref:`, etc.).
2. **`grep`** — linear scan via ripgrep / system grep. **Fallback only** — use when the trigram index is unavailable, when searching outside source files (build output, generated files, logs), or to corroborate a suspicious trigrep result.
3. **`trigrep_structural_search`** — Comby pattern matching with `:[hole]` syntax. Respects balanced delimiters (braces, parens, brackets, strings). A `:[hole]` spans lines while a delimiter encloses it — `{ :[body] }` covers a whole block — and is otherwise bounded to one line; write `:[hole\n]` to lift that bound. **Use only when text/regex would be too fragile** — e.g. finding redundant variables before return statements, broad exception handlers, empty catch blocks. For language-aware structural intent, prefer the LST tools in the `find-symbols` skill.

## When to use this skill

- "Find every TODO" / "where is `Foo` referenced as text" / "search for `/api/v1/.*` in source"
- You need a fast whole-repo coverage answer ("does this pattern exist?")
- You want a count or a file list before deciding on a transformation
- The task is structural-but-not-type-aware (empty catch blocks, redundant variable patterns)

## When NOT to use this skill

- "Find every CALLER of method X" → `find-symbols` skill (uses `find_methods`, type-aware)
- "Find every IMPLEMENTATION of interface Y" → `find-symbols` skill (`find_implementations`)
- "Find every reference to type Z" → `find-symbols` skill (`find_types`)
- "Find every usage of `@Foo`" → `find-symbols` skill (`find_annotations`, won't false-match javadoc)
- "Apply the same change across all matches" → `edit-code` skill (recipe path is atomic and won't drop the long tail)

## Query syntax for `trigrep_search`

- Plain text: `UserService`
- Regex: `/public\s+void\s+\w+\(/` — **MUST be wrapped in slashes**. A bare `(a|b)` is illegal because `|` is the OR operator outside `/.../`.
- Filters: `lang:java`, `file:src/main`, `case:yes`, `sym:methodName`. `type:` picks what a result is (`type:path`, `type:file`, `type:symbol`), not a class — for types use `ref:` / `extends:` / `implements:` below. Leave out `repo:` and `branch:`: the index covers this one repository's working tree, so they select nothing useful, and negating one (`-repo:x`) drops every result.
- Type-FQN filters (`extends:`, `implements:`, `annotated:`, `returns:`, `throws:`) take a type name: a fully-qualified name matches exactly (`implements:java.io.Serializable`), a bare simple name matches that type in any package (`implements:Serializable`), or an AspectJ glob where `*` is one segment and `..` is any number of packages (`extends:java.lang.*`). `annotated:` matches declarations (classes, methods, fields) carrying that annotation directly — no meta-annotation closure (`annotated:org.springframework.context.annotation.Configuration`, `annotated:AutoConfiguration`).
- Reference filters (`ref:`, `call:`) resolve through the type table, not text. Bare `ref:java.util.List` matches where `List` appears **as a written type** — a `List`-typed field/variable declaration, a generic, a cast, `new`, a Javadoc link (like OpenRewrite's `FindTypes`). It does **not** match an *expression* that merely evaluates to a `List`: a method call on a `List` (`l.clear()`) or a `List`-typed field access (`this.privateFields`). Those are opt-in kinds. Narrow to a reference **kind** with a dotted qualifier: `ref.type:` (written type-name positions), `ref.call:` (method receivers), `ref.new:` (`new T()`), `ref.field:` (field accesses by owner), `ref.fieldtype:` (a field access whose value is of that type), `ref.doc:` (Javadoc), or a set `ref.[type,call]:`. `ref.owner:T` finds the members *declared in* `T` (its surface); `ref.any:T` is the kind-blind union of everything — also what recipe `UsesType` narrowing uses, so no precondition ever misses a file. All take the same type-name forms (FQN / simple name / AspectJ glob) as the filters above.
- Mainframe: `lang:cobol` (programs and copybooks), `lang:jcl` (jobs and procs), `lang:bms`, `lang:controlm`, `lang:controlcard` (sort, IDCAMS and parm members), `lang:bind`, `lang:linkedit`, `lang:listload` (what AMBLIST and the binder printed), `lang:sql` (DB2 DDL), `lang:ims` (DBDs, PSBs, MFS and gen decks), `lang:assembler`, `lang:rexx`, `lang:clist`, `lang:sas`, `lang:doc` (run book members), `lang:pli` and `lang:c` narrow by source type whether or not the file parsed. JCL declares `job`, `proc`, `step` and `dd` symbols by name field, qualified by position (`sym:TCATBALF.STEP05.TCATBAL`, or `sym:TCATBAL`), and BMS declares `mapset`, `map` and `field` (`sym:COSGN00.COSGN0A.TRNNAME`). What JCL names is a reference: `ref.pgm:CBACT01C` (the program a step runs, `EXEC PGM=`), `ref.proc:REPROC` (the procedure it invokes), `ref.dsn:AWS.M2.CARDDEMO.ACCTDATA.PS` (the data set a DD names; for `DSN=LIB(MEMBER)` the library), `ref.member:REPROCT` (the member, qualified as `LIB.MEMBER`, so `ref:REPROCT` reaches `DSN=&CNTLLIB(REPROCT)`). All four are in bare `ref:`, so `sym:CBACT01C` and `ref:CBACT01C` walk from a program to the jobs that run it. COBOL declares `program` (`sym:CBACT01C`), `division` and `section` by their first word (`sym:CBACT01C.WORKING-STORAGE`), `paragraph` (`sym:1000-ACCTFILE-GET-NEXT`, or `sym:CBACT01C.0000-MAIN.0200-READ` for one inside a section), `file-description` (the `FD`, never the `SELECT` or `OPEN` naming the file), `data-item` and `condition-name` (level 88), each qualified by its program; a copybook's items are qualified by its member name (`sym:CVACT01Y.ACCT-ID`), and a copied item is declared in the copybook, not in the programs that `COPY` it. `sym:ACCT-ID` matches every name containing it (`CC-ACCT-ID` too); anchor it as `sym:^ACCT-ID$` for that name alone. What a COBOL program names is a reference, all in bare `ref:`: `ref.call:LGSTSQ` (the program a `CALL`, `EXEC CICS LINK` or `XCTL` transfers to, by literal or by the one literal its data name is known to hold, so `LINK PROGRAM(WS-PGM)` after `MOVE 'LGSTSQ' TO WS-PGM` counts), `ref.perform:1000-READ` (a `PERFORM` target, and the section qualifying it in `PERFORM 0200-READ OF 0000-MAIN`) and `ref.goto:` (a `GO TO` target), `ref.copy:LGCMAREA` (a `COPY` or `EXEC SQL INCLUDE`), `ref.data:WS-EOF` (a data name where it is used; `SET END-OF-FILE TO TRUE` is a use of `WS-EOF` under it, and a host variable or CICS operand counts when the program declares it), `ref.file:ACCTFILE-FILE` (the `SELECT`, `OPEN`, `READ`, `CLOSE`), `ref.dd:ACCTFILE` (the `ASSIGN TO` ddname, which `sym:ACCTFILE` finds in JCL — a dynamic `ASSIGN TO WS-DDNAME` is a `ref.data:` on that item instead, since the ddname it holds is nowhere in the source). A usage is qualified like its declaration, so `ref:CVACT01Y.ACCT-ID` is every program using the copied field and `ref:ACCT-ID` any field of that name.
- Scopes: `scope:comments`, `scope:code` and `scope:strings` restrict every content term (text, phrase, regex) of the query to comments only, to everything but comments (a string literal is code), or to string literals only — `scope:comments Amazon` finds `Amazon` in comment lines and `*>` comments, `scope:code CBACT01C` skips the banner and finds the `PROGRAM-ID`. The regions come from the index's `comment` and `string-literal` symbols, which only COBOL records today; in every other language `scope:comments` finds nothing and `scope:code` finds everything. A bare `sym:` leaves both kinds out (a region is not a name); `select:symbol.comment` and `select:symbol.string-literal` reach them.
- Combine: `/sendMessage\(/ lang:java file:src/main`
- Reserved keywords: `lang`, `file`, `repo`, `branch`, `case`, `sym`, `type`, `select`, `scope`, `count`, `visibility`, `static`, `final`, `abstract`, `extends`, `implements`, `annotated`, `returns`, `throws`, `ref`, `references`, `call`, `calls`, `struct`. To use one as a literal word, quote it (`"final"`) or wrap in regex (`/final/`).
- Boolean: `a OR b`, `a | b`, `-term`, `NOT term`, `(a OR b) c`.
- `sym:` lookups require `build_status.searchIndex.symbolCount > 0`. `@Foo` is a plain text match; for annotation usage use `annotated:` or `find_annotations`.

## Result shape

Responses lead with `matchCount` / `fileCount`, then `files`. In the default `content` mode each file is `{path, count, matches: [{line, content}], more?}`:

- `matches` is **capped per file** (a handful of lines) to keep the response small. `count` is the file's true match total and `more` is how many lines were not shown — trust `count`/`more`, not `matches.length`. A top-level `truncatedMatches` sums the lines hidden by the cap.
- `hasMore` plus `offset` page through additional **files**. When a result is capped, `nextStepGuidance` says how to narrow (a `file:`/`lang:` filter or a tighter query) — prefer narrowing over paging.
- Use `output_mode: "files_with_matches"` for paths only, or `"count"` for per-file counts, when you don't need the matched lines.

## Examples

Find all TODO markers in Java sources:

```
trigrep_search(query = "TODO lang:java file:src/main")
```

Find every method signature matching a pattern:

```
trigrep_search(query = "/public\\s+void\\s+handle\\w+\\(/")
```

Find empty catch blocks (structural):

```
trigrep_structural_search(query = "catch (:[type] :[e]) { }")
```

Find redundant-variable-before-return (structural):

```
trigrep_structural_search(query = ":[type] :[var] = :[expr]; return :[var]; lang:java")
```

## Anti-patterns

- **After running `trigrep_search`, your next move is EXACTLY ONE of three:**
  1. If the task is to apply a uniform change to the matches (replace, migrate, rename, swap, convert, remove), call `edit_code` (via the `edit-code` skill) to discover a recipe. Do **NOT** hand-edit file-by-file, and do **NOT** search again with a variant query first.
  2. If there are no relevant matches (all hits in comments, tests, fixtures, or non-source files) and no transformation is needed, write a final assistant message declaring the task complete and **stop**. Do not keep re-searching the same intent.
  3. If the intent is structural (callers / implementers / field types / type references), pivot to `find_methods` / `find_implementations` / `find_types` / `find_annotations` (via the `find-symbols` skill).
- **Repeated `trigrep_search` calls with near-identical queries are a known failure mode.** Pick one of the three actions above.
- **`grep` is the fallback, not the default.** Whole-repo `grep` scans take seconds where `trigrep_search` returns in milliseconds. Only fall back when the trigram index is unavailable, when scanning non-source content, or to corroborate.
- **Reading the error on parse failure.** Trigrep errors include the failing position, a hint identifying the likely cause (usually: unwrapped regex, or a bare reserved word like `final`), and a syntax primer. Fix and retry — don't silently abandon the search.
- **Symbol search needs symbol data.** Without `build_status.searchIndex.symbolCount > 0`, `sym:name` queries find nothing — an empty result there doesn't mean the symbol is absent.
- **Structural patterns with no anchor are slow.** `:[name]` with no concrete tokens around it can match too much; always include at least one literal keyword in the pattern.
