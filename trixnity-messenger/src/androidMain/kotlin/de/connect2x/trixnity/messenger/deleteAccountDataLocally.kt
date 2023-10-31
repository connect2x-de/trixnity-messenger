package de.connect2x.trixnity.messenger

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

actual suspend fun deleteAccountDataLocally(accountName: String) {
    withContext(Dispatchers.IO) {
        accountMutex.withLock {
            getAccountPath(accountName).deleteRecursively()
        }
    }
}