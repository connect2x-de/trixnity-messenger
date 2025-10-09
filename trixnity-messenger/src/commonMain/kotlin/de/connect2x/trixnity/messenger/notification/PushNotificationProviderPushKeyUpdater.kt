package de.connect2x.trixnity.messenger.notification

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.update
import de.connect2x.trixnity.messenger.update
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

open class PushNotificationProviderPushKeyUpdater(
    private val multiSettings: MatrixMultiMessengerSettingsHolder?,
    private val settings: MatrixMessengerSettingsHolder?,
) {
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
