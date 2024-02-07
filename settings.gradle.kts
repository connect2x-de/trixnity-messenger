rootProject.name = "trixnity-messenger-root"
include(
    "trixnity-messenger",
    "integrationtests",
    "emojis",
)

buildCache {
    local {
        directory = File(rootDir, ".gradle").resolve("build-cache")
        removeUnusedEntriesAfterDays = 30
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0") // https://github.com/gradle/foojay-toolchains/tags
}