import co.touchlab.skie.configuration.DefaultArgumentInterop
import co.touchlab.skie.configuration.EnumInterop
import co.touchlab.skie.configuration.FlowInterop
import co.touchlab.skie.configuration.FunctionInterop
import co.touchlab.skie.configuration.SealedInterop
import co.touchlab.skie.configuration.SuspendInterop
import de.connect2x.conventions.configureJava
import de.connect2x.conventions.isCI
import de.connect2x.conventions.registerCoverageTask
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode

plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform)
    alias(sharedLibs.plugins.kotlin.serialization)
    alias(sharedLibs.plugins.android.library)
    alias(sharedLibs.plugins.mokkery)
    alias(sharedLibs.plugins.skie)
    alias(sharedLibs.plugins.kmmBridge)
    alias(sharedLibs.plugins.dokka)
    `maven-publish`
}

configureJava(sharedLibs.versions.targetJvm)
registerCoverageTask()

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    androidTarget {
        publishLibraryVariants("release")
    }
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        tasks.withType<Test>().configureEach {
            if (isCI.not()) {
                maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
            }
        }
    }
    js {
        compilerOptions {
            sourceMap.set(true)
            sourceMapEmbedSources.set(JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS)
        }
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
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach {
        it.binaries.framework {
            baseName = "TrixnityMessenger"
            export(sharedLibs.decompose)
            export(libs.trixnity.client)
            isStatic = true
        }
    }
    applyDefaultHierarchyTemplate()
    sourceSets {
        all {
            languageSettings.optIn("kotlin.uuid.ExperimentalUuidApi")
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }
        val commonMain by getting {
            dependencies {
                api(libs.trixnity.client)
                implementation(libs.trixnity.crypto.driver.libolm)
                implementation(libs.trixnity.crypto.core)
                api(sharedLibs.ktor.client.logging)
                api(sharedLibs.decompose)
                api(sharedLibs.kotlinx.coroutines.core)
                api(libs.logging)
                api(sharedLibs.koin.core)
                api(sharedLibs.kotlinx.serialization.core)
                api(sharedLibs.kotlinx.serialization.json)
                api(sharedLibs.kotlinx.datetime)
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
                implementation(libs.okio.fakefilesystem)
            }
        }
        val jvmAndNativeMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.trixnity.client.repository.room)
                // implementation(sharedLibs.androidx.sqlite3mc.bundled)
                implementation(libs.sqlitenity.bundled)
                implementation(libs.sqlitenity.compat)
                api(libs.trixnity.client.media.okio)
            }
        }
        jvmMain {
            dependsOn(jvmAndNativeMain)
            kotlin.srcDirs("src/icu4j/kotlin")
            dependencies {
                implementation(sharedLibs.jna)
                implementation(sharedLibs.jna.platform)
                implementation(libs.icu4j)
            }
        }
        androidMain {
            dependsOn(jvmAndNativeMain)
            dependencies {
                implementation(sharedLibs.androidx.activity.ktx)
                implementation(sharedLibs.androidx.security.crypto)
                implementation(sharedLibs.androidx.browser)
                implementation(libs.media3.exoplayer)
                implementation(sharedLibs.androidx.work.runtime.ktx)
                implementation(sharedLibs.androidx.lifecycle.livedata.ktx)
            }
        }
        nativeMain {
            dependsOn(jvmAndNativeMain)
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
                implementation(project(":wrappers-zipjs"))
                implementation(project.dependencies.platform(sharedLibs.kotlin.wrappers.bom))
                implementation(sharedLibs.kotlin.browser)
                implementation(sharedLibs.ktor.client.js) // since there is only 1 engine in web, we select it here
            }
        }
        val nonAndroidTest by creating {
            dependsOn(commonTest)
        }
        val jvmAndNativeTest by creating {
            dependsOn(commonTest)
        }
        jvmTest {
            dependsOn(jvmAndNativeTest)
            dependsOn(nonAndroidTest)
            dependencies {
                implementation(libs.logback.classic)
            }
        }
        nativeTest {
            dependsOn(nonAndroidTest)
            dependsOn(jvmAndNativeTest)
        }
        jsTest {
            dependsOn(nonAndroidTest)
        }
        androidUnitTest {
            dependsOn(jvmAndNativeTest)
            kotlin.srcDirs("src/icu4j/kotlin")
            dependencies {
                implementation(libs.logback.classic)
                implementation(libs.icu4j)
            }
        }
    }
}

android {
    namespace = "$group.trixnity.messenger"
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
        group("$group.trixnity.messenger.settings") {
            FlowInterop.Enabled(false)
            EnumInterop.Enabled(false)
            SealedInterop.Enabled(false)
            DefaultArgumentInterop.Enabled(false)
            FunctionInterop.FileScopeConversion.Enabled(false)
            SuspendInterop.Enabled(false)
        }
    }
}

if (isCI) {
    kmmbridge {
        mavenPublishArtifacts()
        spm()
    }
}
