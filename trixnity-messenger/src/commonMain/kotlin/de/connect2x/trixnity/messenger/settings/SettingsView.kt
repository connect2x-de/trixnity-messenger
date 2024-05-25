package de.connect2x.trixnity.messenger.settings

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import kotlin.reflect.KProperty

interface SettingsView<S : Settings<S>>

fun <S : Settings<S>, T : SettingsView<S>> Settings<S>.get(serializer: KSerializer<T>): T =
    settingsJson.decodeFromJsonElement(serializer, JsonObject(this))

inline fun <S : Settings<S>, reified T : SettingsView<S>> Settings<S>.get(): T = get(serializer())

fun <S : Settings<S>, T : SettingsView<S>> MutableSettings<S>.set(value: T, serializer: KSerializer<T>) {
    val newValues = settingsJson.encodeToJsonElement(serializer, value) as? JsonObject
    if (newValues != null) putAll(newValues) // TODO merge?
}

inline fun <S : Settings<S>, reified T : SettingsView<S>> MutableSettings<S>.set(value: T) = set(value, serializer())

inline fun <S : Settings<S>, reified T : SettingsView<out S>> settingsView(): SettingsViewDelegate<S, T> =
    SettingsViewDelegate(serializer())

class SettingsViewDelegate<S : Settings<S>, T : SettingsView<out S>>(
    private val serializer: KSerializer<T>
) {
    operator fun getValue(settings: S, property: KProperty<*>): T =
        settingsJson.decodeFromJsonElement(serializer, JsonObject(settings))
}
