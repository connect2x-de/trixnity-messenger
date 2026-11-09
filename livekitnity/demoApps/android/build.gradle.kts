import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep

plugins {
  alias(sharedLibs.plugins.c2xConventions)
  alias(sharedLibs.plugins.android.application)
  alias(sharedLibs.plugins.compose.compiler)
  alias(libs.plugins.spotless)
}

android {
  namespace = "de.connect2x.livekitnity.demo.android"
  compileSdk = sharedLibs.versions.androidCompileSdk.get().toInt()
  defaultConfig {
    minSdk = sharedLibs.versions.androidMinSdk.get().toInt()
  }
}

dependencies {
  implementation(project(":livekitnity-api"))
  implementation(project(":livekitnity-core"))
  implementation(project(":livekitnity-compose"))
  implementation(project(":demoApps:shared"))

  implementation(sharedLibs.compose.runtime)
  implementation(sharedLibs.compose.ui)

  implementation(sharedLibs.androidx.activity)
  implementation(sharedLibs.androidx.activityCompose)
}

spotless {
  kotlin {
    ktfmt().metaStyle()
    target("src/*/kotlin/**/*.kt")
  }
  kotlinGradle {
    ktfmt().metaStyle()
  }
  format("xml") {
    target("src/**/*.xml")
    eclipseWtp(EclipseWtpFormatterStep.XML)
    trimTrailingWhitespace()
    endWithNewline()
  }
}
