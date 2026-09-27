plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.graalvm.native) apply false
    alias(libs.plugins.open.rewrite)
}


dependencies {
    rewrite(libs.openrewrite.recipes.kotlin)
    rewrite(project(":ReWrites"))
}


rewrite {
    activeRecipe("org.openrewrite.kotlin.OrderImports")
    activeRecipe("org.openrewrite.kotlin.search.FindUnsafeCast\$KtRecipes")
    activeRecipe("net.seriousoft.rewrite.uuid.MigrateJavaUtilUuidToKotlinUuid")
    isExportDatatables = true
}
