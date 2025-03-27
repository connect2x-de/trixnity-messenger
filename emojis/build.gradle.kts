plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

application {
    mainClass = "EmojisKt"
}

dependencies {
    implementation(libs.kotlinx.serialization)
}
