package de.connect2x.trixnity.messenger.internal.logic

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.viewmodel.initialsync.AccountSync
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal interface AccountSyncStatesLogic {
    fun accountSyncStates(): Flow<Map<UserId, AccountSync>>
}

internal fun AccountSyncStatesLogic(matrixClients: MatrixClients): AccountSyncStatesLogic {
    return AccountSyncStatesLogicImpl(matrixClients = matrixClients)
}

private class AccountSyncStatesLogicImpl(private val matrixClients: MatrixClients) : AccountSyncStatesLogic {

    override fun accountSyncStates(): Flow<Map<UserId, AccountSync>> {
        return accountSyncStates(matrixClients)
    }
}

private fun accountSyncStates(matrixClients: MatrixClients): Flow<Map<UserId, AccountSync>> {
    @OptIn(ExperimentalCoroutinesApi::class)
    return matrixClients.flatMapLatest(::accountSyncStates)
}

private fun accountSyncStates(matrixClients: Map<UserId, MatrixClient>): Flow<Map<UserId, AccountSync>> {
    if (matrixClients.isEmpty()) return flowOf(emptyMap())

    return combine(matrixClients.map(::accountSyncStates), Array<Pair<UserId, AccountSync>>::toMap)
}

private fun accountSyncStates(entry: Map.Entry<UserId, MatrixClient>): Flow<Pair<UserId, AccountSync>> {
    return entry.value.initialSyncDone
        .map { if (it) AccountSync.DONE else AccountSync.INITIAL_SYNC }
        .map { entry.key to it }
}
