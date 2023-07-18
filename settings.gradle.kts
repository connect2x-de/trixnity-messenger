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