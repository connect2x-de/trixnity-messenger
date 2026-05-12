@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import co.touchlab.skie.util.cache.readTextOrNull
import de.connect2x.conventions.configureJava
import de.connect2x.conventions.defaultCompilerOptions
import de.connect2x.conventions.registerCoverageTask
import de.connect2x.conventions.withAndroidLibrary
import de.connect2x.conventions.withBrowser
import de.connect2x.conventions.withIos
import de.connect2x.conventions.withJvm
import de.connect2x.conventions.withWeb
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.tasks.KotlinTest
import java.time.Duration

plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform)
    alias(sharedLibs.plugins.android.library)
    alias(sharedLibs.plugins.compose.multiplatform)
    alias(sharedLibs.plugins.compose.compiler)
    alias(sharedLibs.plugins.kotlin.parcelize)
    alias(sharedLibs.plugins.kotlinx.kover)
    alias(sharedLibs.plugins.kotlin.serialization)
    id("de.connect2x.uitest-infra")
}

configureJava(sharedLibs.versions.targetJvm)
registerCoverageTask("koverXmlReportJvm")

kotlin {
    withSourcesJar()
    defaultCompilerOptions()
    withAndroidLibrary("$group.compose.view") {
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }
    withJvm {
        testRuns.named("test") {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
        tasks.withType<Test>().configureEach {
            maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
        }
    }
    withWeb {
        withBrowser {
            commonWebpackConfig {
                showProgress = true
            }
            testTask {
                useKarma {
                    useFirefoxHeadless()
                    timeout = Duration.ofSeconds(300)
                }
            }
        }
        useEsModules()
        binaries.library()
        generateTypeScriptDefinitions()
    }
    withIos()
    applyDefaultHierarchyTemplate {
        common {
            group("ios") {
                withIos()
            }
            group("skia") {
                withJvm()
                group("web")
                group("ios")
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                api(projects.trixnityMessenger)
                api(sharedLibs.compose.runtime)
                api(sharedLibs.compose.foundation)
                api(sharedLibs.compose.resources)
                api(sharedLibs.compose.material3)
                api(sharedLibs.compose.ui)
                api(sharedLibs.compose.materialIconsExtended)
                api(sharedLibs.decompose)
                api(sharedLibs.decompose.extensions)
                api(sharedLibs.aboutLibraries.compose.m3)
                api(sharedLibs.androidx.lifecycle.runtime.compose)
                implementation(sharedLibs.kotlinx.datetime)
                implementation(sharedLibs.androidx.annotation)
                implementation(libs.okio)
                implementation(sharedLibs.compose.uiUtil)
                implementation(libs.highlights)
                implementation(libs.markdown)

                // FileKit
                implementation(libs.filekit.core)
                implementation(libs.filekit.compose)
            }
        }
        jvmMain {
            dependencies {
                implementation(sharedLibs.ktor.client.okhttp)
                implementation(libs.pdfbox)
            }
        }
        androidMain {
            dependencies {
                implementation(sharedLibs.compose.uiTooling)
                implementation(sharedLibs.androidx.appcompat)
                implementation(sharedLibs.androidx.work.runtime.ktx)
                implementation(sharedLibs.androidx.activity.compose)
                implementation(sharedLibs.compose.uiToolingPreview)
                implementation(sharedLibs.androidx.security.crypto)
                implementation(sharedLibs.ktor.client.okhttp)
            }
        }
        webMain {
            dependencies {
                implementation(npm("copy-webpack-plugin", libs.versions.copyWebpackPlugin.get()))
                implementation(project.dependencies.platform(sharedLibs.kotlin.wrappers.bom))
                implementation(sharedLibs.kotlin.browser)
                implementation(projects.wrappersPdfjs)

                implementation(sharedLibs.compose.ui)
                implementation(sharedLibs.compose.uiUtil)
            }
        }
        commonTest {
            dependencies {
                implementation(sharedLibs.kotlin.test)
                implementation(sharedLibs.compose.uiTest)
                implementation(libs.okio.fakefilesystem)
                implementation(sharedLibs.kotlinx.coroutines.test)
                implementation(sharedLibs.lognity.test)
                implementation(sharedLibs.ktor.client.core)
                implementation(sharedLibs.ktor.client.contentNegotiation)
                implementation(sharedLibs.ktor.serialization.kotlinx.json)
            }
        }
        jvmTest {
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
    buildFeatures {
        compose = true
    }
    defaultConfig {
        testApplicationId = "de.connect2x.trixnity.messenger.compose.view"
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

val uiTestInfraServiceProvider = gradle.sharedServices.registerIfAbsent(
    "uiTestInfraService",
    UITestInfraService::class
) {
    parameters.projectDir.set(layout.projectDirectory)
}

fun Task.configureTestInfra() {
    usesService(uiTestInfraServiceProvider)
    doFirst {
        uiTestInfraServiceProvider.get().startInfra(logger)
    }
}

// native + web
tasks.withType(KotlinTest::class).configureEach {
    configureTestInfra()
}

// JVM (Desktop)
tasks.withType(Test::class) {
    configureTestInfra()
}

// Android Instrumented
tasks.matching { it.name == "connectedDebugAndroidTest" }
    .configureEach {
        configureTestInfra()
        finalizedBy(exportAndroidScreenshots)
    }

tasks.named("iosSimulatorArm64Test") {
    finalizedBy(exportIosScreenshots)
}

val resolveIosSimulatorContainer by tasks.registering {
    group = "verification"
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }

    val resultFile = layout.buildDirectory.file("ios-container-path.txt")

    outputs.file(resultFile)

    doLast {
        val devicesDir = File(
            "${System.getProperty("user.home")}/Library/Developer/CoreSimulator/Devices"
        )

        // find most recently modified device
        val appsDir = devicesDir.listFiles()
            ?.filter { File(it, "data").resolve("Documents").resolve("screenshots").exists() }
            ?.map { File(it, "data").resolve("Documents").resolve("screenshots") }
            ?.onEach { println("${it.absolutePath} (${it.lastModified()}") }
            ?.maxByOrNull { it.lastModified() }
            ?: error("No simulator devices found")

        resultFile.get().asFile.writeText(appsDir.absolutePath)

        println("Resolved iOS container: ${appsDir.absolutePath}")
    }
}

val exportIosScreenshots by tasks.registering(Copy::class) {
    group = "verification"
    dependsOn(resolveIosSimulatorContainer)

    val resultFile = layout.buildDirectory.file("ios-container-path.txt")

    from(resultFile.get().asFile.readTextOrNull())
    into(layout.projectDirectory.dir("screenshots"))
}

val exportAndroidScreenshots by tasks.registering(Exec::class) {
    finalizedBy(removeAndroidScreenshots)

    val packageName = "de.connect2x.trixnity.messenger.compose.view"
    val localPath = layout.projectDirectory.dir("screenshots").asFile

    doFirst {
        localPath.mkdirs()
    }

    commandLine(
        "bash", "-c",
        """
        adb exec-out run-as $packageName sh -c 'cd files/screenshots && tar -cf - "Android instrumented"' | tar -xf - -C ${localPath.absolutePath}
        """.trimIndent()
    )
}

val removeAndroidScreenshots by tasks.registering(Exec::class) {
    val packageName = "de.connect2x.trixnity.messenger.compose.view"

    commandLine(
        "bash", "-c",
        """
        adb exec-out run-as $packageName sh -c 'rm -rf files/screenshots'
        """.trimIndent()
    )
}
