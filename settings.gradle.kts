plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "KtorAOT"
include("SerializableProcessor")
include("WebApi")
include("Domain")
include("Persistence")