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
        compilations.configureEach {
            kotlinOptions.jvmTarget = kotlinJvm
        }

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
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
        browser {
            // Run test in firefox for ci as trixnity/kmp-dockerfiles/base has only firefox
            testRuns.create("firefox").executionTask.configure {
                useKarma {
                    useFirefoxHeadless()
                }
            }
        }
        binaries.library()
    }
    listOf(
//        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    )

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
                implementation(libs.sysnotify)
                implementation(libs.androidx.autofill)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)
                implementation(compose.uiTooling)
                implementation(libs.qrcode)
                implementation(libs.filekit.compose)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.pdfbox)
            }
        }
        androidMain {
            dependencies {
                implementation(compose.uiTooling)
                implementation(libs.bundles.android.common)
                implementation(compose.preview)
                implementation(libs.androidx.security.crypto)
                implementation(libs.ktor.client.okhttp)
                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.firebase.messaging.ktx)
                implementation(libs.sysnotify.android)
                // for Previews:
                implementation(libs.slf4j.api)
            }
        }
        val webMain by getting {
            dependencies {
                implementation(npm("copy-webpack-plugin", libs.versions.copyWebpackPlugin.get()))
                implementation(project.dependencies.platform(libs.kotlin.wrappers.bom))
                implementation(libs.kotlin.browser)
                implementation(libs.sysnotify.js)
                implementation(libs.filekit.core)
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
                implementation(libs.kotest.junit.runner)
            }
        }
    }
}

dependencies {
    androidTestImplementation(libs.ui.test.junit4.android)
    debugImplementation(libs.ui.test.android.manifest)
    implementation(variantOf(libs.sysnotify) { classifier("jvm-natives-windows-x64") })
    implementation(variantOf(libs.sysnotify) { classifier("jvm-natives-linux-x64") })
    implementation(variantOf(libs.sysnotify) { classifier("jvm-natives-macos-x64") })
}

android {
    namespace = "de.connect2x.messenger.compose.view"
    compileSdk = libs.versions.androidCompileSDK.get().toInt()

    buildFeatures {
        compose = true
    }

    defaultConfig {
        minSdk = libs.versions.androidMinimalSDK.get().toInt()
        testOptions.targetSdk = libs.versions.androidTargetSDK.get().toInt()
        lint.targetSdk = libs.versions.androidTargetSDK.get().toInt()
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
