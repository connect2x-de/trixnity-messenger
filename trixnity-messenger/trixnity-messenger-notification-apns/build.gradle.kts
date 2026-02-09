import de.connect2x.conventions.configureJava
import de.connect2x.conventions.defaultCompilerOptions
import de.connect2x.conventions.withIos

plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform)
    alias(sharedLibs.plugins.kotlin.serialization)
    alias(sharedLibs.plugins.dokka)
    alias(sharedLibs.plugins.mavenPublish)
}

configureJava(sharedLibs.versions.targetJvm)

kotlin {
    withSourcesJar()
    defaultCompilerOptions()
    withIos()
    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.trixnityMessenger)
            }
        }
        commonTest {
            dependencies {
                implementation(sharedLibs.kotlin.test)
                implementation(sharedLibs.kotlinx.coroutines.test)
                implementation(sharedLibs.kotest.assertions.core)
            }
        }
    }
}
