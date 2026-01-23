import de.connect2x.conventions.configureJava

plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform)
    alias(sharedLibs.plugins.kotlin.serialization)
    alias(sharedLibs.plugins.android.library)
    alias(sharedLibs.plugins.dokka)
    `maven-publish`
}

configureJava(sharedLibs.versions.targetJvm)

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    androidTarget {
        publishLibraryVariants("release")
    }
    applyDefaultHierarchyTemplate()
    sourceSets {
        all {
            languageSettings.optIn("kotlin.uuid.ExperimentalUuidApi")
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }
        val commonMain by getting {
            dependencies {
                implementation(projects.trixnityMessenger)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(sharedLibs.kotlin.test)
                implementation(sharedLibs.kotlinx.coroutines.test)
                implementation(sharedLibs.kotest.assertions.core)
            }
        }
        androidMain {
            dependencies {
                implementation(sharedLibs.androidx.work.runtime.ktx)
                implementation(libs.unifiedpush.android.connector.get().toString()) {
                    exclude(group = "com.google.crypto.tink", module = "tink")
                }
            }
        }
    }
}

android {
    namespace = "$group.trixnity.messenger.notification.unifiedpush"
    compileSdk = sharedLibs.versions.androidCompileSDK.get().toInt()
    defaultConfig {
        minSdk = sharedLibs.versions.androidMinimalSDK.get().toInt()
    }
    sourceSets {
        named("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
        }
    }
    buildTypes {
        debug {
            isDefault = true
        }
        release {
            isMinifyEnabled = false
        }
    }
}
