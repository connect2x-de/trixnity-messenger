package de.connect2x.trixnity.messenger.viewmodel.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.flow
import net.folivo.trixnity.utils.ByteArrayFlow

private val log = KotlinLogging.logger {}

// TODO: modify toByteArray in the Trixnity SDK instead
fun ByteArrayFlow.limitSize(maxSizeBytes: Long): ByteArrayFlow = flow {
    var size = 0
    collect { nextBytes ->
        size += nextBytes.size
        if (size > maxSizeBytes) throw MaxByteFlowSizeException(maxSizeBytes)
        else emit(nextBytes)
    }
}

class MaxByteFlowSizeException(val maxSizeBytes: Long) :
    IllegalStateException("byte flow is exceeding $maxSizeBytes bytes!")
