package de.connect2x.trixnity.messenger

import com.juul.indexeddb.Database
import com.juul.indexeddb.Key
import com.juul.indexeddb.openDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.client.media.indexeddb.IndexedDBMediaStore
import net.folivo.trixnity.client.store.repository.indexeddb.createIndexedDBRepositoriesModule
import org.koin.core.module.Module

private val log = KotlinLogging.logger { }

@JsModule("@js-joda/timezone")
@JsNonModule
external object JsJodaTimeZoneModule

private val jsJodaTz = JsJodaTimeZoneModule

private const val accountNamesDbName = "__accountNames__"
private const val accountNamesStore = "accountNames"

actual suspend fun createRepositoriesModule(accountName: String): Module {
    log.info { "create IndexDB store" }
    getAccountNamesDb()
        .writeTransaction(accountNamesStore) {
            objectStore(accountNamesStore).add(accountName)
        }
    return createIndexedDBRepositoriesModule(getDbName(accountName))
}

actual suspend fun getAccountNames(): List<String> = withContext(Dispatchers.Default) {
    (getAccountNamesDb()
        .transaction(accountNamesStore) {
            objectStore(accountNamesStore).getAll()
        }
            ).toList()
}

private fun getDbName(accountName: String) =
    "${MessengerConfig.instance.appName.replaceFirstChar { it.lowercase() }}-$accountName"

internal actual suspend fun createMediaStore(accountName: String): MediaStore {
    return IndexedDBMediaStore("timmy_media")
}

private suspend fun getAccountNamesDb(): Database {
    return openDatabase(accountNamesDbName, 1) { database, oldVersion, newVersion ->
        if (oldVersion < 1) {
            database.createObjectStore(accountNamesStore)
        }
    }
}

actual suspend fun deleteDatabase(accountName: String) {

}

actual suspend fun deleteAccountDataLocally(accountName: String) {
    getAccountNamesDb().writeTransaction(accountNamesStore) {
        objectStore(accountNamesStore).delete(Key(accountName))
    }
}

actual fun closeApp() {

}

actual fun isNetworkAvailable(): Boolean {
    return true
}

actual fun deviceDisplayName(): String {
    return "Browser"
}

actual suspend fun getLogContent(): String {
    return ""
}

actual fun sendLogToDevs(emailAddress: String, subject: String, content: String) {

}