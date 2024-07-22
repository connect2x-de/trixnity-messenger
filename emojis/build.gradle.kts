plugins {
    id("application")
    kotlin("jvm")
    kotlin("plugin.serialization") version libs.versions.kotlin.get() // for creation of Emojis.kt
}

val jvmTarget = libs.versions.kotlinJvmTarget.get().toInt()

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
