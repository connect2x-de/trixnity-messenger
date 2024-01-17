package de.connect2x.trixnity.messenger

import kotlinx.browser.localStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

inline fun <reified S : Any> createLocalStorageSettingsHolder(
    name: String,
    crossinline initialSettings: () -> S,
) = createSettingsHolder(object : SettingsStorage<S> {
    override suspend fun write(settings: S) {
        val json = Json.encodeToString(settings)
        localStorage.setItem(name, json)
    }

    override suspend fun read(): S {
        val json = localStorage.getItem(name) ?: return initialSettings()
        return Json.decodeFromString<S>(json)
    }
})