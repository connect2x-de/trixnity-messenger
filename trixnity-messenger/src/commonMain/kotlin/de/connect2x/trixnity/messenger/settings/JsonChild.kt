package de.connect2x.trixnity.messenger.settings

import kotlinx.serialization.json.JsonObject

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
    if (keys.isEmpty()) return JsonObject(source + value)

    var nextProperty = source
    val segmentsToProperty = keys.mapIndexed { index, segment ->
        nextProperty =
            if (index == keys.lastIndex) value
            else nextProperty[segment] as? JsonObject ?: JsonObject(mapOf())
        segment to nextProperty
    }.asReversed()

    var target = JsonObject(mapOf())
    segmentsToProperty.forEachIndexed { index, (segment, property) ->
        val parent =
            if (index == segmentsToProperty.lastIndex) source
            else segmentsToProperty[index + 1].second
        val siblings = parent[segment] as? JsonObject ?: JsonObject(mapOf())
        target = JsonObject(parent + (segment to JsonObject(siblings + property + target)))
    }
    return target
}
