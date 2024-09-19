package de.connect2x.messenger.desktop

import ch.qos.logback.core.PropertyDefinerBase
import de.connect2x.messenger.BuildConfig
import de.connect2x.messenger.Flavor
import de.connect2x.trixnity.messenger.util.getAppPath

class MessengerFolderPropertyDefiner : PropertyDefinerBase() {
    override fun getPropertyValue(): String =
        if (System.getenv("TRIXNITY_MESSENGER_ROOT_PATH") == null && BuildConfig.flavor == Flavor.DEV) {
            "./app-data"
        } else getAppPath(BuildConfig.appNameCleaned).toString()
}
