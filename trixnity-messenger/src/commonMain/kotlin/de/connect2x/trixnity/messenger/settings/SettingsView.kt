package de.connect2x.trixnity.messenger.settings

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import kotlin.reflect.KProperty

interface SettingsView<S : Settings<S>>

@OptIn(ExperimentalSerializationApi::class)
fun <S : Settings<S>, T : SettingsView<S>> Settings<S>.get(serializer: KSerializer<T>): T {
    val key = serializer.descriptor.annotations.filterIsInstance<NestedSettingsView>().firstOrNull()?.key
    val content = if (key != null) get(key) ?: JsonObject(emptyMap())
    else JsonObject(this)
    return SettingsJson.decodeFromJsonElement(serializer, content)
}

inline fun <S : Settings<S>, reified T : SettingsView<S>> Settings<S>.get(): T = get(serializer())

@OptIn(ExperimentalSerializationApi::class)
fun <S : Settings<S>, T : SettingsView<S>> MutableSettings<S>.set(value: T, serializer: KSerializer<T>) {
    val newValues = SettingsJson.encodeToJsonElement(serializer, value) as? JsonObject
    val key = serializer.descriptor.annotations.filterIsInstance<NestedSettingsView>().firstOrNull()?.key
    if (newValues != null) {
        if (key != null) {
            val oldValues = get(key) as? JsonObject
            put(key, JsonObject(buildMap {
                if (oldValues != null) putAll(oldValues)
                putAll(newValues)
            }))
        } else putAll(newValues)
    }
}

inline fun <S : Settings<S>, reified T : SettingsView<S>> MutableSettings<S>.set(value: T) = set(value, serializer())

inline fun <S : Settings<S>, reified T : SettingsView<S>> settingsView(): SettingsViewDelegate<S, T> =
    SettingsViewDelegate(serializer())

class SettingsViewDelegate<S : Settings<S>, T : SettingsView<S>>(
    private val serializer: KSerializer<T>
) {
    operator fun getValue(settings: S, property: KProperty<*>): T = settings.get(serializer)
}

@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS)
annotation class NestedSettingsView(val key: String)
