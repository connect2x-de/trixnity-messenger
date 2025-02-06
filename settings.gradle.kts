rootProject.name = "trixnity-messenger-root"
include(
    "trixnity-messenger",
    "integrationtests",
    "emojis",
    "trixnity-messenger-compose-view",
    "trixnity-messenger-compose-app",
)

buildCache {
    local {
        directory = File(rootDir, ".gradle").resolve("build-cache")
        removeUnusedEntriesAfterDays = 30 // TODO: deprecated
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0") // https://github.com/gradle/foojay-toolchains/tags
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
