package de.connect2x.trixnity.messenger

import kotlinx.browser.localStorage
import kotlinx.serialization.encodeToString

inline fun <reified S : Any> createLocalStorageSettingsHolder(
    name: String,
    crossinline initialSettings: () -> S,
) = createSettingsHolder(object : SettingsStorage<S> {
    override suspend fun write(settings: S) {
        val json = settingsJson.encodeToString(settings)
        localStorage.setItem(name, json)
    }

    override suspend fun read(): S {
        val json = localStorage.getItem(name) ?: return initialSettings()
        return settingsJson.decodeFromString<S>(json)
    }
})