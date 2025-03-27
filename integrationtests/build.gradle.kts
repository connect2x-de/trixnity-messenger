plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization").version(libs.versions.kotlin.get())
}

kotlin {
    val kotlinJvm = libs.versions.kotlinJvmTarget.get()
    jvmToolchain(kotlinJvm.toInt())
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = kotlinJvm
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            // testLogging.showStandardStreams = true   // activate when detailed information in tests is required
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
        val commonMain by getting {}
        val jvmTest by getting {
            dependencies {
                implementation(projects.trixnityMessenger)
                implementation(libs.trixnity.client)
                implementation(libs.trixnity.client.repository.exposed)
                implementation(libs.kotlinx.coroutines.test)
                implementation(kotlin("test"))
                implementation(libs.kotest.assertion.core)
                implementation(libs.kotlinx.datetime)
//                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.java)
                implementation(libs.bundles.testcontainers)
                implementation(libs.logback.classic)
                implementation(libs.okio.fakefilesystem)
            }
        }
    }
}
