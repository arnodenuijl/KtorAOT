package net.seriousoft.rewrite.uuid

import org.junit.jupiter.api.Test
import org.openrewrite.kotlin.Assertions.kotlin
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

class MigrateJavaUtilUuidToKotlinUuidTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(MigrateJavaUtilUuidToKotlinUuid())
    }

    @Test
    fun `changes the type of fields, parameters and return types`() = rewriteRun(
        kotlin(
            """
            import java.util.UUID

            class Repo {
                fun find(id: UUID): UUID? = id
            }
            """,
            """
            import kotlin.uuid.Uuid

            class Repo {
                fun find(id: Uuid): Uuid? = id
            }
            """
        )
    )

    @Test
    fun `rewrites randomUUID to random`() = rewriteRun(
        kotlin(
            """
            import java.util.UUID

            fun newId(): UUID = UUID.randomUUID()
            """,
            """
            import kotlin.uuid.Uuid

            fun newId(): Uuid = Uuid.random()
            """
        )
    )

    @Test
    fun `rewrites fromString to parse`() = rewriteRun(
        kotlin(
            """
            import java.util.UUID

            fun parseId(s: String): UUID = UUID.fromString(s)
            """,
            """
            import kotlin.uuid.Uuid

            fun parseId(s: String): Uuid = Uuid.parse(s)
            """
        )
    )

    @Test
    fun `rewrites the long,long constructor to fromLongs`() = rewriteRun(
        kotlin(
            """
            import java.util.UUID

            val id: UUID = UUID(0, 1)
            """,
            """
            import kotlin.uuid.Uuid

            val id: Uuid = Uuid.fromLongs(0, 1)
            """
        )
    )
}
