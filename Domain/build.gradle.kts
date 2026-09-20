plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.serialization)
    alias(libs.plugins.graalvm.native)


}

group = "net.seriousoft"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    ksp(project(":SerializableProcessor"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.reflect)
    implementation(libs.graalvm.sdk.nativeimage)

    testImplementation(kotlin("test"))
}

ksp {
    arg("serializable.processor.namespace", "domain.generated.graalvm")
}

tasks.test {
    useJUnitPlatform()
}