package de.connect2x.trixnity.messenger

import io.github.oshai.kotlinlogging.KotlinLogging
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.client.media.indexeddb.IndexedDBMediaStore

private val log = KotlinLogging.logger { }
internal actual suspend fun createMediaStore(accountName: String): MediaStore {
    log.info { "create IndexedDBMediaStore" }
    return IndexedDBMediaStore(getMediaStoreName(accountName))
}