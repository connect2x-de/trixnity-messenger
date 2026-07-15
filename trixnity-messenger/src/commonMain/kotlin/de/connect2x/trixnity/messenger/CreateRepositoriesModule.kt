package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.client.RepositoriesModule
import de.connect2x.trixnity.core.model.UserId
import org.koin.core.module.Module

interface CreateRepositoriesModule {
    suspend fun generateDatabaseKey(): ByteArray?

    suspend fun create(userId: UserId, databaseKey: ByteArray?): RepositoriesModule

    suspend fun load(userId: UserId, databaseKey: ByteArray?): RepositoriesModule

    /**
     * This could check for platform- or db-specific exceptions and throw an appropriate subclass of
     * [MatrixClientInitializationException]. If nothing can be handled, do nothing and let generic exception handlers
     * do the work.
     */
    fun handleExceptions(exc: Exception)
}

internal expect fun platformCreateRepositoriesModuleModule(): Module
