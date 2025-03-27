import com.mikepenz.aboutlibraries.plugin.AboutLibrariesTask
import de.connect2x.conventions.isCI
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
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
val licensesDir = layout.buildDirectory.dir("generated").get().dir("aboutLibraries").asFile

val licenses by tasks.registering(AboutLibrariesTask::class) {
    resultDirectory = licensesDir
    dependsOn("collectDependencies")
}

aboutLibraries {
    configPath = "trixnity-messenger-compose-app/license-config"
    // Disable this as it causes issues with a custom AboutLibrariesTask
    registerAndroidTasks = false
}

val buildConfigGenerator by tasks.registering {
    val licencesFile = licensesDir.resolve("aboutlibraries.json")
    val generatedSrc = layout.buildDirectory.dir("generated-src/kotlin/")
    inputs.file(licencesFile)
    doLast {
        val outputFile = generatedSrc.get()
            .dir(appId.replace(".", "/"))
            .file("BuildConfig.kt")
        val quotes = "\"\"\""
        val licencesString = licencesFile.readText()
            .replace("$", "\${'$'}")
            .replace(quotes, "")

        val buildConfigString =
            """
            package $appId
            
            object BuildConfig {
                const val version = "$version"
                val flavor = Flavor.valueOf("$buildFlavor")
                const val appName = "$appName"
                const val appId = "$appId"
                val licenses = $quotes$licencesString$quotes
            }
            
            enum class Flavor { PROD, DEV }
        """.trimIndent()
        outputFile.asFile.apply {
            ensureParentDirsCreated()
            createNewFile()
            writeText(buildConfigString)
        }
    }
    outputs.dirs(generatedSrc)
    dependsOn(licenses)
}

kotlin {
    androidTarget()
    jvm("desktop")
    js("web", IR) {
        browser {
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
            kotlin.srcDir(buildConfigGenerator.map { it.outputs })
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
                appResourcesRootDir.set(layout.buildDirectory)
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
            isMinifyEnabled = true
        }
    }
    packaging {
        resources {
            excludes += "META-INF/versions/9/previous-compilation-data.bin"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}
