package de.connect2x.trixnity.messenger

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.client.media.okio.OkioMediaStore
import okio.Path.Companion.toOkioPath

internal actual suspend fun createMediaStore(accountName: String): MediaStore =
    withContext(Dispatchers.IO) {
        OkioMediaStore(getAccountPath(accountName).resolve("media").toOkioPath())
    }