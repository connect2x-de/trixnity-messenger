package de.connect2x.trixnity.messenger

import kotlinx.browser.window
import kotlinx.serialization.encodeToString

inline fun <reified S : Any> createLocalStorageSettingsHolder(
    name: String,
    crossinline initialSettings: () -> S,
) = createSettingsHolder(object : SettingsStorage<S> {
    override suspend fun write(settings: S) {
        val json = settingsJson.encodeToString(settings)
        println("write ${json}")
        window.localStorage.setItem(name, json)
        println("wrote json")
    }

    override suspend fun read(): S {
        val json = window.localStorage.getItem(name) ?: return initialSettings()
        return settingsJson.decodeFromString<S>(json)
    }
})