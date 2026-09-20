plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "net.seriousoft"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":Domain"))
    implementation(libs.exposed.core)
    implementation(libs.exposed.json)
    implementation(libs.exposed.jdbc)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}