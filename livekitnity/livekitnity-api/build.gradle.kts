@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(sharedLibs.plugins.c2xConventions)
  alias(sharedLibs.plugins.kotlin.multiplatform)
  alias(sharedLibs.plugins.android.library)
  alias(libs.plugins.spotless)
}

android {
  namespace = "de.connect2x.livekitnity.api"
  compileSdk = sharedLibs.versions.androidCompileSdk.get().toInt()
  defaultConfig {
    minSdk = sharedLibs.versions.androidMinSdk.get().toInt()
  }
}

kotlin {
  explicitApi()

  androidTarget()
  js {
    browser()
    useEsModules()
  }
  wasmJs {
    browser()
    useEsModules()
  }

  sourceSets {
    commonMain.dependencies {
      api(sharedLibs.kotlinx.coroutines.core)
    }
    androidMain.dependencies {
      api(libs.webrtcSdk.androidPrefixed)
    }
    webMain.dependencies {
      api(libs.kotlinx.browser)
    }
  }
}

spotless {
  kotlin {
    ktfmt().metaStyle()
    target("src/*/kotlin/**/*.kt")
  }
  kotlinGradle {
    ktfmt().metaStyle()
  }
}
