plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            // TODO: activate when detailed information in tests is required
            // testLogging.showStandardStreams = true
        }
    }

    sourceSets {
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(projects.trixnityMessenger)
                implementation(libs.trixnity.client)
                implementation(libs.trixnity.client.repository.exposed)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotest.assertion.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.uuid)
                implementation(libs.ktor.client.java)
                implementation(libs.bundles.testcontainers)
                implementation(libs.logback.classic)
                implementation(libs.okio.fakefilesystem)
            }
        }
    }
}
