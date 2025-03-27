dependencyResolutionManagement {
    // Suppress is okay because it is an incubating API, the suppression name just doesn't match
    @Suppress("UnstableApiUsage") repositories {
        google()
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
