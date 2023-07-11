buildscript {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:${Versions.androidGradle}")
        classpath(kotlin("gradle-plugin", version = Versions.kotlin))
    }
}

plugins {
    id("org.kodein.mock.mockmp") version Versions.mocKMP apply false
    id("io.kotest.multiplatform") version Versions.kotest apply false
}

allprojects {
    group = "de.connect2x"
    version = withVersionSuffix(Versions.trixnityMessenger)

    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}
