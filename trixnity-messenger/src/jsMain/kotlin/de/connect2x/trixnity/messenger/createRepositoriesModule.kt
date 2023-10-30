package de.connect2x.trixnity.messenger

import io.github.oshai.kotlinlogging.KotlinLogging
import net.folivo.trixnity.client.store.repository.indexeddb.createIndexedDBRepositoriesModule
import org.koin.core.module.Module

private val log = KotlinLogging.logger { }
actual suspend fun createRepositoriesModule(accountName: String, dbPassword: DbPassword): Module {
    log.info { "createIndexedDBRepositoriesModule" }
    LocalAccountNames.update { it + accountName }
    return createIndexedDBRepositoriesModule(getDbName(accountName))
}