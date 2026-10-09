plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
    application
}

group = "net.seriousoft"
version = "1.0-SNAPSHOT"


dependencies {
    implementation(project(":Persistence"))
    implementation(project(":Domain"))
    implementation(libs.exposed.core)
    implementation(libs.exposed.json)
    implementation(libs.exposed.jdbc)
    implementation(libs.oracle.jdbc)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.forwarded.header)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.routing.openapi)
    implementation(libs.ktor.server.routing.swagger)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.logback.classic)
    testImplementation(kotlin("test"))
}

ktor {
    fatJar {
        archiveFileName.set("ktor-service.jar")
    }
    openApi {
        enabled = true
        codeInferenceEnabled = true
        onlyCommented = false
    }
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}
tasks.shadowJar {
    mergeServiceFiles()
}

application {
    mainClass.set("ApplicationKt")
}
