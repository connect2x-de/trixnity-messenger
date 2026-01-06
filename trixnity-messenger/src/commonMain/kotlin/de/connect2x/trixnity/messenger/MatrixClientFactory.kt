package de.connect2x.trixnity.messenger

import net.folivo.trixnity.client.CryptoDriverModule
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.MatrixClientConfiguration
import net.folivo.trixnity.client.MediaStoreModule
import net.folivo.trixnity.client.RepositoriesModule
import net.folivo.trixnity.client.create
import net.folivo.trixnity.clientserverapi.client.MatrixClientAuthProviderData
import kotlin.coroutines.CoroutineContext

interface MatrixClientFactory {
    suspend fun create(
        repositoriesModule: RepositoriesModule,
        mediaStoreModule: MediaStoreModule,
        cryptoDriverModule: CryptoDriverModule,
        authProviderData: MatrixClientAuthProviderData?,
        coroutineContext: CoroutineContext,
        configuration: MatrixClientConfiguration.() -> Unit,
    ): Result<MatrixClient> = MatrixClient.create(
        repositoriesModule = repositoriesModule,
        mediaStoreModule = mediaStoreModule,
        cryptoDriverModule = cryptoDriverModule,
        authProviderData = authProviderData,
        coroutineContext = coroutineContext,
        configuration = configuration,
    )

    companion object : MatrixClientFactory
}
