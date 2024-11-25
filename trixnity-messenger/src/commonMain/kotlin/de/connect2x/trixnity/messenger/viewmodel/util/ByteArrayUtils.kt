package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.flow.flow
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArray


// TODO: modify toByteArray in the Trixnity SDK instead
fun ByteArrayFlow.limitSize(maxSizeBytes: Long): ByteArrayFlow = flow {
    var size = 0
    collect { nextBytes ->
        size += nextBytes.size
        if (size > maxSizeBytes) throw MaxByteFlowSizeException(maxSizeBytes)
        else emit(nextBytes)
    }
}

suspend fun ByteArrayFlow.limitedByteArrayOrNull(
    maxSizeBytes: Long,
    knownSize: Long? = null,
    onError: ((e: Exception) -> Unit)? = null,
): ByteArray? {
    if (knownSize == null || knownSize <= maxSizeBytes) {
        return try {
            this.limitSize(maxSizeBytes).toByteArray()
        } catch (e: Exception) {
            if (e is MaxByteFlowSizeException || e.cause is MaxByteFlowSizeException) {
                onError?.let { it(e) }
            }
            else throw e
            null
        }
    } else {
        onError?.let { it(MaxByteFlowSizeException(maxSizeBytes)) }
        return null
    }
}

class MaxByteFlowSizeException(maxSizeBytes: Long) :
    IllegalStateException("byte flow is exceeding $maxSizeBytes bytes!")
