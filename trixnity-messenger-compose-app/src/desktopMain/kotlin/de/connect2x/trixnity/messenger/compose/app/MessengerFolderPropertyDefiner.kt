package de.connect2x.trixnity.messenger.compose.app

import ch.qos.logback.core.PropertyDefinerBase
import de.connect2x.trixnity.messenger.util.getAppPath

class MessengerFolderPropertyDefiner : PropertyDefinerBase() {
    override fun getPropertyValue(): String =
        if (System.getenv("TRIXNITY_MESSENGER_ROOT_PATH") == null && BuildConfig.flavor == Flavor.DEV) {
            "./app-data"
        } else getAppPath(BuildConfig.appId).toString()
}
