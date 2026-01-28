rootProject.name = "trixnity-messenger-root"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven("https://gitlab.com/api/v4/projects/68438621/packages/maven") // c2x Conventions
        maven("https://gitlab.com/api/v4/projects/75787729/packages/maven") // Compose Multiplatform A11y
    }
}

// Suppress is okay because it is an incubating API, the suppression name just doesn't match
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        maven("https://gitlab.com/api/v4/projects/68438621/packages/maven") // c2x Conventions
        maven("https://gitlab.com/api/v4/projects/75787860/packages/maven") // Compose Multiplatform Core A11y
        maven("https://gitlab.com/api/v4/projects/75787729/packages/maven") // Compose Multiplatform A11y
    }
}

plugins {
    id("de.connect2x.conventions.c2x-settings-plugin") version "0.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    "trixnity-messenger",
    "trixnity-messenger:trixnity-messenger-notification-fcm",
    "trixnity-messenger:trixnity-messenger-notification-apns",
    "integrationtests",
    "emojis",
    "wrappers-zipjs",
    "wrappers-pdfjs",
    "trixnity-messenger-compose-view",
    "trixnity-messenger-compose-app",
)
