package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.viewmodel.settings.PushMode


class MessengerConfig private constructor() {
    var appName: String = "Trixnity Messenger"
    var packageName: String = "de.connect2x"

    var encryptDb: Boolean = true

    var defaultPushMode: PushMode = PushMode.NONE

    var defaultHomeServer: String = ""

    var sendLogsEmailAddress: String? = null

    companion object { // no object since it would require @ThreadLocal for native
        val instance: MessengerConfig = MessengerConfig()
    }

    override fun toString(): String {
        return "MessengerConfig(appName='$appName', encryptDb=$encryptDb, defaultPushMode=$defaultPushMode, defaultHomeServer='$defaultHomeServer')"
    }
}
