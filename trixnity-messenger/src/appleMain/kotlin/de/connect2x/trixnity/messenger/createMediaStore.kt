package de.connect2x.trixnity.messenger

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.client.media.okio.OkioMediaStore

private val log = KotlinLogging.logger { }
internal actual suspend fun createMediaStore(accountName: String): MediaStore =
    withContext(Dispatchers.IO) {
        val mediaPath = getAppPath(accountName).resolve("media")
        val mediaStore = OkioMediaStore(mediaPath)
        log.debug { "media store path: $mediaPath" }
        mediaStore
    }