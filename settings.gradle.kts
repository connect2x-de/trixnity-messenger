rootProject.name = "trixnity-messenger-root"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven("https://gitlab.com/api/v4/projects/68438621/packages/maven") // c2x Conventions
        maven("https://gitlab.com/api/v4/projects/75787729/packages/maven") // Compose Multiplatform A11y
    }
    includeBuild("build-logic")
}

// Suppress is okay because it is an incubating API, the suppression name just doesn't match
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        maven("https://gitlab.com/api/v4/projects/68438621/packages/maven") // c2x Conventions
        maven("https://gitlab.com/api/v4/projects/75787860/packages/maven") // Compose Multiplatform Core A11y
        maven("https://gitlab.com/api/v4/projects/75787729/packages/maven") // Compose Multiplatform A11y
        maven("https://gitlab.com/api/v4/projects/72301746/packages/maven") // Lognity
        maven("https://gitlab.com/api/v4/projects/58749664/packages/maven") // Sysnotify
        maven("https://gitlab.com/api/v4/projects/26519650/packages/maven") // Trixnity
        maven("https://gitlab.com/api/v4/projects/72850047/packages/maven") // SQLitenity
    }
}

plugins {
    id("de.connect2x.conventions.c2x-settings-plugin") version "20260325.112432"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

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
