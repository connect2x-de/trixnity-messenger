package de.connect2x.trixnity.messenger

import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType

interface MatrixClientService {
    val matrixClients: StateFlow<List<NamedMatrixClient>>
    val scope: CoroutineScope?
    val notificationCount: StateFlow<Long>

    suspend fun login(
        baseUrl: Url,
        identifier: IdentifierType,
        password: String,
        initialDeviceDisplayName: String?,
        accountName: String,
    ): Result<Unit>

    suspend fun loginWithToken(
        baseUrl: Url,
        identifier: IdentifierType,
        token: String,
        initialDeviceDisplayName: String?,
        accountName: String,
    ): Result<Unit>

    suspend fun logout(accountName: String): Result<Unit>

    suspend fun initFromStore(
        accountName: String,
    ): Result<Boolean>

    fun destroy() {
        scope?.cancel()
    }
}