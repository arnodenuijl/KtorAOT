plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.graalvm.native) apply false
}