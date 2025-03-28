package de.connect2x.trixnity.messenger.settings

import kotlinx.serialization.json.Json

val SettingsJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}
