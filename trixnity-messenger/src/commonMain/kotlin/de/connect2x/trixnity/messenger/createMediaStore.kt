package de.connect2x.trixnity.messenger

import net.folivo.trixnity.client.media.MediaStore

internal expect suspend fun createMediaStore(accountName: String): MediaStore
