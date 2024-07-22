plugins {
    id("application")
    kotlin("jvm")
    kotlin("plugin.serialization").version(libs.versions.kotlin.get()) // for creation of Emojis.kt
}

application {
    mainClass.set("EmojisKt")
}

dependencies {
    implementation(libs.kotlinx.serialization)
}

kotlin {
    jvmToolchain(libs.versions.kotlinJvmTarget.get().toInt())
}
