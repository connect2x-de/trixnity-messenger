import de.connect2x.conventions.configureJava
import de.connect2x.conventions.defaultCompilerOptions
import de.connect2x.conventions.withAndroidLibrary

plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform)
    alias(sharedLibs.plugins.kotlin.serialization)
    alias(sharedLibs.plugins.android.library)
}

configureJava(sharedLibs.versions.targetJvm)

kotlin {
    withSourcesJar()
    defaultCompilerOptions()
    withAndroidLibrary("$group.notification.unifiedpush")
    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain { dependencies { implementation(projects.trixnityMessenger) } }
        commonTest {
            dependencies {
                implementation(sharedLibs.kotlin.test)
                implementation(sharedLibs.kotlinx.coroutines.test)
                implementation(sharedLibs.kotest.assertions.core)
            }
        }
        androidMain {
            dependencies {
                implementation(sharedLibs.androidx.workRuntime)
                implementation(libs.unifiedpush.android.connector.get().toString()) {
                    exclude(group = "com.google.crypto.tink", module = "tink")
                }
            }
        }
    }
}

android {
    sourceSets { named("main") { manifest.srcFile("src/androidMain/AndroidManifest.xml") } }
    buildTypes {
        debug { isDefault = true }
        release { isMinifyEnabled = false }
    }
}
