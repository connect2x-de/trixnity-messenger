buildscript {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }

    dependencies {
//        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin-api:${Versions.kotlin}") // FIXME
        classpath("com.android.tools.build:gradle:${Versions.androidGradle}")
        classpath(kotlin("gradle-plugin", version = Versions.kotlin))
    }
}

plugins {
    id("io.kotest.multiplatform") version Versions.kotest apply false
    kotlin("plugin.serialization") version Versions.kotlin apply false
    id("com.google.devtools.ksp") version Versions.ksp apply false
    id("co.touchlab.skie") version Versions.skie apply false
    id("co.touchlab.kmmbridge") version Versions.kmmBridge apply false
    id("org.jetbrains.dokka") version Versions.dokka apply false
}

allprojects {
    group = "de.connect2x"
    version = withVersionSuffix(Versions.trixnityMessenger)

    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://gitlab.com/api/v4/projects/26519650/packages/maven")
    }
}
