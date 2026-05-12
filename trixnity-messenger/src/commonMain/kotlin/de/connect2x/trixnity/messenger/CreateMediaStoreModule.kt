package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.client.MediaStoreModule
import de.connect2x.trixnity.core.model.UserId
import org.koin.core.module.Module

fun interface CreateMediaStoreModule {
    suspend operator fun invoke(userId: UserId): MediaStoreModule
}

expect fun platformCreateMediaStoreModuleModule(): Module
