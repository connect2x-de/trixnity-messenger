package de.connect2x.trixnity.messenger

import net.folivo.trixnity.client.RepositoriesModule
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module

interface CreateRepositoriesModule {
    suspend fun generateDatabaseKey(): ByteArray?
    suspend fun create(userId: UserId, databaseKey: ByteArray?): RepositoriesModule
    suspend fun load(userId: UserId, databaseKey: ByteArray?): RepositoriesModule
}

internal expect fun platformCreateRepositoriesModuleModule(): Module
