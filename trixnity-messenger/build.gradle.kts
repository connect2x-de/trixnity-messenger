import co.touchlab.skie.configuration.DefaultArgumentInterop
import co.touchlab.skie.configuration.EnumInterop
import co.touchlab.skie.configuration.FlowInterop
import co.touchlab.skie.configuration.FunctionInterop
import co.touchlab.skie.configuration.SealedInterop
import co.touchlab.skie.configuration.SuspendInterop

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    `maven-publish`
    alias(libs.plugins.mokkery)
    alias(libs.plugins.skie)
    alias(libs.plugins.kmmbridge)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlinx.kover)
}

kotlin {
    val kotlinJvm = libs.versions.kotlinJvmTarget.get()

    jvmToolchain(kotlinJvm.toInt())
    androidTarget {
        compilations.configureEach {
            kotlinOptions.jvmTarget = kotlinJvm
        }
        publishLibraryVariants("release")
    }
    jvm {
        compilations.configureEach {
            kotlinOptions.jvmTarget = kotlinJvm
        }
    }
    js {
        browser {
            testTask {
                useKarma {
                    useFirefoxHeadless()
                }
            }
        }
        nodejs()
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
            export(libs.decompose)
            export(libs.trixnity.client)
            isStatic = true
        }
    }
    applyDefaultHierarchyTemplate()
    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
            languageSettings.optIn("kotlin.uuid.ExperimentalUuidApi")
        }
        val commonMain by getting {
            dependencies {
                api(libs.trixnity.client)
                implementation(libs.trixnity.crypto.core)
                api(libs.ktor.client.logging)
                api(libs.decompose)
                api(libs.kotlinx.coroutines)
                api(libs.logging)
                api(libs.koin.core)
                api(libs.kotlinx.serialization)
                implementation(libs.okio)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kim)
                implementation(libs.markdown)
                implementation(libs.skie.annotations)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.okio.fakefilesystem)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotest.assertion.core)
                implementation(libs.ktor.client.mock)
                implementation(libs.mokkery.coroutines)
            }
        }
        val jvmAndNativeMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.trixnity.client.repository.room)
                api(libs.trixnity.client.media.okio)
            }
        }
        jvmMain {
            dependsOn(jvmAndNativeMain)
            kotlin.srcDirs("src/icu4j/kotlin")
            dependencies {
                implementation(libs.bundles.jna)
                implementation(libs.androidx.sqlite3mc.bundled)
                implementation(libs.icu4j)
            }
        }
        androidMain {
            dependsOn(jvmAndNativeMain)
            dependencies {
                implementation(libs.androidx.activity)
                implementation(libs.androidx.security.crypto)
                implementation(libs.androidx.browser)
                implementation(libs.androidx.sqlite3.bundled)
            }
        }
        nativeMain {
            dependsOn(jvmAndNativeMain)
        }
        iosMain {
            dependencies {
                // since with iOS projects, we cannot include the engine, we select it here
                implementation(libs.ktor.client.darwin)
                implementation(libs.androidx.sqlite3.bundled)
            }
        }
        jsMain {
            dependencies {
                implementation(libs.trixnity.client.repository.indexeddb)
                api(libs.trixnity.client.media.opfs)
                api(libs.trixnity.client.media.indexeddb)
                api(npm("@js-joda/timezone", libs.versions.jsJoda.get()))
                implementation(npm("@zip.js/zip.js", libs.versions.zipjs.get()))
                implementation(npm("pdfjs-dist", libs.versions.pdfjs.get()))
                implementation(project.dependencies.platform(libs.kotlin.wrappers.bom))
                implementation(libs.kotlin.browser)
                implementation(libs.ktor.client.js) // since there is only 1 engine in web, we select it here
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

tasks.register("testCoverage") {
    val reportTask = tasks.named("koverXmlReportJvm").get()
    dependsOn(reportTask)
    doLast {
        val regex = """<counter type="INSTRUCTION" missed="(\d+)" covered="(\d+)"/>""".toRegex()
        for (file in reportTask.outputs.files) {
            file.useLines { lines ->
                val coverage = lines.last(regex::containsMatchIn)
                regex.find(coverage)?.let { coverageData ->
                    val covered = coverageData.groupValues[2].toInt()
                    val missed = coverageData.groupValues[1].toInt()
                    println("Total test coverage: ${covered * 100 / (missed + covered)}%")
                }
            }
        }
    }
}

android {
    namespace = "de.connect2x.trixnity.messenger"
    compileSdk = libs.versions.androidCompileSDK.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinimalSDK.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.kotlinJvmTarget.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.kotlinJvmTarget.get())
    }

    sourceSets {
        named("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")

            assets.srcDir(File(layout.buildDirectory.asFile.get(), "generated/moko/androidMain/assets"))
            res.srcDir(File(layout.buildDirectory.asFile.get(), "generated/moko/androidMain/res"))
        }
    }

    buildTypes {
        release {
            isDefault = true
        }
    }
}

skie {
    analytics {
        disableUpload.set(true)
    }
    build {
        produceDistributableFramework()
        this.enableConcurrentSkieCompilation = true
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
        group("de.connect2x.trixnity.messenger.settings") {
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
