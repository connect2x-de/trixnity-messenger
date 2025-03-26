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
    }
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Suppress is okay because it is an incubating API, the suppression name just doesn't match
    @Suppress("UnstableApiUsage") repositories {
        mavenCentral()
        google()
        mavenLocal()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://gitlab.com/api/v4/projects/26519650/packages/maven") // trixnity
        maven("https://gitlab.com/api/v4/projects/58749664/packages/maven") // sysnotify
        maven("https://gitlab.com/api/v4/projects/65998892/packages/maven") // androidx
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0") // https://github.com/gradle/foojay-toolchains/tags
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
