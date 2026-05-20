@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import de.connect2x.conventions.configureJava
import de.connect2x.conventions.defaultCompilerOptions
import de.connect2x.conventions.withAndroid
import de.connect2x.conventions.withBrowser
import de.connect2x.conventions.withIos
import de.connect2x.conventions.withJvm
import de.connect2x.conventions.withWeb
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform)
    alias(sharedLibs.plugins.android.library)
    alias(sharedLibs.plugins.compose.multiplatform)
    alias(sharedLibs.plugins.compose.compiler)
}

configureJava(sharedLibs.versions.targetJvm)

kotlin {
    withSourcesJar()
    defaultCompilerOptions()
    withJvm()
    withWeb { withBrowser() }
    withIos()
    withAndroid("$group.compose.view.typography.nunito", minSdk = libs.versions.minSdkVersion)

    sourceSets {
        commonMain.dependencies {
            api(projects.trixnityMessengerComposeView)
            implementation(sharedLibs.compose.resources)
        }
    }
}
