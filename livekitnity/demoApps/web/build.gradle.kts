@file:OptIn(ExperimentalWasmDsl::class)

import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(sharedLibs.plugins.c2xConventions)
  alias(sharedLibs.plugins.kotlin.multiplatform)
  alias(sharedLibs.plugins.compose.compiler)
  alias(sharedLibs.plugins.compose.multiplatform)
  alias(libs.plugins.spotless)
}

kotlin {
  explicitApi()

  js {
    browser()
    useEsModules()
    binaries.executable()
  }
  wasmJs {
    browser()
    useEsModules()
    binaries.executable()
  }

  sourceSets {
    webMain.dependencies {
      implementation(project(":livekitnity-api"))
      implementation(project(":livekitnity-core"))
      implementation(project(":livekitnity-compose"))
      implementation(project(":demoApps:shared"))

      implementation(sharedLibs.compose.runtime)
      implementation(sharedLibs.compose.ui)
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
  format("html") {
    target("src/**/*.html")
    eclipseWtp(EclipseWtpFormatterStep.HTML)
    trimTrailingWhitespace()
    endWithNewline()
  }
  format("css") {
    target("src/**/*.css")
    eclipseWtp(EclipseWtpFormatterStep.CSS)
    trimTrailingWhitespace()
    endWithNewline()
  }
}
