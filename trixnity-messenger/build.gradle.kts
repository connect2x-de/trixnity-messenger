import co.touchlab.skie.configuration.DefaultArgumentInterop
import co.touchlab.skie.configuration.EnumInterop
import co.touchlab.skie.configuration.SealedInterop

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    `maven-publish`
    alias(libs.plugins.kotest)
    alias(libs.plugins.mokkery)
    alias(libs.plugins.skie)
    alias(libs.plugins.kmmbridge)
    alias(libs.plugins.dokka)
}

kotlin {
    val kotlinJvm = libs.versions.kotlinJvmTarget.get()
    jvmToolchain(kotlinJvm.toInt())
    androidTarget {
        compilations.all {
            kotlinOptions.jvmTarget = kotlinJvm
        }
        publishLibraryVariants("release")
    }
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = kotlinJvm
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            // testLogging.showStandardStreams = true   // activate when detailed information in tests is required
        }
        tasks.withType<Test>().configureEach {
            if (isCI.not()) {
                maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
            }
        }
    }
    js(IR) {
        browser {
            testTask(Action {
                enabled = false // TODO
//                useKarma {
//                    useFirefoxHeadless()
//                    useConfigDirectory(rootDir.resolve("karma.config.d"))
//                    webpackConfig.configDirectory = rootDir.resolve("webpack.config.d")
//                }
            }
            )
        }
        binaries.library()
        generateTypeScriptDefinitions()
    }
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach {
        it.binaries.framework {
            export(libs.decompose)
            export(libs.trixnity.client)
        }
    }
    applyDefaultHierarchyTemplate()
    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
        }
        commonMain {
            dependencies {
                api(libs.trixnity.client)
                implementation(libs.trixnity.crypto.core)
                implementation(libs.trixnity.client.media)
                api(libs.ktor.client.logging)
                api(libs.decompose)
                api(libs.kotlinx.coroutines)
                api(libs.logging)
                api(libs.koin.core)
                api(libs.kotlinx.serialization)
                implementation(libs.okio)
                implementation(libs.kotlinx.datetime)
                implementation(libs.uuid)
                implementation(libs.korge)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.okio.fakefilesystem)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.bundles.kotest)
                implementation(libs.ktor.client.mock)
            }
        }
        val jvmAndNativeMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.trixnity.client.realm)
            }
        }
        jvmMain {
            dependsOn(jvmAndNativeMain)
            dependencies {
                implementation(libs.bundles.jna)
            }
        }
        androidMain {
            dependsOn(jvmAndNativeMain)
            dependencies {
                implementation(libs.androidx.activity)
                implementation(libs.androidx.security.crypto)
            }
        }
        nativeMain {
            dependsOn(jvmAndNativeMain)
        }
        iosMain {
            dependencies {
//                implementation(libs.ktor.client.drawin)
            }
        }
        jsMain {
            dependencies {
                implementation(libs.trixnity.client.repository.indexeddb)
                implementation(libs.trixnity.client.media.opfs)
                api(npm("@js-joda/timezone", libs.versions.jsJoda.get()))
                implementation(npm("@zip.js/zip.js", libs.versions.zipjs.get()))
                implementation(project.dependencies.platform(libs.kotlin.wrappers.bom))
                implementation(libs.kotlin.browser)
            }
        }
        val jvmAndNativeTest by creating {
            dependsOn(commonTest.get())
        }
        jvmTest {
            dependsOn(jvmAndNativeTest)
            dependencies {
                implementation(libs.kotest.junit.runner)
//                implementation(libs.ktor.client.java)
            }
        }
        nativeTest {
            dependsOn(jvmAndNativeTest)
        }
        val androidUnitTest by getting {
            dependsOn(jvmAndNativeTest)
            dependencies {
//                implementation(libs.ktor.client.android)
            }
        }
    }
}

android {
    namespace = "de.connect2x.trixnity.messenger"
    compileSdk = 33

    defaultConfig {
        minSdk = 28
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

//publishing {
//    val dokkaJar by tasks.registering(Jar::class) {
//        dependsOn(tasks.dokkaHtml)
//        from(tasks.dokkaHtml.flatMap { it.outputDirectory })
//        archiveClassifier.set("javadoc")
//        onlyIf { isCI }
//    }
//
//    repositories {
//        maven {
//            url = uri("${System.getenv("CI_API_V4_URL")}/projects/47538655/packages/maven")
//            name = "GitLab"
//            credentials(HttpHeaderCredentials::class) {
//                name = "Job-Token"
//                value = System.getenv("CI_JOB_TOKEN")
//            }
//            authentication {
//                create("header", HttpHeaderAuthentication::class)
//            }
//        }
//    }
//    publications.configureEach {
//        if (this is MavenPublication) {
//            pom {
//                name.set(project.name)
//                description.set("Multiplatform Kotlin SDK for Matrix messengers")
//                url.set("https://gitlab.com/connect2x/trixnity-messenger/trixnity-messenger")
//                licenses {
//                    license {
//                        name.set("GNU AFFERO GENERAL PUBLIC LICENSE version 3")
//                        url.set("https://www.gnu.org/licenses/agpl-3.0.html")
//                    }
//                }
//                developers {
//                    developer {
//                        id.set("michael.thiele")
//                        id.set("benkuly")
//                    }
//                }
//                scm {
//                    url.set("https://gitlab.com/connect2x/trixnity-messenger/trixnity-messenger")
//                }
//            }
//            if (isCI) artifact(dokkaJar)
//        }
//    }
//}

skie {
    analytics {
        disableUpload.set(true)
    }
    features {
        group {
            EnumInterop.Enabled(false)
            SealedInterop.Enabled(false)
            DefaultArgumentInterop.Enabled(false)
        }
    }
}

if (isCI) {
    kmmbridge {
        mavenPublishArtifacts()
        spm()
    }
}
