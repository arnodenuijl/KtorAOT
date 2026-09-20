plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
    alias(libs.plugins.graalvm.native)
    alias(libs.plugins.ksp)
    application
}

group = "net.seriousoft"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
}

dependencies {
    ksp(project(":SerializableProcessor"))

    implementation(project(":Persistence"))
    implementation(project(":Domain"))
    implementation(libs.exposed.core)
    implementation(libs.exposed.json)
    implementation(libs.exposed.jdbc)
    implementation(libs.oracle.jdbc)
    implementation(libs.kotlin.reflect)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.routing.openapi)
    implementation(libs.ktor.server.routing.swagger)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.logback.classic)
    implementation(libs.graalvm.sdk.nativeimage)
    testImplementation(kotlin("test"))
}

ksp {
    arg("serializable.processor.namespace", "webapi.generated.graalvm")
}

ktor {
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
application {
    mainClass.set("ApplicationKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("ktor-service")
            mainClass.set("ApplicationKt")

            // Pass flags to allow netty/cio socket allocations and include resources
            buildArgs.add("-H:+IncludeAllLocales")
            buildArgs.add("-H:IncludeResources=.*\\.conf|.*\\.yaml|.*\\.xml")
            buildArgs.add("--features=webapi.generated.graalvm.ReflectionFeature")
            buildArgs.add("--features=domain.generated.graalvm.ReflectionFeature")
            buildArgs.add("--enable-http")
        }
    }
}
