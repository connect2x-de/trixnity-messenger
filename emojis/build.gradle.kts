plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

val jvmTarget = libs.versions.jvmTarget.get().toInt()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmTarget)
    }
    sourceCompatibility = JavaVersion.toVersion(jvmTarget)
    targetCompatibility = sourceCompatibility
}

kotlin {
    jvmToolchain(jvmTarget)
}

application {
    mainClass.set("EmojisKt")
}

dependencies {
    implementation(libs.kotlinx.serialization)
}
