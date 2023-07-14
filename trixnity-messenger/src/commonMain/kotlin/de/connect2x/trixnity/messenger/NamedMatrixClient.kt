package de.connect2x.trixnity.messenger

import kotlinx.coroutines.flow.MutableStateFlow
import net.folivo.trixnity.client.MatrixClient

data class NamedMatrixClient(
    val accountName: String,
    val matrixClient: MutableStateFlow<MatrixClient?>,
) {
    override fun hashCode(): Int {
        return accountName.hashCode() + matrixClient.value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is NamedMatrixClient) {
            accountName == other.accountName &&
                    matrixClient.value == other.matrixClient.value
        } else false
    }

    override fun toString(): String {
        return "NamedMatrixClient(accountName=$accountName, matrixClient(value)=${matrixClient.value})"
    }
}

fun NamedMatrixClient.matrixClientOrThrow(): MatrixClient =
    matrixClient.value
        ?: throw IllegalStateException("cannot find MatrixClient for account $accountName")

