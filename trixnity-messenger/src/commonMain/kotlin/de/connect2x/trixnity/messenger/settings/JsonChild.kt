package de.connect2x.trixnity.messenger.settings

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

internal fun getJsonChild(
    source: JsonObject,
    vararg keys: String
): JsonObject = getJsonChild(source, keys.toList())

internal fun getJsonChild(
    source: JsonObject,
    keys: List<String>
): JsonObject {
    if (keys.isEmpty()) return source

    var target: JsonObject = source
    keys.forEach { segment ->
        target = target[segment] as? JsonObject ?: return JsonObject(mapOf())
    }
    return target
}

internal fun putJsonChild(
    source: JsonObject,
    value: JsonObject,
    vararg keys: String
): JsonObject = putJsonChild(source, value, keys.toList())

internal fun putJsonChild(
    source: JsonObject,
    value: JsonObject,
    keys: List<String>
): JsonObject {
    val update =
        keys.asReversed().fold(value) { child, segment -> buildJsonObject { put(segment, child) } }
    return jsonMerge(source, update)
}
