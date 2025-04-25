import de.connect2x.conventions.isCI
import de.connect2x.conventions.registerMultiplatformLicensesTasks
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.aboutlibraries.plugin)
    // TODO active when you want to use google-services for notifications (needs google-services.json)
    // alias(libs.plugins.google.services)
}

val version = libs.versions.trixnityMessengerApp.get()
val appName = libs.versions.appName.get()
val appId = libs.versions.appId.get()

enum class BuildFlavor { PROD, DEV }

val buildFlavor = BuildFlavor.valueOf(System.getenv("MESSENGER_BUILD_FLAVOR") ?: if (isCI) "PROD" else "DEV")

kotlin {
    androidTarget()
    jvm("desktop")
    js("web") {
        compilerOptions {
            sourceMap.set(true)
            sourceMapEmbedSources.set(JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS)
        }
        browser {
            commonWebpackConfig {
                showProgress = true
            }
            runTask {
                mainOutputFileName = "$appId.js"
            }
            webpackTask {
                mainOutputFileName = "$appId.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.trixnityMessengerComposeView)
                implementation(compose.components.resources)
            }
        }
        val desktopMain by getting {
            dependencies {
                // this is needed to create lock files working on all machines
                if (System.getProperty("bundleAll") == "true") {
                    implementation(compose.desktop.linux_x64)
                    implementation(compose.desktop.linux_arm64)
                    implementation(compose.desktop.windows_x64)
                    implementation(compose.desktop.macos_x64)
                    implementation(compose.desktop.macos_arm64)
                } else {
                    implementation(compose.desktop.currentOs)
                }
                implementation(libs.logback.classic)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        androidMain {
            dependencies {
                implementation(compose.uiTooling)
                implementation(libs.bundles.android.common)
                implementation(libs.slf4j.api)
                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.firebase.messaging.ktx)
            }
        }
        val webMain by getting {
            dependencies {
                implementation(npm("copy-webpack-plugin", libs.versions.copyWebpackPlugin.get()))
            }
        }
    }
}

registerMultiplatformLicensesTasks { licenseTask, target, _ ->
    // TODO: move this into c2x-conventions eventually
    val targetName = target.targetName
    val buildConfigTask =
        tasks.register("generateBuildConfig${targetName.capitalized()}") {
            dependsOn(licenseTask)
            group = "build config"
            val generatedSrc =
                layout.buildDirectory.dir("generatedSrc/${targetName}Main/kotlin")
            doLast {
                val outputFile = generatedSrc.get()
                    .dir(appId.replace(".", "/"))
                    .file("BuildConfig.kt")
                val quotes = "\"\"\""
                val licencesString = licenseTask.get().outputFile.get().asFile.readText()
                    .replace("$", "\${'$'}")
                    .replace(quotes, "")

                val buildConfigString =
                    """
            package $appId

            actual val BuildConfig: CommonBuildConfig = object : CommonBuildConfig {
                override val version: String = "$version"
                override val flavor: Flavor = Flavor.valueOf("$buildFlavor")
                override val appName: String = "$appName"
                override val appId: String = "$appId"
                override val licenses: String = $quotes$licencesString$quotes
            }
        """.trimIndent()
                outputFile.asFile.apply {
                    ensureParentDirsCreated()
                    createNewFile()
                    writeText(buildConfigString)
                }
            }
            outputs.dirs(generatedSrc)
        }
    kotlin.sourceSets.named("${targetName}Main") {
        kotlin.srcDir(buildConfigTask.map { it.outputs })
    }
}

compose {
    desktop {
        application {
            mainClass = "$appId.desktop.MainKt"
            jvmArgs("-Xmx1G", "-XX:+HeapDumpOnOutOfMemoryError")
            buildTypes.release.proguard {
                isEnabled = false
            }
            nativeDistributions {
                modules("java.net.http", "java.sql", "java.naming")
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                // @see https://github.com/JetBrains/compose-jb/tree/master/tutorials/Native_distributions_and_local_execution#jvm-resource-loading
                appResourcesRootDir = layout.buildDirectory
                packageName = appId
                packageVersion = libs.versions.trixnityMessengerApp.get()

                windows {
                    menu = true
                    iconFile.set(project.file("src/desktopMain/resources/logo.ico"))
                }
                macOS {
                    dockName = appName
                    iconFile.set(project.file("src/desktopMain/resources/logo.icns"))
                }
                linux {
                    modules("jdk.security.auth")
                }
            }
        }
    }
}

android {
    namespace = appId
    compileSdk = libs.versions.androidCompileSDK.get().toInt()
    buildFeatures {
        compose = true
    }
    defaultConfig {
        minSdk = libs.versions.androidMinimalSDK.get().toInt()
        targetSdk = libs.versions.androidTargetSDK.get().toInt()
        resValue("string", "app_name", appName)
        resValue("string", "scheme", appId)
    }
    buildTypes {
        debug {
            isDefault = true
        }
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
    }
    packaging {
        resources {
            excludes += "META-INF/versions/9/previous-compilation-data.bin"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}
