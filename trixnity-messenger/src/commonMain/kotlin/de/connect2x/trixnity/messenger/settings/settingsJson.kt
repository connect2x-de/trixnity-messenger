package de.connect2x.trixnity.messenger.settings

import kotlinx.serialization.json.Json

@PublishedApi
internal val settingsJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}
