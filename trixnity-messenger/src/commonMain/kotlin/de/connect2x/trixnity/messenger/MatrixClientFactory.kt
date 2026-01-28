package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.client.CryptoDriverModule
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.MatrixClientConfiguration
import de.connect2x.trixnity.client.MediaStoreModule
import de.connect2x.trixnity.client.RepositoriesModule
import de.connect2x.trixnity.client.create
import de.connect2x.trixnity.clientserverapi.client.MatrixClientAuthProviderData
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
