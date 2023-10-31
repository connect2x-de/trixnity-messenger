package de.connect2x.trixnity.messenger

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val log = KotlinLogging.logger { }
actual suspend fun deleteAccountDataLocally(accountName: String) {
    withContext(Dispatchers.IO) {
        accountMutex.withLock {
            val result = getAppFolder(accountName).toFile().deleteRecursively()
            log.debug { "deleted account folder for account $accountName: $result" }
        }
    }
}