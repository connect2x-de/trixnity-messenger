package de.connect2x.trixnity.messenger

import java.util.*

enum class OS(val value: String) {
    WINDOWS("Windows"), MAC_OS("macOS"), LINUX("Linux")
}

internal fun getOs(): OS {
    val os = System.getProperty("os.name", "generic").lowercase(Locale.ENGLISH)
    return when {
        os.contains("mac", ignoreCase = true) || os.contains("darwin", ignoreCase = true) -> OS.MAC_OS
        os.contains("win", ignoreCase = true) -> OS.WINDOWS
        os.contains("linux", ignoreCase = true) -> OS.LINUX
        else -> throw RuntimeException("os $os is not supported")
    }
}