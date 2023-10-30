package de.connect2x.trixnity.messenger

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem

actual suspend fun deleteAccountDataLocally(accountName: String) = withContext(Dispatchers.IO) {
    accountMutex.withLock {
        FileSystem.SYSTEM.deleteRecursively(getAppPath(accountName), mustExist = false)
    }
}