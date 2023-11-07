import co.touchlab.skie.configuration.DefaultArgumentInterop
import co.touchlab.skie.configuration.EnumInterop
import co.touchlab.skie.configuration.SealedInterop
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
    id("com.android.library")
    id("kotlin-parcelize")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("io.kotest.multiplatform")
    id("com.google.devtools.ksp")
    id("co.touchlab.skie")
    `maven-publish`
    id("co.touchlab.kmmbridge")
//    id("org.jetbrains.dokka")
}

kotlin {
    jvmToolchain(Versions.kotlinJvmTarget.number)
    androidTarget {
        compilations.all {
            kotlinOptions.jvmTarget = Versions.kotlinJvmTarget.toString()
        }
        publishLibraryVariants("release")
    }
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = Versions.kotlinJvmTarget.toString()
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
//    listOf(
//        iosArm64(),
//        iosSimulatorArm64(),
//        iosX64()
//    ).forEach {
//        it.binaries.framework {
//            export("com.arkivanov.decompose:decompose:${Versions.decompose}")
//            export("com.arkivanov.essenty:lifecycle:${Versions.essenty}")
////            export("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinxCoroutines}")
//            export("net.folivo:trixnity-client:${Versions.trixnity}")
//            isStatic = true
//        }
//    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
        }
        commonMain {
            dependencies {
                api("net.folivo:trixnity-client:${Versions.trixnity}")
                implementation("net.folivo:trixnity-crypto-core:${Versions.trixnity}")
                implementation("net.folivo:trixnity-client-media-okio:${Versions.trixnity}")
                api("io.ktor:ktor-client-core:${Versions.ktor}")
                api("io.ktor:ktor-client-logging:${Versions.ktor}")
                api("com.arkivanov.decompose:decompose:${Versions.decompose}")
                api("com.arkivanov.essenty:lifecycle:${Versions.essenty}")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinxCoroutines}")
                api("io.github.oshai:kotlin-logging:${Versions.kotlinLogging}")
                api("io.insert-koin:koin-core:${Versions.koin}")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinxSerialization}")
                implementation("com.squareup.okio:okio:${Versions.okio}")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${Versions.kotlinxDatetime}")
                implementation("com.benasher44:uuid:${Versions.uuid}")
                implementation("com.russhwolf:multiplatform-settings:${Versions.multiplatformSettings}")
                implementation("com.russhwolf:multiplatform-settings-coroutines:${Versions.multiplatformSettings}")
                implementation("com.soywiz.korlibs.korim:korim:${Versions.korge}")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.kodein.mock:mockmp-runtime:${Versions.mocKmp}")

                implementation("com.squareup.okio:okio-fakefilesystem:${Versions.okio}")
                implementation("com.russhwolf:multiplatform-settings-test:${Versions.multiplatformSettings}")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.kotlinxCoroutines}")
                implementation("io.kotest:kotest-common:${Versions.kotest}")
                implementation("io.kotest:kotest-framework-engine:${Versions.kotest}")
                implementation("io.kotest:kotest-assertions-core:${Versions.kotest}")
                implementation("io.kotest:kotest-framework-datatest:${Versions.kotest}")
                implementation("ch.qos.logback:logback-classic:${Versions.logback}")
                implementation("io.ktor:ktor-client-mock:${Versions.ktor}")
            }
        }
        jvmMain {
            dependencies {
                implementation("net.folivo:trixnity-client-repository-realm:${Versions.trixnity}")
                implementation("net.java.dev.jna:jna:${Versions.jna}")
                implementation("net.java.dev.jna:jna-platform:${Versions.jna}")
            }
        }
        androidMain {
            dependencies {
                implementation("net.folivo:trixnity-client-repository-realm:${Versions.trixnity}")
                implementation("androidx.activity:activity-ktx:${Versions.activity}")
                implementation("androidx.security:security-crypto:${Versions.crypto}")
            }
        }
        jsMain {
            dependencies {
                implementation("net.folivo:trixnity-client-repository-indexeddb:${Versions.trixnity}")
                implementation("net.folivo:trixnity-client-media-indexeddb:${Versions.trixnity}")
                api(npm("@js-joda/timezone", "2.3.0"))
                implementation("org.jetbrains.kotlin-wrappers:kotlin-browser:1.0.0-pre.635")
            }
        }
        appleMain {
            dependencies {
                implementation("net.folivo:trixnity-client-repository-realm:${Versions.trixnity}")
                implementation("io.ktor:ktor-client-darwin:${Versions.ktor}")
            }
        }
        jvmTest {
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
    }
}

android {
    namespace = "de.connect2x.trixnity.messenger"
    compileSdk = 33

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = Versions.kotlinJvmTarget
        targetCompatibility = Versions.kotlinJvmTarget
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

dependencies {
    configurations
        .filter { it.name.startsWith("ksp") && it.name.contains("Test") }
        .forEach {
            add(it.name, "org.kodein.mock:mockmp-processor:${Versions.mocKmp}")
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
//    val dokkaJar by tasks.registering(Jar::class) {
//        onlyIf { isCI }
//        dependsOn(tasks.dokkaHtml)
//        from(tasks.dokkaHtml.flatMap { it.outputDirectory })
//        archiveClassifier.set("javadoc")
//    }

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
    publications.configureEach {
        if (this is MavenPublication) {
            pom {
                name.set(project.name)
                description.set("Multiplatform Kotlin SDK for Matrix messengers")
                url.set("https://gitlab.com/connect2x/trixnity-messenger/trixnity-messenger")
                licenses {
                    license {
                        name.set("GNU AFFERO GENERAL PUBLIC LICENSE version 3")
                        url.set("https://www.gnu.org/licenses/agpl-3.0.html")
                    }
                }
                developers {
                    developer {
                        id.set("michael.thiele")
                        id.set("benkuly")
                    }
                }
                scm {
                    url.set("https://gitlab.com/connect2x/trixnity-messenger/trixnity-messenger")
                }
            }
//            artifact(dokkaJar)
        }
    }
}

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