import de.connect2x.conventions.configureJava

plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform)
    alias(sharedLibs.plugins.kotlin.serialization)
}

configureJava(sharedLibs.versions.targetJvm)

kotlin {
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        val jvmTest by getting {
            dependencies {
                implementation(sharedLibs.kotlin.test)
                implementation(projects.trixnityMessenger)
                implementation(libs.trixnity.client)
                implementation(libs.trixnity.client.repository.exposed)
                implementation(sharedLibs.kotlinx.coroutines.test)
                implementation(sharedLibs.kotest.assertions.core)
                implementation(sharedLibs.kotlinx.datetime)
                implementation(sharedLibs.ktor.client.java)
                implementation(libs.bundles.testcontainers)
                implementation(libs.logback.classic)
                implementation(libs.okio.fakefilesystem)
                implementation(sharedLibs.kotlinx.coroutines.debug)
            }
        }
    }
}
