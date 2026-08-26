package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.clientserverapi.model.user.ProfileField
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClients
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

interface GetAccountProfileDisplayName {
    fun fromUserId(userId: UserId): Flow<String>

    fun fromMatrixClient(matrixClient: MatrixClient): Flow<String>
}

@OptIn(ExperimentalCoroutinesApi::class)
class GetAccountProfileDisplayNameImpl(private val matrixClients: MatrixClients) : GetAccountProfileDisplayName {
    override fun fromUserId(userId: UserId): Flow<String> =
        matrixClients.mapNotNull { it[userId] }.flatMapLatest { fromMatrixClient(it) }

    override fun fromMatrixClient(matrixClient: MatrixClient): Flow<String> =
        matrixClient.profile.map { it?.get(ProfileField.DisplayName)?.value ?: "" }
}
