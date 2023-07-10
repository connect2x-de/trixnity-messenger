package de.connect2x.trixnity.messenger.viewmodel.settings

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

actual fun createSettings(): Settings {
    val delegate: Preferences = Preferences.userNodeForPackage(MessengerSettings::class.java)
    return PreferencesSettings(delegate)
}