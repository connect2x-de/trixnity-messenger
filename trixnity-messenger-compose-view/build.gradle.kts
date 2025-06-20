import de.connect2x.conventions.registerCoverageTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.parcelize)
}

registerCoverageTask()

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }
    jvm("desktop") {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        tasks.withType<Test>().configureEach {
            maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
        }
    }
    js("web") {
        compilerOptions {
            sourceMap.set(true)
            sourceMapEmbedSources.set(JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS)
        }
        browser {
            commonWebpackConfig {
                showProgress = true
            }
            // Run test in firefox for ci as trixnity/kmp-dockerfiles/base has only firefox
            testRuns.create("firefox").executionTask.configure {
                useKarma {
                    useFirefoxHeadless()
                }
            }
        }
        binaries.library()
        generateTypeScriptDefinitions()
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.trixnityMessenger)
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
                implementation(libs.androidx.annotation)
            }
        }
        val desktopMain by getting {
            dependencies {
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
                implementation(libs.kotlin.test)
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

dependencies {
    androidTestImplementation(libs.ui.test.junit4.android)
    debugImplementation(libs.ui.test.android.manifest)
}

android {
    namespace = "${libs.versions.appId.get()}.compose.view"
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
