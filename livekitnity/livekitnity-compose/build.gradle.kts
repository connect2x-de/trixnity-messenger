@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(sharedLibs.plugins.c2xConventions)
  alias(sharedLibs.plugins.kotlin.multiplatform)
  alias(sharedLibs.plugins.android.library)
  alias(sharedLibs.plugins.compose.compiler)
  alias(libs.plugins.spotless)
}

android {
  namespace = "de.connect2x.livekitnity.compose"
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
      api(project(":livekitnity-api"))
      api(sharedLibs.compose.runtime)
      api(sharedLibs.compose.ui)
      api(sharedLibs.compose.foundation)
    }
    androidMain.dependencies {
      implementation(libs.livekit.android)
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
