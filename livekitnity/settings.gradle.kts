@file:Suppress("UnstableApiUsage")

pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
    maven("https://gitlab.com/api/v4/projects/68438621/packages/maven") // c2x Conventions
  }
}

plugins {
  id("de.connect2x.conventions.c2x-settings-plugin") version "20261116.171406"
}

rootProject.name = "LiveKitnity"

include(":demoApps:android")

include(":demoApps:web")

include(":demoApps:shared")

include(":livekitnity-api")

include(":livekitnity-bindings-js")

include(":livekitnity-core")

include(
    ":livekitnity-compose",
)
