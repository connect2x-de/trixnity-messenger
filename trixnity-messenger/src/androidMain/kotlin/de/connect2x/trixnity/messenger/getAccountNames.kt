package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.getAccountName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal actual suspend fun getAccountNames(): List<String> = withContext(Dispatchers.IO) {
    accountMutex.withLock {
        getAppFolder()
            .list { file, _ -> file.isDirectory }
            ?.map { it.getAccountName() }
            ?: emptyList()
    }
}
