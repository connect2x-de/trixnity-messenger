package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.cleanAccountName
import de.connect2x.trixnity.messenger.util.getAccountName
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.client.media.okio.OkioMediaStore
import net.folivo.trixnity.client.store.repository.realm.createRealmRepositoriesModule
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import platform.Foundation.*

private val log = KotlinLogging.logger { }

private val accountMutex = Mutex()

actual suspend fun createRepositoriesModule(accountName: String): Module = withContext(Dispatchers.IO) {
    createRealmRepositoriesModule {
        directory(getDbPath(accountName).toString())
    }
}

internal actual suspend fun createMediaStore(accountName: String): MediaStore =
    withContext(Dispatchers.IO) {
        val mediaStore = OkioMediaStore(getAppPath().resolve("media"))
        log.debug { "media store location: $mediaStore" }
        mediaStore
    }

actual suspend fun deleteDatabase(accountName: String) = withContext(Dispatchers.IO) {
    accountMutex.withLock {
        FileSystem.SYSTEM.deleteRecursively(getDbPath(accountName), mustExist = false)
    }
}

actual suspend fun deleteAccountDataLocally(accountName: String) = withContext(Dispatchers.IO) {
    accountMutex.withLock {
        FileSystem.SYSTEM.deleteRecursively(getAppPath().resolve(accountName), mustExist = false)
    }
}

actual suspend fun getAccountNames(): List<String> =
    withContext(Dispatchers.IO) {
        accountMutex.withLock {
            FileSystem.SYSTEM.list(getAppPath()).map { it.name.getAccountName() }
        }
    }

private fun getAppPath() = NSBundle.mainBundle.bundlePath.toPath()
private fun getDbPath(accountName: String) = getAppPath().resolve(accountName.cleanAccountName())

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