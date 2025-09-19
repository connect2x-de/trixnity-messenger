package de.connect2x.trixnity.messenger.settings

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

fun jsonMerge(source: JsonObject, update: JsonObject, override: List<String>? = null): JsonObject {
    if (override?.isEmpty() == true) return JsonObject(source + update)
    val result = source.toMutableMap()

    data class StackElement(val base: MutableMap<String, JsonElement>, val update: JsonObject, val path: List<String>)

    val stack = ArrayDeque<StackElement>()

    stack.add(StackElement(result, update, emptyList()))

    while (stack.isNotEmpty()) {
        val (currentBase, currentUpdate, path) = stack.removeLast()

        for ((key, updateValue) in currentUpdate) {
            val baseValue = currentBase[key]
            
            if (baseValue is JsonObject && updateValue is JsonObject) {
                val newPath = path + key
                if (newPath == override) {
                    currentBase[key] = JsonObject(baseValue + updateValue)
                } else {
                    val mergedChild = baseValue.toMutableMap()
                    currentBase[key] = JsonObject(mergedChild)
                    stack.add(StackElement(mergedChild, updateValue, newPath))
                }
            } else {
                currentBase[key] = updateValue
            }
        }
    }

    return JsonObject(result)
}
