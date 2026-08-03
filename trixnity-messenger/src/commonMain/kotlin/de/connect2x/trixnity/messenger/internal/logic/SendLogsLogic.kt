package de.connect2x.trixnity.messenger.internal.logic

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import de.connect2x.trixnity.messenger.internal.util.SendLogToDevsIfPossible
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName

internal interface SendLogsLogic {
    suspend fun sendLogs()

    suspend fun trySendLogs()
}

internal fun SendLogsLogic(
    matrixMessengerBaseConfiguration: MatrixMessengerBaseConfiguration,
    sendLogToDevsIfPossible: SendLogToDevsIfPossible,
    getDefaultDeviceDisplayName: GetDefaultDeviceDisplayName,
): SendLogsLogic {
    return SendLogsLogicImpl(
        matrixMessengerBaseConfiguration = matrixMessengerBaseConfiguration,
        sendLogToDevsIfPossible = sendLogToDevsIfPossible,
        getDefaultDeviceDisplayName = getDefaultDeviceDisplayName,
    )
}

private class SendLogsLogicImpl(
    private val matrixMessengerBaseConfiguration: MatrixMessengerBaseConfiguration,
    private val sendLogToDevsIfPossible: SendLogToDevsIfPossible,
    private val getDefaultDeviceDisplayName: GetDefaultDeviceDisplayName,
) : SendLogsLogic {

    override suspend fun sendLogs() {
        try {
            trySendLogs()
        } catch (exc: Throwable) {
            log.error(exc) { "Cannot send error report." }
        }
    }

    override suspend fun trySendLogs() {
        val emailAddress =
            matrixMessengerBaseConfiguration.sendLogsEmailAddress ?: return log.warn { "no sendLogsEmailAddress set" }

        log.debug { "send logs to devs (email: $emailAddress)" }

        sendLogToDevsIfPossible(emailAddress = emailAddress, subject = subject())
    }

    private fun subject(): String {
        // TODO include version of trixnity-messenger or maybe move sendLogs to client
        return "error report for $${matrixMessengerBaseConfiguration.appName} (${getDefaultDeviceDisplayName()})"
    }

    companion object {
        private val log = Logger("de.connect2x.trixnity.messenger.internal.logic.SendLogcLogic")
    }
}
