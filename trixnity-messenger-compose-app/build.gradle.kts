import de.connect2x.conventions.CI
import de.connect2x.conventions.configureJava
import de.connect2x.conventions.defaultCompilerOptions
import de.connect2x.conventions.registerMultiplatformLicensesTasks
import de.connect2x.conventions.withAndroid
import de.connect2x.conventions.withBrowser
import de.connect2x.conventions.withIos
import de.connect2x.conventions.withJs
import de.connect2x.conventions.withJvm
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated

plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform)
    alias(sharedLibs.plugins.android.application)
    alias(sharedLibs.plugins.compose.multiplatform)
    alias(sharedLibs.plugins.compose.compiler)
    alias(sharedLibs.plugins.aboutLibraries.plugin)
    alias(sharedLibs.plugins.google.services)
}

configureJava(sharedLibs.versions.targetJvm)

val appVersion = "1.0.0"
val appName = "Trixnity Messenger"
val appId = "de.connect2x.trixnity.messenger.compose.app"

enum class BuildFlavor { PROD, DEV }

val buildFlavor = BuildFlavor.valueOf(
    project.properties["tm_build_flavor"] as? String
        ?: System.getenv("TM_BUILD_FLAVOR")
        ?: if (CI.isCI) "PROD" else "DEV"
)

registerMultiplatformLicensesTasks { licenseTask, target, variant ->
    // TODO: move this into c2x-conventions eventually
    val targetName = target.targetName
    val buildConfigTask = tasks.register("generateBuildConfig${targetName.capitalized()}${variant.capitalized()}") {
        dependsOn(licenseTask)
        group = "build config"
        inputs.property("tm_build_flavor", buildFlavor)
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
                override val version: String = "$appVersion"
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

kotlin {
    defaultCompilerOptions()
    withAndroid("$group.compose.app", minSdk = libs.versions.minSdkVersion)
    withJvm()
    withJs {
        withBrowser {
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
        useEsModules()
    }
    withIos {
        binaries.framework {
            export(sharedLibs.decompose)
            export(sharedLibs.essenty.lifecycle)
            export(projects.trixnityMessengerComposeView)
            baseName = "TrixnityMessengerUI"
            isStatic = true
        }
    }
    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain {
            dependencies {
                api(projects.trixnityMessengerComposeView) // api because of iOS
                implementation(sharedLibs.compose.resources)
                implementation(sharedLibs.lognity.core)
                implementation(sharedLibs.lognity.config)
                implementation(sharedLibs.lognity.core.config)
                api(sharedLibs.decompose) // needed for export to iOS
                api(sharedLibs.essenty.lifecycle) // needed for export to iOS
            }
        }
        jvmMain {
            dependencies {
                // this is needed to create lock files working on all machines
                if (System.getProperty("bundleAll") == "true") {
                    implementation(sharedLibs.compose.desktop.linuxX64)
                    implementation(sharedLibs.compose.desktop.linuxArm64)
                    implementation(sharedLibs.compose.desktop.windowsX64)
                    implementation(sharedLibs.compose.desktop.macosX64)
                    implementation(sharedLibs.compose.desktop.macosArm64)
                } else {
                    implementation(compose.desktop.currentOs)
                }
                implementation(sharedLibs.kotlinx.coroutines.swing)
            }
        }
        androidMain {
            dependencies {
                implementation(projects.trixnityMessenger.trixnityMessengerNotificationFcm)
                implementation(projects.trixnityMessenger.trixnityMessengerNotificationUnifiedpush)
                implementation(sharedLibs.compose.uiTooling)
                implementation(sharedLibs.androidx.appcompat)
                implementation(sharedLibs.androidx.work.runtime.ktx)
                implementation(sharedLibs.androidx.activity.compose)
            }
        }
        iosMain {
            dependencies {
                implementation(projects.trixnityMessenger.trixnityMessengerNotificationApns)
            }
        }
        webMain {
            dependencies {
                implementation(npm("copy-webpack-plugin", libs.versions.copyWebpackPlugin.get()))
            }
        }
    }
}

val distributionJavaHome: String = System.getenv("DIST_JAVA_HOME") ?: javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(sharedLibs.versions.distributionJvm.get().toInt())
    vendor = JvmVendorSpec.ADOPTIUM
}.get().metadata.installationPath.asFile.absolutePath

compose {
    desktop {
        application {
            mainClass = "$appId.Main"
            jvmArgs("-Xmx1G", "-XX:+HeapDumpOnOutOfMemoryError")
            javaHome = distributionJavaHome
            buildTypes.release.proguard {
                isEnabled = false
            }
            nativeDistributions {
                modules("java.net.http", "java.sql", "java.naming")
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                // @see https://github.com/JetBrains/compose-jb/tree/master/tutorials/Native_distributions_and_local_execution#jvm-resource-loading
                appResourcesRootDir = layout.buildDirectory
                packageName = appId
                packageVersion = appVersion

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
    defaultConfig {
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
