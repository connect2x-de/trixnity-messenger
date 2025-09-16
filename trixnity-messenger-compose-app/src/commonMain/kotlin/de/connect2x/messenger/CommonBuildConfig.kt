package de.connect2x.messenger

enum class Flavor { PROD, DEV }

interface CommonBuildConfig {
    val version: String
    val flavor: Flavor
    val appName: String
    val appId: String
    val licenses: String
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect") // This links to sources generated on demand
expect val BuildConfig: CommonBuildConfig
