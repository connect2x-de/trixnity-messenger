import de.connect2x.conventions.defaultCompilerOptions
import de.connect2x.conventions.withBrowser
import de.connect2x.conventions.withJs

plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform)
    alias(sharedLibs.plugins.dokka)
    alias(libs.plugins.seskar)
    `maven-publish`
}

kotlin {
    withSourcesJar()
    defaultCompilerOptions()
    withJs {
        withBrowser {
            commonWebpackConfig {
                showProgress = true
            }
        }
        useEsModules()
        binaries.library()
        generateTypeScriptDefinitions()
    }
    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain {
            dependencies {
                api(npm("@zip.js/zip.js", libs.versions.zipjs.get()))
                implementation(project.dependencies.platform(sharedLibs.kotlin.wrappers.bom))
                api(sharedLibs.kotlin.browser)
            }
        }
    }
}
