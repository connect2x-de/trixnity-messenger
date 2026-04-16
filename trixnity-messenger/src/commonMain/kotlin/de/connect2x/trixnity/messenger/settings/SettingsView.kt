package de.connect2x.trixnity.messenger.settings

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import kotlin.reflect.KProperty

interface SettingsView<S : Settings<S>>

fun <S : Settings<S>, T : SettingsView<S>> Settings<S>.getValues(serializer: KSerializer<T>): JsonObject? {
    val keys = serializer.descriptor.annotations.filterIsInstance<NestedSettingsView>().firstOrNull()
        ?.let { listOf(it.key) + it.otherKeys }
    val parent = JsonObject(this)
    val newValues =
        if (keys != null) getJsonChild(parent, keys)
        else parent
    return newValues
}

@OptIn(ExperimentalSerializationApi::class)
fun <S : Settings<S>, T : SettingsView<S>> Settings<S>.get(serializer: KSerializer<T>): T {
    return SettingsJson.decodeFromJsonElement(serializer, getValues(serializer) ?: JsonObject(mapOf()))
}

fun <S : Settings<S>, T : SettingsView<S>> Settings<S>.getOrNull(serializer: KSerializer<T>): T? {
    return SettingsJson.decodeFromJsonElement(serializer, getValues(serializer) ?: return null)
}

inline fun <S : Settings<S>, reified T : SettingsView<S>> Settings<S>.get(): T = get(serializer())

inline fun <S : Settings<S>, reified T : SettingsView<S>> Settings<S>.getOrNull(): T? = getOrNull(serializer())

@OptIn(ExperimentalSerializationApi::class)
fun <S : Settings<S>, T : SettingsView<S>> MutableSettings<S>.set(value: T, serializer: KSerializer<T>) {
    val keys = serializer.descriptor.annotations.filterIsInstance<NestedSettingsView>().firstOrNull()
        ?.let { listOf(it.key) + it.otherKeys }
    val newValues = SettingsJson.encodeToJsonElement(serializer, value) as? JsonObject
    if (newValues != null) {
        if (keys != null) putAll(putJsonChild(JsonObject(this), newValues, keys))
        else putAll(jsonMerge(JsonObject(this), newValues, listOf()))
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

/**
 * Allows to define a view on the settings.
 **
 * For example, when `key="dino"` the result would be:
 *
 * ```json
 * {
 *     "dino": {
 *         "unicorn": true
 *     },
 *     // other settings
 * }
 * ```
 *
 * Nesting is also allowed.
 *
 * For example, when `key="dino","unicorn"` the result would be:
 *
 * ```json
 * {
 *     "dino": {
 *         "unicorn": {
 *             "super": true
 *         }
 *         // other settings
 *     },
 *     // other settings
 * }
 * ```
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS)
annotation class NestedSettingsView(val key: String, vararg val otherKeys: String)
