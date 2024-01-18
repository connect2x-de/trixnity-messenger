package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.SecretString
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module

interface CreateRepositoriesModule {
    suspend fun create(userId: UserId): CreateResult
    suspend fun load(userId: UserId, databasePassword: SecretString?): Module

    data class CreateResult(
        val module: Module,
        val databasePassword: SecretString?,
    )
}

internal expect fun platformCreateRepositoriesModuleModule(): Module