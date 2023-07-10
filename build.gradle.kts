buildscript {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:${Versions.androidGradle}")

        classpath(kotlin("gradle-plugin", version = Versions.kotlin))

        classpath("com.jaredsburrows:gradle-license-plugin:0.8.91")
    }
}

plugins {
    id("org.kodein.mock.mockmp") version Versions.mocKMP apply false
    id("io.kotest.multiplatform") version Versions.kotest apply false
}

allprojects {
    group = "de.connect2x"
    version = Versions.trixnityMessenger

    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}
