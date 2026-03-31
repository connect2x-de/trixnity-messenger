import org.gradle.kotlin.dsl.`kotlin-dsl`


plugins {
    `kotlin-dsl`
}

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
