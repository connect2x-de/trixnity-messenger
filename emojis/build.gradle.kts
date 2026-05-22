import de.connect2x.conventions.configureJava

plugins {
    application
    alias(sharedLibs.plugins.kotlin.jvm)
    alias(sharedLibs.plugins.kotlin.serialization)
}

configureJava(sharedLibs.versions.targetJvm)

application { mainClass = "EmojisKt" }

dependencies {
    implementation(sharedLibs.kotlinx.serialization.core)
    implementation(sharedLibs.kotlinx.serialization.json)
}
