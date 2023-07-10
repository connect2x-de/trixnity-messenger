package de.connect2x.trixnity.messenger.viewmodel.settings

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import de.connect2x.trixnity.messenger.MessengerConfig
import de.connect2x.trixnity.messenger.getContext

actual fun createSettings(): com.russhwolf.settings.Settings {
    val appName = MessengerConfig.instance.appName
    val delegate = getContext().getSharedPreferences(
        appName,
        Context.MODE_PRIVATE
    )
    return SharedPreferencesSettings(delegate)
}