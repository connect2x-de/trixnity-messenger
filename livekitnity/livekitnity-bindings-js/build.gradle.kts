@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(sharedLibs.plugins.c2xConventions)
  alias(sharedLibs.plugins.kotlin.multiplatform)
  alias(libs.plugins.spotless)
  idea
}

kotlin {
  explicitApi()

  js {
    useEsModules()
    browser()
  }
  wasmJs {
    useEsModules()
    browser()
  }

  compilerOptions {
    freeCompilerArgs.addAll(
        "-Xwarning-level=ERROR_SUPPRESSION:disabled",
    )
  }

  sourceSets {
    webMain.dependencies {
      implementation(npm("livekit-client", "2.22.4"))
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

idea {
  module {
    excludeDirs.add(file("generator"))
  }
}
