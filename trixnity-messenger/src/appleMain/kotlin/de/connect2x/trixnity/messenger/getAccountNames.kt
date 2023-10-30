package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.getAccountName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem

internal actual suspend fun getAccountNames(): List<String> =
    withContext(Dispatchers.IO) {
        accountMutex.withLock {
            FileSystem.SYSTEM.list(getAppPath(accountName = null)).map { it.name.getAccountName() }
        }
    }