rootProject.name = "trixnity-messenger-root"

pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven("https://gitlab.com/api/v4/projects/68438621/packages/maven") // c2x Conventions
    }
}

plugins { id("de.connect2x.conventions.c2x-settings-plugin") version "20261015.130157" }

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

includeBuild("livekitnity")

include(
    "trixnity-messenger",
    "trixnity-messenger:trixnity-messenger-notification-apns",
    "trixnity-messenger:trixnity-messenger-notification-fcm",
    "trixnity-messenger:trixnity-messenger-notification-unifiedpush",
    "trixnity-messenger-compose-view",
    "trixnity-messenger-compose-view:trixnity-messenger-compose-view-typography-nunito",
    "trixnity-messenger-compose-app",
    "integrationtests",
    "emojis",
    "wrappers-zipjs",
    "wrappers-pdfjs",
)

dependencyResolutionManagement {
    versionCatalogs {
        create("livekitnityLibs") { from(files("livekitnity/gradle/libs.versions.toml")) }
    }

    @Suppress("UnstableApiUsage")
    repositories {
        exclusiveContent {
            forRepository { maven("https://jitpack.io") }
            filter { includeModule("com.github.davidliu", "audioswitch") }
        }
    }
}
