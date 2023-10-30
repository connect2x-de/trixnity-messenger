package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.cleanAccountName
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

internal fun getAppPath(accountName: String?): Path {
    val path = (
            NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory,
                NSUserDomainMask,
                true
            )[0] as String) // user directory
        .toPath().resolve(MessengerConfig.instance.appName) // /appName
        .let { if (accountName == null) it else it.resolve(accountName.cleanAccountName()) } // /accountName
    FileSystem.SYSTEM.createDirectories(path)
    return path
}

internal fun getDbPath(accountName: String) = getAppPath(accountName).resolve("database")