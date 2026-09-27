import org.gradle.kotlin.dsl.maven

// Credentials are read from Gradle properties (set in ~/.gradle/gradle.properties at the
// user level, or via ORG_GRADLE_PROJECT_* environment variables in CI). Never hardcode them here.
// Note: the pluginManagement {} block is evaluated in isolation, so it must resolve its own
// providers rather than referencing top-level vals from the rest of this script.
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://artifacts.codegenomeproject.org/maven")
            credentials {
                username = providers.gradleProperty("codegenomeprojectMavenUsername").getOrElse("")
                password = providers.gradleProperty("codegenomeprojectMavenToken").getOrElse("")
            }
        }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven {
            url = uri("https://artifacts.codegenomeproject.org/maven")
            credentials {
                username = providers.gradleProperty("codegenomeprojectMavenUsername").getOrElse("")
                password = providers.gradleProperty("codegenomeprojectMavenToken").getOrElse("")
            }
        }
        // Add custom internal/private repositories here once for all submodules
    }
}

rootProject.name = "KtorAOT"
include("SerializableProcessor")
include("WebApi")
include("Domain")
include("Persistence")
include("ReWrites")