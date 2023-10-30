package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.getAccountName
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

private val log = KotlinLogging.logger { }
actual suspend fun getAccountNames(): List<String> = withContext(Dispatchers.IO) {
    accountMutex.withLock {
        getAppFolder(accountName = null)
            .listDirectoryEntries()
            .filter { it.isDirectory() }
            .map {
                log.debug { "account encoded: ${it.name}, decoded: ${it.name.getAccountName()}" }
                it.name.getAccountName()
            }
    }
}