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
    id("io.kotest.multiplatform") version Versions.kotest apply false
    id("com.google.devtools.ksp") version Versions.ksp apply false
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
