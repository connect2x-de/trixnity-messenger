package de.connect2x.trixnity.messenger.settings

import kotlinx.serialization.json.JsonElement

interface Settings<S : Settings<S>> : Map<String, JsonElement>
interface MutableSettings<S : Settings<S>> : MutableMap<String, JsonElement>

abstract class SettingsImpl<S : Settings<S>>(
    delegate: Map<String, JsonElement>
) : Settings<S>, Map<String, JsonElement> by delegate

data class MutableSettingsImpl<S : Settings<S>>(
    private val delegate: S,
) : MutableSettings<S>, MutableMap<String, JsonElement> by delegate.toMutableMap()
