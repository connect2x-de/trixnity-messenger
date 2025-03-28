plugins {
    `kotlin-dsl`
}

fun Provider<PluginDependency>.asLibrary(): Provider<String> {
    return map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
}

dependencies {
    compileOnly(libs.plugins.kotlin.multiplatform.asLibrary())
    compileOnly(libs.plugins.kotlin.jvm.asLibrary())
    compileOnly(libs.plugins.kotlinx.kover.asLibrary())
    compileOnly(libs.plugins.android.library.asLibrary())
    compileOnly(libs.plugins.android.application.asLibrary())
}

gradlePlugin {
    plugins {
        register("c2x-conventions") {
            id = "c2x-conventions"
            implementationClass = "de.connect2x.conventions.C2XConventionsPlugin"
        }
    }
}
