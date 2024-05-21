package de.connect2x.trixnity.messenger.settings

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.serializer
import kotlin.reflect.KProperty

interface SettingsView<S : Settings<S>>

inline fun <S : Settings<S>, reified T : SettingsView<S>> Settings<S>.get(): T =
    settingsJson.decodeFromJsonElement(JsonObject(this))

inline fun <S : Settings<S>, reified T : SettingsView<S>> MutableSettings<S>.set(value: T) {
    val newValues = settingsJson.encodeToJsonElement(value) as? JsonObject
    if (newValues != null) putAll(newValues)
}

inline fun <S : Settings<S>, reified T : SettingsView<out S>> settingsView(): SettingsViewDelegate<S, T> =
    SettingsViewDelegate(serializer())

class SettingsViewDelegate<S : Settings<S>, T : SettingsView<out S>>(
    private val serializer: KSerializer<T>
) {
    operator fun getValue(settings: S, property: KProperty<*>): T =
        settingsJson.decodeFromJsonElement(serializer, JsonObject(settings))
}
