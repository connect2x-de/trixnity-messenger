buildscript {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:${libs.versions.androidGradle.get()}")
        classpath(kotlin("gradle-plugin", version = libs.versions.kotlin.get()))
    }
}

plugins {
    alias(libs.plugins.kotest).apply(false)
    kotlin("plugin.serialization") version libs.versions.kotlin.get() apply false
    alias(libs.plugins.mokkery).apply(false)
    alias(libs.plugins.skie).apply(false)
    alias(libs.plugins.kmmbridge).apply(false)
    alias(libs.plugins.dokka).apply(false)
}

allprojects {
    group = "de.connect2x"
    version = withVersionSuffix("2.1.1")

    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://gitlab.com/api/v4/projects/26519650/packages/maven")
    }

    if (System.getenv("WITH_LOCK")?.toBoolean() == true) {
        dependencyLocking {
            lockAllConfigurations()
        }

        val dependenciesForAll by tasks.registering(DependencyReportTask::class) {}
    }
}
