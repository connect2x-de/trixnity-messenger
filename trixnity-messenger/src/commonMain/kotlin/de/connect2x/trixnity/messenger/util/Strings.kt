package de.connect2x.trixnity.messenger.util

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
