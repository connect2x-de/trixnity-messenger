package de.connect2x.trixnity.messenger.notification

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.update
import de.connect2x.trixnity.messenger.update

open class PushNotificationProviderPushKeyUpdater(
    private val multiSettings: MatrixMultiMessengerSettingsHolder?,
    private val settings: MatrixMessengerSettingsHolder?,
) {
    companion object {
        private val log: Logger =
            Logger("de.connect2x.trixnity.messenger.notification.PushNotificationProviderPushKeyUpdater")
    }

    suspend fun onPushKeyUpdate(pushKey: String) {
        log.debug { "got pushKey update" }
        if (multiSettings != null) {
            multiSettings.update<MatrixMultiMessengerNotificationProviderPushSettings> {
                it.copy(pushKey = pushKey)
            }
        } else if (settings != null) {
            settings.update<MatrixMessengerNotificationProviderPushSettings> {
                it.copy(pushKey = pushKey)
            }
        }
    }

}
