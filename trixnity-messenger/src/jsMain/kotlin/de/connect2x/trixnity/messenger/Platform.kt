package de.connect2x.trixnity.messenger

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

actual suspend fun createRepositoriesModule(context: Any?, accountName: String): Module {
    log.info { "create in memory store" }
    return createIndexedDBRepositoriesModule("timmy")
}

internal actual suspend fun createMediaStore(context: Any?, accountName: String): MediaStore {
    return IndexedDBMediaStore("timmy_media")
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