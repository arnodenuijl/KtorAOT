plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "net.seriousoft"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ksp.symbol.processing.api)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}