package de.connect2x.trixnity.messenger

import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.model.UserId

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

    suspend fun loginWith(
        baseUrl: Url,
        userId: UserId,
        deviceId: String,
        accessToken: String,
        displayName: String?,
        avatarUrl: String? = null,
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