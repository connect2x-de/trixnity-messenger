package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.cleanAccountName
import de.connect2x.trixnity.messenger.util.getAccountName
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.client.media.okio.OkioMediaStore
import net.folivo.trixnity.client.store.repository.realm.createRealmRepositoriesModule
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

private val log = KotlinLogging.logger { }

private val accountMutex = Mutex()

actual suspend fun createRepositoriesModule(accountName: String): Module = withContext(Dispatchers.IO) {
    createRealmRepositoriesModule {
        val databasePath = getDbPath(accountName).toString()
        log.debug { "database path: $databasePath" }
        directory(databasePath)
    }
}

internal actual suspend fun createMediaStore(accountName: String): MediaStore =
    withContext(Dispatchers.IO) {
        val mediaPath = getAppPath(accountName).resolve("media")
        val mediaStore = OkioMediaStore(mediaPath)
        log.debug { "media store path: $mediaPath" }
        mediaStore
    }

actual suspend fun deleteAccountDataLocally(accountName: String) = withContext(Dispatchers.IO) {
    accountMutex.withLock {
        FileSystem.SYSTEM.deleteRecursively(getAppPath(accountName), mustExist = false)
    }
}

actual suspend fun getAccountNames(): List<String> =
    withContext(Dispatchers.IO) {
        accountMutex.withLock {
            FileSystem.SYSTEM.list(getAppPath(accountName = null)).map { it.name.getAccountName() }
        }
    }

private fun getAppPath(accountName: String?): Path {
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

private fun getDbPath(accountName: String) = getAppPath(accountName).resolve("database")

actual fun closeApp() {
}

actual fun isNetworkAvailable(): Boolean {
    return true
}

actual fun deviceDisplayName(): String {
    return "iOS"
}

actual suspend fun getLogContent(): String {
    return ""
}

actual fun sendLogToDevs(emailAddress: String, subject: String, content: String) {

}