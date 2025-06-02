package de.connect2x.trixnity.messenger

import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module

fun interface CreateMediaStoreModule {
    suspend operator fun invoke(userId: UserId): Module
}

expect fun platformCreateMediaStoreModuleModule(): Module
