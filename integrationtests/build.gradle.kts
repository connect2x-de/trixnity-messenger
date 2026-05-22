import de.connect2x.conventions.configureJava
import de.connect2x.conventions.defaultCompilerOptions
import de.connect2x.conventions.withJvm

plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform)
    alias(sharedLibs.plugins.kotlin.serialization)
}

configureJava(sharedLibs.versions.targetJvm)

kotlin {
    defaultCompilerOptions()
    withJvm { testRuns.named("test") { executionTask.configure { useJUnitPlatform() } } }
    sourceSets {
        jvmTest {
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
                implementation(libs.okio.fakefilesystem)
                implementation(sharedLibs.kotlinx.coroutines.debug)
                implementation(sharedLibs.lognity.test)
            }
        }
    }
}
