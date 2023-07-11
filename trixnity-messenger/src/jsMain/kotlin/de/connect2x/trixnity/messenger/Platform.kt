package de.connect2x.trixnity.messenger

import com.juul.indexeddb.Database
import com.juul.indexeddb.openDatabase
import de.connect2x.trixnity.messenger.viewmodel.util.runBlocking
import io.github.oshai.kotlinlogging.KotlinLogging
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

actual fun getAccountNames(): List<String> {
    return runBlocking {
        getAccountNamesDb()
            .transaction(accountNamesStore) {
                objectStore(accountNamesStore).getAll()
            }
    } as List<String>
}

private fun getDbName(accountName: String) =
    "${MessengerConfig.instance.appName.replaceFirstChar { it.lowercase() }}-$accountName"

internal actual suspend fun createMediaStore(context: Any?, accountName: String): MediaStore {
    return IndexedDBMediaStore("timmy_media")
}

private suspend fun getAccountNamesDb(): Database {
    return openDatabase(accountNamesDbName, 1) { database, oldVersion, newVersion ->
        if (oldVersion < 1) {
            database.createObjectStore(accountNamesStore)
        }
    }
}

actual fun deleteDatabase(accountName: String) {

}

actual fun closeApp() {

}

actual fun getVersion(): String {
    return "0.0.1"
}

actual fun getLicenses(): String {
    return "Licenses"
}

actual fun isNetworkAvailable(): Boolean {
    return true
}

actual fun deviceDisplayName(): String {
    return "Browser"
}

actual fun getLogContent(): String {
    return ""
}

actual fun sendLogToDevs(emailAddress: String, subject: String, content: String) {

}