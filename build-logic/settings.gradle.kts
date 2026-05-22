rootProject.name = "build-logic"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven("https://gitlab.com/api/v4/projects/68438621/packages/maven") // c2x Conventions
    }
}

dependencyResolutionManagement { repositories { mavenLocal() } }

plugins { id("de.connect2x.conventions.c2x-settings-plugin") version "20260520.091403" }
