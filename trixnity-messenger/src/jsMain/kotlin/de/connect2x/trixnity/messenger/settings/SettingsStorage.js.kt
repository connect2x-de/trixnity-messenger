package de.connect2x.trixnity.messenger.settings

import kotlinx.browser.window

class LocalStorageSettingsStorage(
    private val name: String,
) : SettingsStorage {
    override suspend fun write(settings: String) = window.localStorage.setItem(name, settings)
    override suspend fun read(): String? = window.localStorage.getItem(name)
}
