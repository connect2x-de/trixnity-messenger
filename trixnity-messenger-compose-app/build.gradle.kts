
import com.mikepenz.aboutlibraries.plugin.AboutLibrariesTask
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated

plugins {
    kotlin("multiplatform")
    id("com.android.application")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.aboutlibraries.plugin)
    // TODO active when you want to use google-services for notifications (needs google-services.json)   id("com.google.gms.google-services")
}

val version = libs.versions.messenger.get()
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

tasks.named("prepareKotlinIdeaImport") {
    val prepareKotlinIdeaImport = this
    kotlin.sourceSets.all {
        prepareKotlinIdeaImport.dependsOn(kotlin)
    }
}

kotlin {
    val kotlinJvmTarget = libs.versions.jvmTarget.get()
    androidTarget()
    jvmToolchain(JavaLanguageVersion.of(kotlinJvmTarget).asInt())
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = kotlinJvmTarget
        }
    }
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
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
        commonMain {
            dependencies {
                implementation(project(":trixnity-messenger-compose-view"))
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
            jvmArgs(
//            "-Dapple.awt.application.appearance=system",
                "-Xmx1G",
                "-XX:+HeapDumpOnOutOfMemoryError",
            )

            buildTypes.release.proguard {
                isEnabled = false
            }
            nativeDistributions {
                modules("java.net.http", "java.sql", "java.naming")
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                appResourcesRootDir.set(layout.buildDirectory) // @see https://github.com/JetBrains/compose-jb/tree/master/tutorials/Native_distributions_and_local_execution#jvm-resource-loading
                packageName = appId
                packageVersion = libs.versions.messenger.get()

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
    buildFeatures {
        compose = true
    }
    compileSdk = libs.versions.androidCompileSDK.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinimalSDK.get().toInt()
        targetSdk = libs.versions.androidTargetSDK.get().toInt()
        resValue("string", "app_name", appName)
        resValue("string", "scheme", appId)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
    }
    buildTypes {
        release {
            isDefault = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    packaging {
        resources {
            excludes += "META-INF/versions/9/previous-compilation-data.bin"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}
