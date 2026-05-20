import de.connect2x.conventions.defaultCompilerOptions
import de.connect2x.conventions.withBrowser
import de.connect2x.conventions.withWeb

plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform)
    alias(libs.plugins.seskar)
}

kotlin {
    withSourcesJar()
    defaultCompilerOptions()
    withWeb {
        withBrowser { commonWebpackConfig { showProgress = true } }
        useEsModules()
        binaries.library()
        generateTypeScriptDefinitions()
    }
    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain {
            dependencies {
                api(npm("pdfjs-dist", libs.versions.pdfjs.get()))
                implementation(project.dependencies.platform(sharedLibs.kotlin.wrappers.bom))
                api(sharedLibs.kotlin.browser)
            }
        }
    }
}
