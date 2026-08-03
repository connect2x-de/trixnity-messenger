package de.connect2x.trixnity.messenger.internal.logic

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.verification
import de.connect2x.trixnity.client.verification.VerificationService.SelfVerificationMethods
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClients
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal interface MatrixClientSelfVerificationMethodsLogic {
    fun selfVerificationMethods(): Flow<Map<UserId, SelfVerificationMethods>>
}

internal fun MatrixClientSelfVerificationMethodsLogic(
    matrixClients: MatrixClients
): MatrixClientSelfVerificationMethodsLogic {
    return MatrixClientSelfVerificationMethodsLogicImpl(matrixClients = matrixClients)
}

private class MatrixClientSelfVerificationMethodsLogicImpl(private val matrixClients: MatrixClients) :
    MatrixClientSelfVerificationMethodsLogic {
    override fun selfVerificationMethods(): Flow<Map<UserId, SelfVerificationMethods>> {
        return selfVerificationMethods(matrixClients)
    }
}

private fun selfVerificationMethods(matrixClients: MatrixClients): Flow<Map<UserId, SelfVerificationMethods>> {
    @OptIn(ExperimentalCoroutinesApi::class)
    return matrixClients.flatMapLatest(::selfVerificationMethods)
}

private fun selfVerificationMethods(
    matrixClients: Map<UserId, MatrixClient>
): Flow<Map<UserId, SelfVerificationMethods>> {
    if (matrixClients.isEmpty()) return flowOf(emptyMap())

    return combine(
        flows = matrixClients.map(::selfVerificationMethods),
        transform = Array<Pair<UserId, SelfVerificationMethods>>::toMap,
    )
}

private fun selfVerificationMethods(
    entry: Map.Entry<UserId, MatrixClient>
): Flow<Pair<UserId, SelfVerificationMethods>> {
    return entry.value.verification.getSelfVerificationMethods().map { entry.key to it }
}
