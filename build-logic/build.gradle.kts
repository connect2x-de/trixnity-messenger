import org.gradle.kotlin.dsl.`kotlin-dsl`
import de.connect2x.conventions.applyKtfmt


plugins {
    `kotlin-dsl`
    alias(sharedLibs.plugins.c2xConventions)
    alias(sharedLibs.plugins.ktfmt)
}

applyKtfmt()

repositories {
    mavenCentral()
    gradlePluginPortal()
}


gradlePlugin {
    plugins {
        register("uitestInfra") {
            id = "de.connect2x.uitest-infra"
            implementationClass = "UITestInfraPlugin"
        }
    }
}
