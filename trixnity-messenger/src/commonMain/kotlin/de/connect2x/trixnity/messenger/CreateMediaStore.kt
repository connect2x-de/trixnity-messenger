package de.connect2x.trixnity.messenger

import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module

fun interface CreateMediaStore {
    suspend operator fun invoke(userId: UserId): MediaStore
}

expect fun platformCreateMediaStoreModule(): Module