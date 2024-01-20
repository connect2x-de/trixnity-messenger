package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.SecretByteArray
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module

interface CreateRepositoriesModule {
    suspend fun create(userId: UserId): CreateResult
    suspend fun load(userId: UserId, databasePassword: SecretByteArray?): Module

    data class CreateResult(
        val module: Module,
        val databasePassword: SecretByteArray?,
    )
}

internal expect fun platformCreateRepositoriesModuleModule(): Module