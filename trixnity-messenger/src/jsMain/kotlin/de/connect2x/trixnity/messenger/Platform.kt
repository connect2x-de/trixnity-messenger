package de.connect2x.trixnity.messenger

import com.juul.indexeddb.deleteDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.browser.localStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.client.media.indexeddb.IndexedDBMediaStore
import net.folivo.trixnity.client.store.repository.indexeddb.createIndexedDBRepositoriesModule
import org.koin.core.module.Module

private val log = KotlinLogging.logger { }

@JsModule("@js-joda/timezone")
@JsNonModule
external object JsJodaTimeZoneModule

private val jsJodaTz = JsJodaTimeZoneModule

private object LocalAccountNames {
    private const val KEY = "accountNames"

    fun get() = localStorage.getItem(KEY)
        ?.let { Json.decodeFromString<List<String>>(it) }
        ?: emptyList()

    fun update(updater: (List<String>) -> List<String>) =
        localStorage.setItem(KEY, Json.encodeToString(updater(get())))
}

actual suspend fun createRepositoriesModule(accountName: String): Module {
    log.info { "create IndexDB store" }
    LocalAccountNames.update { it + accountName }
    return createIndexedDBRepositoriesModule(getDbName(accountName))
}

actual suspend fun getAccountNames(): List<String> = LocalAccountNames.get()

private fun getDbName(accountName: String) =
    "${MessengerConfig.instance.appName.replaceFirstChar { it.lowercase() }}-$accountName"

private fun getMediaStoreName(accountName: String) =
    getDbName(accountName) + "-media"

internal actual suspend fun createMediaStore(accountName: String): MediaStore =
    IndexedDBMediaStore(getMediaStoreName(accountName))

actual suspend fun deleteAccountDataLocally(accountName: String) {
    LocalAccountNames.update { it - accountName }
    deleteDatabase(getDbName(accountName))
    deleteDatabase(getMediaStoreName(accountName))
}

actual fun closeApp() {

}

actual fun isNetworkAvailable(): Boolean {
    return true
}

actual fun deviceDisplayName(): String {
    return "${MessengerConfig.instance.appName.replaceFirstChar { it.lowercase() }} (Browser)"
}

actual suspend fun getLogContent(): String {
    return ""
}

actual fun sendLogToDevs(emailAddress: String, subject: String, content: String) {

}