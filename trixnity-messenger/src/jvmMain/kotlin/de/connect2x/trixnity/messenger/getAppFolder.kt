package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.cleanAccountName
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path

private val log = KotlinLogging.logger { }

fun getAppFolder(accountName: String?): Path {
    val appName = MessengerConfig.instance.appName
    val cleanAccountName = accountName?.cleanAccountName()
    log.debug { "get app folder for $appName and account $accountName (decoded: $cleanAccountName)" }
    val path = when (getOs()) {
        OS.MAC_OS -> {
            Path.of(System.getenv("HOME")).resolve("Library").resolve("Application Support")
                .resolve(appName).let {
                    if (cleanAccountName == null) it
                    else it.resolve(cleanAccountName)
                }
        }

        OS.WINDOWS -> {
            Path.of(System.getenv("LOCALAPPDATA")).resolve(appName).let {
                if (cleanAccountName == null) it
                else it.resolve(cleanAccountName)
            }
        }

        OS.LINUX -> {
            Path.of(System.getenv("HOME")).resolve(".$appName").let {
                if (cleanAccountName == null) it
                else it.resolve(cleanAccountName)
            }
        }
    }

    Files.createDirectories(path)
    return path
}

internal fun getDbFolderPath(accountName: String): Path =
    getAppFolder(accountName).resolve("database")