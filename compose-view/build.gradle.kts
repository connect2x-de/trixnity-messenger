import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
}

kotlin {
    val kotlinJvm = libs.versions.jvmTarget.get()
    jvmToolchain(kotlinJvm.toInt())
    androidTarget {
        publishLibraryVariants("release")

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant {
            sourceSetTree.set(KotlinSourceSetTree.test)
            dependencies {
                // TODO Remove the dependency on ui-test-junit4-android when 1.7.0 is released,
                //  as the needed classes in will have moved to ui-test
                implementation(libs.ui.test.android)
                debugImplementation(libs.ui.test.android.manifest)
            }
        }
        compilations.configureEach {
            kotlinOptions.jvmTarget = kotlinJvm
        }
    }
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = kotlinJvm
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            // testLogging.showStandardStreams = true   // activate when detailed information in tests is required
        }
        tasks.withType<Test>().configureEach {
            maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
        }
    }
    js("web", IR) {
        browser()
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
        commonMain {
            dependencies {
                api(project(":trixnity-messenger"))
                api(compose.runtime)
                api(compose.foundation)
                api(compose.components.resources)
                api(compose.material3)
                api(compose.ui)
                api(compose.materialIconsExtended)
                api(libs.decompose)
                api(libs.decompose.extensions)
                api(libs.aboutlibraries)
                implementation(libs.okio)
                implementation(libs.kotlinx.datetime)
                implementation(compose.uiUtil)
                implementation(libs.compose.richeditor)
                implementation(libs.mpfilepicker)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)
                implementation(compose.uiTooling)
                implementation(libs.qrcode)
                implementation(libs.filekit.compose)
                implementation(libs.ktor.client.okhttp)
            }
        }
        androidMain {
            dependencies {
                implementation(compose.uiTooling)
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.security.crypto)
                implementation(libs.bundles.androix.camera)
                implementation(libs.zxing.android)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.bundles.android.common)
                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.firebase.messaging.ktx)
                // for Previews:
                implementation(libs.slf4j.api)
            }
        }
        val webMain by getting {
            dependencies {
                implementation(npm("copy-webpack-plugin", libs.versions.copyWebpackPlugin.get()))
                implementation(libs.kotlin.browser)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))

                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
                implementation(libs.okio.fakefilesystem)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

android {
    namespace = "de.connect2x.messenger.compose.view"
    compileSdk = libs.versions.androidCompileSDK.get().toInt()

    buildFeatures {
        compose = true
    }

    defaultConfig {
        minSdk = libs.versions.androidMinimalSDK.get().toInt()
        targetSdk = libs.versions.androidTargetSDK.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
    }

    sourceSets {
        named("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
        }
    }
}
