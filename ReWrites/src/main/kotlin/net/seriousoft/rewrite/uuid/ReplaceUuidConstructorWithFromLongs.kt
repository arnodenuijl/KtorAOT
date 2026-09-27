package net.seriousoft.rewrite.uuid

import org.openrewrite.ExecutionContext
import org.openrewrite.Preconditions
import org.openrewrite.Recipe
import org.openrewrite.TreeVisitor
import org.openrewrite.java.JavaVisitor
import org.openrewrite.java.search.UsesType
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.TypeUtils
import org.openrewrite.kotlin.KotlinTemplate

/**
 * `java.util.UUID` has a public `UUID(long mostSigBits, long leastSigBits)` constructor, but
 * `kotlin.uuid.Uuid` has none. This recipe rewrites `UUID(a, b)` constructor calls to the
 * equivalent `Uuid.fromLongs(a, b)` factory call so that a later `ChangeType` to
 * `kotlin.uuid.Uuid` compiles.
 */
class ReplaceUuidConstructorWithFromLongs : Recipe() {

    override fun getDisplayName(): String =
        "Replace `java.util.UUID(long, long)` with `kotlin.uuid.Uuid.fromLongs(long, long)`"

    override fun getDescription(): String =
        "`kotlin.uuid.Uuid` has no public constructor, so `new UUID(mostSigBits, leastSigBits)` " +
            "is rewritten to `Uuid.fromLongs(mostSigBits, leastSigBits)` ahead of migrating the type."

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
        val condition = UsesType<ExecutionContext>("java.util.UUID", false)
        return Preconditions.check(condition, object : JavaVisitor<ExecutionContext>() {
            override fun visitNewClass(newClass: J.NewClass, ctx: ExecutionContext): J {
                val nc = super.visitNewClass(newClass, ctx) as J.NewClass
                val type = nc.clazz?.type
                if (type != null && TypeUtils.isOfClassType(type, "java.util.UUID") && nc.arguments.size == 2) {
                    return KotlinTemplate.builder("kotlin.uuid.Uuid.fromLongs(#{any(long)}, #{any(long)})")
                        .build()
                        .apply(
                            updateCursor(nc),
                            nc.coordinates.replace(),
                            nc.arguments[0],
                            nc.arguments[1]
                        )
                }
                return nc
            }
        })
    }
}
