package net.seriousoft.rewrite.uuid

import org.openrewrite.Recipe
import org.openrewrite.java.ChangeMethodName
import org.openrewrite.java.ChangeType

/**
 * Migrates usages of the platform `java.util.UUID` type to Kotlin's `kotlin.uuid.Uuid`
 * (stable since Kotlin 2.1, `@ExperimentalUuidApi` before that).
 *
 * Runs as an ordered pipeline of sub-recipes so that method/constructor call shapes are
 * adapted to the Kotlin API *before* the type itself is renamed:
 *
 * 1. [ReplaceUuidConstructorWithFromLongs] - `UUID(msb, lsb)` -> `Uuid.fromLongs(msb, lsb)`
 * 2. `UUID.randomUUID()` -> `Uuid.random()`
 * 3. `UUID.fromString(s)` -> `Uuid.parse(s)`
 * 4. [ChangeType] - `java.util.UUID` -> `kotlin.uuid.Uuid` everywhere else (fields, parameters,
 *    return types, generics, imports).
 *
 * This recipe is generic: it targets the `java.util.UUID` / `kotlin.uuid.Uuid` APIs directly
 * and does not depend on anything specific to this project, so it can be run against any
 * Kotlin (or mixed Kotlin/Java) codebase.
 */
class MigrateJavaUtilUuidToKotlinUuid : Recipe() {

    override fun getDisplayName(): String = "Migrate `java.util.UUID` to `kotlin.uuid.Uuid`"

    override fun getDescription(): String =
        "Replaces usages of the JDK `java.util.UUID` type with Kotlin's `kotlin.uuid.Uuid`, " +
            "rewriting `randomUUID()`, `fromString(String)` and the `(long, long)` constructor " +
            "to their `Uuid` equivalents (`random()`, `parse(String)`, `fromLongs(long, long)`)."

    override fun getRecipeList(): List<Recipe> = listOf(
        ReplaceUuidConstructorWithFromLongs(),
        ChangeMethodName("java.util.UUID randomUUID()", "random", null, null),
        ChangeMethodName("java.util.UUID fromString(String)", "parse", null, null),
        ChangeType("java.util.UUID", "kotlin.uuid.Uuid", null)
    )
}
