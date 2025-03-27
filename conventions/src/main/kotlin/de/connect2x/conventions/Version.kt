package de.connect2x.conventions

import org.gradle.api.provider.Provider
import java.time.Instant

fun withVersionSuffix(versionProvider: Provider<String>): String {
    val version = versionProvider.get()
    return when {
        isRelease -> {
            val commitTagVersion = System.getenv("CI_COMMIT_TAG").removePrefix("v")
            check(version == commitTagVersion.substringBefore("-")) {
                "version from code ($version) does not match commit tag version ($commitTagVersion)"
            }
            commitTagVersion
        }

        isCI -> {
            val commitEpoch = Instant.parse(System.getenv("CI_COMMIT_TIMESTAMP")).epochSecond
            val commitCustomEpoch = commitEpoch - 1704067200 // 01.01.2024
            "$version-DEV-$commitCustomEpoch"
        }

        else -> "$version-LOCAL"
    }
}
