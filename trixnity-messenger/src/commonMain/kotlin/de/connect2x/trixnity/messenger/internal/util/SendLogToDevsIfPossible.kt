package de.connect2x.trixnity.messenger.internal.util

import de.connect2x.trixnity.messenger.util.SendLogToDevs

internal fun interface SendLogToDevsIfPossible {
    suspend operator fun invoke(emailAddress: String, subject: String)
}

internal fun SendLogToDevsIfPossible(sendLogToDevs: SendLogToDevs?): SendLogToDevsIfPossible {
    return SendLogToDevsIfPossibleImpl(sendLogToDevs = sendLogToDevs)
}

private class SendLogToDevsIfPossibleImpl(private val sendLogToDevs: SendLogToDevs?) : SendLogToDevsIfPossible {
    override suspend fun invoke(emailAddress: String, subject: String) {
        sendLogToDevs?.invoke(emailAddress, subject)
    }
}
