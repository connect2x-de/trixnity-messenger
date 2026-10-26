@file:Suppress("UnstableApiUsage")

pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
    maven("https://gitlab.com/api/v4/projects/68438621/packages/maven") // c2x Conventions
  }
}

plugins {
  id("de.connect2x.conventions.c2x-settings-plugin") version "20260917.190917"
}

dependencyResolutionManagement {
  repositories {
    exclusiveContent {
      forRepository { maven("https://jitpack.io") }
      filter { includeModule("com.github.davidliu", "audioswitch") }
    }
  }
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
