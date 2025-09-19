package de.connect2x.trixnity.messenger.settings

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

fun jsonMerge(source: JsonObject, update: JsonObject): JsonObject {
    val result = source.toMutableMap()

    val stack = ArrayDeque<Pair<MutableMap<String, JsonElement>, JsonObject>>()
    stack.add(result to update)

    while (stack.isNotEmpty()) {
        val (currentBase, currentUpdate) = stack.removeLast()

        for ((key, updateValue) in currentUpdate) {
            val baseValue = currentBase[key]

            if (baseValue is JsonObject && updateValue is JsonObject) {
                val mergedChild = baseValue.toMutableMap()
                currentBase[key] = JsonObject(mergedChild)
                stack.add(mergedChild to updateValue)
            } else {
                currentBase[key] = updateValue
            }
        }
    }

    return JsonObject(result)
}
