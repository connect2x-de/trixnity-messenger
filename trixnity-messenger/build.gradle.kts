@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import co.touchlab.skie.configuration.DefaultArgumentInterop
import co.touchlab.skie.configuration.EnumInterop
import co.touchlab.skie.configuration.FlowInterop
import co.touchlab.skie.configuration.FunctionInterop
import co.touchlab.skie.configuration.SealedInterop
import co.touchlab.skie.configuration.SuspendInterop
import de.connect2x.conventions.CI
import de.connect2x.conventions.configureJava
import de.connect2x.conventions.defaultCompilerOptions
import de.connect2x.conventions.registerCoverageTask
import de.connect2x.conventions.withAndroidLibrary
import de.connect2x.conventions.withIos
import de.connect2x.conventions.withJs
import de.connect2x.conventions.withJvm
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform)
    alias(sharedLibs.plugins.kotlin.serialization)
    alias(sharedLibs.plugins.android.library)
    alias(sharedLibs.plugins.mokkery)
    alias(sharedLibs.plugins.skie)
    alias(sharedLibs.plugins.kmmBridge)
    alias(sharedLibs.plugins.mavenPublish)
}

configureJava(sharedLibs.versions.targetJvm)
registerCoverageTask("koverXmlReportJvm")

kotlin {
    withSourcesJar()
    defaultCompilerOptions()
    withAndroidLibrary()
    withJvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        tasks.withType<Test>().configureEach {
            if (!CI.isCI) {
                maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
            }
        }
    }
    withJs {
        browser {
            commonWebpackConfig {
                showProgress = true
            }
            testTask {
                useKarma {
                    useFirefoxHeadless()
                    useConfigDirectory(rootDir.resolve("karma.config.d"))
                }
            }
        }
        useEsModules()
        binaries.library()
        generateTypeScriptDefinitions()
    }
    withIos {
        binaries.framework {
            baseName = "TrixnityMessenger"
            export(sharedLibs.decompose)
            export(libs.trixnity.client)
            isStatic = true
        }
    }
    applyDefaultHierarchyTemplate {
        common {
            group("ios") {
                withIos()
            }
            group("jvmAndNative") {
                withJvm()
                withAndroidTarget()
                group("ios")
            }
            group("jvmAndAndroid") {
                withJvm()
                withAndroidTarget()
            }
            group("nonAndroid") {
                withJvm()
                group("ios")
                withJs()
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                api(libs.trixnity.client)
                implementation(libs.trixnity.client.cryptodriver.libolm)
                // to prevent diverging trixnity versions downstream,
                api(libs.trixnity.client.cryptodriver.vodozemac)
                implementation(libs.trixnity.crypto.core)
                api(sharedLibs.ktor.client.logging)
                api(sharedLibs.decompose)
                api(sharedLibs.kotlinx.coroutines.core)
                api(sharedLibs.koin.core)
                api(sharedLibs.kotlinx.serialization.core)
                api(sharedLibs.kotlinx.serialization.json)
                api(sharedLibs.kotlinx.datetime)
                api(sharedLibs.lognity.api)
                api(libs.sysnotify)
                implementation(libs.okio)
                implementation(libs.kim)
                implementation(libs.markdown)
                implementation(libs.ksoup.html)
                implementation(sharedLibs.skie.annotations)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(sharedLibs.kotlin.test)
                implementation(sharedLibs.kotlinx.coroutines.test)
                implementation(sharedLibs.kotest.assertions.core)
                implementation(sharedLibs.ktor.client.mock)
                implementation(sharedLibs.mokkery.coroutines)
                implementation(sharedLibs.lognity.test)
                implementation(libs.okio.fakefilesystem)
            }
        }
        val jvmAndNativeMain by getting {
            dependencies {
                implementation(libs.trixnity.client.repository.room)
                // implementation(sharedLibs.androidx.sqlite3mc.bundled)
                implementation(libs.sqlitenity.bundled)
                implementation(libs.sqlitenity.compat)
                api(libs.trixnity.client.media.okio)
            }
        }
        val jvmAndAndroidMain by getting {
            dependencies {
                api(sharedLibs.lognity.slf4j)
            }
        }
        jvmMain {
            kotlin.srcDirs("src/icu4j/kotlin")
            dependencies {
                implementation(sharedLibs.jna)
                implementation(sharedLibs.jna.platform)
                implementation(libs.icu4j)
            }
        }
        androidMain {
            dependencies {
                api(sharedLibs.androidx.activity.ktx)
                implementation(sharedLibs.androidx.security.crypto)
                implementation(sharedLibs.androidx.browser)
                implementation(sharedLibs.androidx.work.runtime.ktx)
                implementation(sharedLibs.androidx.lifecycle.livedata.ktx)
            }
        }
        iosMain {
            dependencies {
                // since with iOS projects, we cannot include the engine, we select it here
                implementation(sharedLibs.ktor.client.darwin)
            }
        }
        jsMain {
            dependencies {
                implementation(libs.trixnity.client.repository.indexeddb)
                api(libs.trixnity.client.media.opfs)
                api(libs.trixnity.client.media.indexeddb)
                api(npm("@js-joda/timezone", libs.versions.jsJoda.get()))
                implementation(projects.wrappersZipjs)
                implementation(project.dependencies.platform(sharedLibs.kotlin.wrappers.bom))
                implementation(sharedLibs.kotlin.browser)
                implementation(sharedLibs.ktor.client.js) // since there is only 1 engine in web, we select it here
            }
        }
        androidUnitTest {
            kotlin.srcDirs("src/icu4j/kotlin")
            dependencies {
                implementation(libs.icu4j)
            }
        }
    }
}

android {
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

dependencies {
    implementation(sharedLibs.ktor.client.logging)
}

skie {
    analytics {
        disableUpload.set(true)
    }
    build {
        produceDistributableFramework()
        enableConcurrentSkieCompilation = true
    }
    features {
        group {
            enableSwiftUIObservingPreview = true
            FlowInterop.Enabled(true)
            EnumInterop.Enabled(false)
            SealedInterop.Enabled(false)

            // is disabled by default (see https://skie.touchlab.co/features/default-arguments),
            // so we have to use annotations where necessary
            DefaultArgumentInterop.Enabled(true)
        }
        group("$group.settings") {
            FlowInterop.Enabled(false)
            EnumInterop.Enabled(false)
            SealedInterop.Enabled(false)
            DefaultArgumentInterop.Enabled(false)
            FunctionInterop.FileScopeConversion.Enabled(false)
            SuspendInterop.Enabled(false)
        }
    }
}

if (CI.isCI) {
    kmmbridge {
        mavenPublishArtifacts()
        spm()
    }
}
