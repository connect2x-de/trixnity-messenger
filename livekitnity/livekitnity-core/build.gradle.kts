@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(sharedLibs.plugins.kotlin.multiplatform)
  alias(sharedLibs.plugins.android.library)
  alias(libs.plugins.spotless)
}

android {
  namespace = "de.connect2x.livekitnity.core"
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
    configureEach {
      // TODO(fhilgers): Limit the amount of Js types spread around the codebase
      languageSettings.optIn("kotlin.js.ExperimentalWasmJsInterop")
    }
    commonMain.dependencies {
      api(project(":livekitnity-api"))
    }
    androidMain.dependencies {
      implementation(libs.livekit.android)
      implementation(libs.webrtcSdk.androidPrefixed)
      implementation(libs.protobuf.javalite)

      implementation(sharedLibs.androidx.activity)
    }
    webMain.dependencies {
      implementation(project(":livekitnity-bindings-js"))
      implementation(libs.kotlinx.browser)
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
