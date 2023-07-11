import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
    id("com.android.library")
    id("kotlin-parcelize")
    kotlin("multiplatform")
    kotlin("plugin.serialization").version(Versions.kotlin)
    id("org.kodein.mock.mockmp")
    id("io.kotest.multiplatform")
    `maven-publish`
}

kotlin {
    jvmToolchain(Versions.kotlinJvmTarget.number)
    android {
        compilations.all {
            kotlinOptions.jvmTarget = Versions.kotlinJvmTarget.toString()
        }
    }
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = Versions.kotlinJvmTarget.toString()
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            // testLogging.showStandardStreams = true   // activate when detailed information in tests is required
        }
        tasks.withType<Test>().configureEach {
            maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
        }
    }
    js(IR) {
        browser {
            testTask {
                enabled = false // TODO
//                useKarma {
//                    useFirefoxHeadless()
//                    useConfigDirectory(rootDir.resolve("karma.config.d"))
//                    webpackConfig.configDirectory = rootDir.resolve("webpack.config.d")
//                }
            }
        }
        binaries.executable()
    }
    ios()
    iosSimulatorArm64()

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
        val commonMain by getting {
            dependencies {
                api("net.folivo:trixnity-client:${Versions.trixnity}")
                api("io.ktor:ktor-client-core:${Versions.ktor}")
                api("io.ktor:ktor-client-logging:${Versions.ktor}")
                api("com.arkivanov.decompose:decompose:${Versions.decompose}")
                api("com.arkivanov.essenty:lifecycle:${Versions.essenty}")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinxCoroutines}")
                api("io.github.oshai:kotlin-logging:${Versions.kotlinLogging}")
                api("io.insert-koin:koin-core:${Versions.koin}")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinxSerialization}")
                implementation("com.squareup.okio:okio:${Versions.okio}") // TODO does not work with Browser JS -> use ByteArrayFlow
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${Versions.kotlinxDatetime}")
                implementation("com.benasher44:uuid:${Versions.uuid}")
                implementation("com.russhwolf:multiplatform-settings:${Versions.multiplatformSettings}")
                implementation("com.russhwolf:multiplatform-settings-coroutines:${Versions.multiplatformSettings}")
                implementation("com.soywiz.korlibs.korim:korim:${Versions.korge}")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("com.squareup.okio:okio-fakefilesystem:${Versions.okio}")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.kotlinxCoroutines}")
                implementation("io.kotest:kotest-common:${Versions.kotest}")
                implementation("io.kotest:kotest-framework-engine:${Versions.kotest}")
                implementation("io.kotest:kotest-assertions-core:${Versions.kotest}")
                implementation("io.kotest:kotest-framework-datatest:${Versions.kotest}")
                implementation("ch.qos.logback:logback-classic:${Versions.logback}")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation("net.folivo:trixnity-client-repository-realm:${Versions.trixnity}")
                implementation("net.folivo:trixnity-client-media-okio:${Versions.trixnity}")
                implementation("net.java.dev.jna:jna:${Versions.jna}")
                implementation("net.java.dev.jna:jna-platform:${Versions.jna}")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("net.folivo:trixnity-client-repository-realm:${Versions.trixnity}")
                implementation("net.folivo:trixnity-client-media-okio:${Versions.trixnity}")
                implementation("androidx.activity:activity-ktx:${Versions.activity}")
                implementation("androidx.security:security-crypto:${Versions.crypto}")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("net.folivo:trixnity-client-repository-indexeddb:${Versions.trixnity}")
                implementation("net.folivo:trixnity-client-media-indexeddb:${Versions.trixnity}")
                api(npm("@js-joda/timezone", "2.3.0"))
            }
        }
        val iosMain by getting {
            dependencies {
                implementation("net.folivo:trixnity-client-repository-realm:${Versions.trixnity}")
                implementation("net.folivo:trixnity-client-media-okio:${Versions.trixnity}")
                implementation("io.ktor:ktor-client-darwin:${Versions.ktor}")
            }
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
        val desktopTest by getting {
            dependencies {
                implementation("io.kotest:kotest-runner-junit5:${Versions.kotest}")
                implementation("io.ktor:ktor-client-java:${Versions.ktor}")
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation("io.ktor:ktor-client-android:${Versions.ktor}")
            }
        }
        val iosTest by getting
        val iosSimulatorArm64Test by getting {
            dependsOn(iosTest)
        }
    }
}

android {
    compileSdk = 33

    defaultConfig {
        minSdk = 28
        targetSdk = 33
    }

    compileOptions {
        sourceCompatibility = Versions.kotlinJvmTarget
        targetCompatibility = Versions.kotlinJvmTarget
    }

    sourceSets {
        named("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")

            assets.srcDir(File(buildDir, "generated/moko/androidMain/assets"))
            res.srcDir(File(buildDir, "generated/moko/androidMain/res"))
        }
    }
}

//afterEvaluate {
//    rootProject.extensions.configure<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension> {
//        versions.webpackDevServer.version = "4.0.0"
//        versions.webpackCli.version = "4.10.0"
//    }
//}

rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
    rootProject.the<YarnRootExtension>().yarnLockMismatchReport = YarnLockMismatchReport.WARNING // NONE | FAIL
    rootProject.the<YarnRootExtension>().reportNewYarnLock = true
    rootProject.the<YarnRootExtension>().yarnLockAutoReplace = true
}

publishing {
    repositories {
        maven {
            url = uri("${System.getenv("CI_API_V4_URL")}/projects/47538655/packages/maven")
            name = "GitLab"
            credentials(HttpHeaderCredentials::class) {
                name = "Job-Token"
                value = System.getenv("CI_JOB_TOKEN")
            }
            authentication {
                create("header", HttpHeaderAuthentication::class)
            }
        }
    }
}