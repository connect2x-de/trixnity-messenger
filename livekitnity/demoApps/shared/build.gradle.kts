@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(sharedLibs.plugins.c2xConventions)
  alias(sharedLibs.plugins.kotlin.multiplatform)
  alias(sharedLibs.plugins.android.library)
  alias(sharedLibs.plugins.compose.compiler)
  alias(sharedLibs.plugins.compose.multiplatform)
  alias(libs.plugins.spotless)
}

android {
  namespace = "de.connect2x.livekitnity.demo.shared"
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
      api(project(":livekitnity-compose"))

      api(sharedLibs.compose.runtime)
      implementation(sharedLibs.compose.ui)
      implementation(sharedLibs.compose.foundation)
      implementation(sharedLibs.compose.material3)
      implementation(sharedLibs.compose.materialIconsExtended)

      implementation(sharedLibs.androidx.lifecycleViewmodelCompose)
    }

    androidMain.dependencies {
      api(sharedLibs.androidx.activity)
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
