package de.connect2x.trixnity.messenger.internal.logic

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.verification
import de.connect2x.trixnity.client.verification.ActiveVerificationState
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClients
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal interface ActiveDeviceVerificationStatesLogic {
    fun activeDeviceVerifications(): Flow<Map<UserId, ActiveVerificationState?>>
}

internal fun ActiveDeviceVerificationStatesLogic(matrixClients: MatrixClients): ActiveDeviceVerificationStatesLogic {
    return ActiveDeviceVerificationStatesLogicImpl(matrixClients = matrixClients)
}

private class ActiveDeviceVerificationStatesLogicImpl(private val matrixClients: MatrixClients) :
    ActiveDeviceVerificationStatesLogic {
    override fun activeDeviceVerifications(): Flow<Map<UserId, ActiveVerificationState?>> {
        return activeDeviceVerifications(matrixClients = matrixClients)
    }
}

private fun activeDeviceVerifications(matrixClients: MatrixClients): Flow<Map<UserId, ActiveVerificationState?>> {
    @OptIn(ExperimentalCoroutinesApi::class)
    return matrixClients.flatMapLatest(::activeDeviceVerifications)
}

private fun activeDeviceVerifications(
    matrixClients: Map<UserId, MatrixClient>
): Flow<Map<UserId, ActiveVerificationState?>> {
    if (matrixClients.isEmpty()) return flowOf(emptyMap())

    return combine(matrixClients.map(::activeDeviceVerifications), Array<Pair<UserId, ActiveVerificationState?>>::toMap)
}

private fun activeDeviceVerifications(
    entry: Map.Entry<UserId, MatrixClient>
): Flow<Pair<UserId, ActiveVerificationState?>> {
    @OptIn(ExperimentalCoroutinesApi::class)
    return entry.value.verification.activeDeviceVerification
        .flatMapLatest { it?.state ?: flowOf(null) }
        .map { entry.key to it }
}
