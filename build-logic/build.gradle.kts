import de.connect2x.conventions.applyKtfmt
import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
    `kotlin-dsl`
    alias(sharedLibs.plugins.c2xConventions)
    alias(sharedLibs.plugins.ktfmt)
}

applyKtfmt()

gradlePlugin {
    plugins {
        register("uitestInfra") {
            id = "de.connect2x.uitest-infra"
            implementationClass = "UITestInfraPlugin"
        }
    }
}
