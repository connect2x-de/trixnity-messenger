package de.connect2x.trixnity.messenger.util

private val emojiPattern: Regex =
    Regex("^([\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+|[\\u203C-\\u3299]|[\\u0023-\\u0039]\\uFE0F?\\u20E3|[\\u00A9\\u00AE\\u2122\\u3030]|\\uFE0F)+$")

expect val String.graphCount: Int

expect inline fun String.forEachGraph(crossinline consumer: (graph: String, index: Int) -> Boolean)

fun String.subGraph(start: Int, end: Int = graphCount - 1): String {
    if (isEmpty()) return ""
    var buffer = ""
    forEachGraph { graph, index ->
        when {
            index < start -> return@forEachGraph true
            index > end -> return@forEachGraph false
        }
        buffer += graph
        true
    }
    return buffer
}

fun String.firstGraph(): String {
    if (isEmpty()) return ""
    var buffer = ""
    forEachGraph { graph, index ->
        buffer = graph
        false
    }
    return buffer
}

fun String.isEmoji(): Boolean = emojiPattern.matches(this)
