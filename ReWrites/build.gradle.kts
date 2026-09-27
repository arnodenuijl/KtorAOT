plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

group = "net.seriousoft"
version = "1.0.0"

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

dependencies {
    implementation(libs.bundles.openrewrite.recipe.authoring)
    runtimeOnly("org.openrewrite:rewrite-java-17:${libs.versions.openrewrite.asProvider().get()}")

    testImplementation(libs.bundles.openrewrite.testing)
    testRuntimeOnly(libs.openrewrite.kotlin)
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${libs.versions.junit.get()}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

