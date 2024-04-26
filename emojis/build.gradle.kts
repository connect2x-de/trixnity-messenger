plugins {
    id("application")
    kotlin("jvm")
    kotlin("plugin.serialization").version(libs.versions.kotlin.get()) // for creation of Emojis.kt
}

application {
    mainClass.set("EmojisKt")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
}

kotlin {
    jvmToolchain(libs.versions.kotlinJvmTarget.get().toInt())
}
