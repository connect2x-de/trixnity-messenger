import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.mikepenz.aboutlibraries.plugin.AboutLibrariesExtension
import de.connect2x.conventions.configureJava
import de.connect2x.conventions.registerCoverageTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform)
    alias(sharedLibs.plugins.android.library)
    alias(sharedLibs.plugins.compose.multiplatform)
    alias(sharedLibs.plugins.compose.compiler)
    alias(sharedLibs.plugins.kotlin.parcelize)
    alias(sharedLibs.plugins.kotlinx.kover)
}

configureJava(sharedLibs.versions.targetJvm)
registerCoverageTask()

tasks.register("koverXmlReportJvm") {
    val desktopReport = tasks.named("koverXmlReportDesktop")

    inputs.files(desktopReport)
    outputs.files(desktopReport)
}

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
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    )
    applyDefaultHierarchyTemplate()
    sourceSets {
        all {
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }
        val commonMain by getting {
            dependencies {
                api(projects.trixnityMessenger)
                api(compose.runtime)
                api(compose.foundation)
                api(compose.components.resources)
                api(compose.material3)
                api(compose.ui)
                api(compose.materialIconsExtended)
                api(sharedLibs.decompose)
                api(sharedLibs.decompose.extensions)
                api(sharedLibs.aboutLibraries.compose.m3)
                implementation(sharedLibs.kotlinx.datetime)
                implementation(sharedLibs.androidx.annotation)
                implementation(libs.okio)
                implementation(compose.uiUtil)
                implementation(libs.sysnotify)
                implementation(libs.highlights)

                // FileKit
                implementation(libs.filekit.core)
                implementation(libs.filekit.compose)
            }
        }
        val desktopAndAndroidMain by creating {
            dependsOn(commonMain)
        }
        val desktopMain by getting {
            dependsOn(desktopAndAndroidMain)
            dependencies {
                implementation(sharedLibs.ktor.client.okhttp)
                implementation(libs.pdfbox)
            }
        }
        androidMain {
            dependsOn(desktopAndAndroidMain)
            dependencies {
                implementation(compose.uiTooling)
                implementation(sharedLibs.androidx.appcompat)
                implementation(sharedLibs.androidx.work.runtime.ktx)
                implementation(sharedLibs.androidx.lifecycle.livedata.ktx)
                implementation(sharedLibs.androidx.activity.compose)
                implementation(libs.logback.android)
                implementation(compose.preview)
                implementation(sharedLibs.androidx.security.crypto)
                implementation(sharedLibs.ktor.client.okhttp)
                implementation(sharedLibs.firebase.messaging)
                // for Previews:
                implementation(libs.slf4j.api)
            }
        }
        val webMain by getting {
            dependencies {
                implementation(npm("copy-webpack-plugin", libs.versions.copyWebpackPlugin.get()))
                implementation(project.dependencies.platform(sharedLibs.kotlin.wrappers.bom))
                implementation(sharedLibs.kotlin.browser)
                implementation(project(":wrappers-pdfjs"))
            }
        }

        commonTest {
            dependencies {
                implementation(sharedLibs.kotlin.test)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
                implementation(libs.okio.fakefilesystem)
                implementation(sharedLibs.kotlinx.coroutines.test)
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(sharedLibs.kotlinx.coroutines.swing)
            }
        }
    }
}

dependencies {
    androidTestImplementation(sharedLibs.compose.ui.test.junit4.android)
    debugImplementation(sharedLibs.compose.ui.test.android.manifest)
}

android {
    namespace = "de.connect2x.trixnity.messenger.compose.view"
    compileSdk = sharedLibs.versions.androidCompileSDK.get().toInt()
    buildFeatures {
        compose = true
    }
    defaultConfig {
        minSdk = sharedLibs.versions.androidMinimalSDK.get().toInt()
        testOptions.targetSdk = sharedLibs.versions.androidTargetSDK.get().toInt()
        lint.targetSdk = sharedLibs.versions.androidTargetSDK.get().toInt()
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
