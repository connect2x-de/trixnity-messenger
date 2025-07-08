import co.touchlab.skie.configuration.DefaultArgumentInterop
import co.touchlab.skie.configuration.EnumInterop
import co.touchlab.skie.configuration.FlowInterop
import co.touchlab.skie.configuration.FunctionInterop
import co.touchlab.skie.configuration.SealedInterop
import co.touchlab.skie.configuration.SuspendInterop
import de.connect2x.conventions.isCI
import de.connect2x.conventions.registerCoverageTask
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.seskar)
    `maven-publish`
}

registerCoverageTask()

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    js {
        compilerOptions {
            sourceMap.set(true)
            sourceMapEmbedSources.set(JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS)
        }
        browser {
            commonWebpackConfig {
                showProgress = true
            }
            testTask {
                useKarma {
                    useFirefoxHeadless()
                }
            }
        }
        binaries.library()
        generateTypeScriptDefinitions()
    }
    applyDefaultHierarchyTemplate()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(npm("@zip.js/zip.js", libs.versions.zipjs.get()))
                implementation(project.dependencies.platform(libs.kotlin.wrappers.bom))
                api(libs.kotlin.browser)
            }
        }
    }
}
