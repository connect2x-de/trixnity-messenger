package de.connect2x.trixnity.messenger.util

expect val String.graphemeCount: Int

expect inline fun String.forEachGrapheme(crossinline consumer: (graph: String, index: Int) -> Boolean)

fun String.subGraph(start: Int, end: Int = graphemeCount - 1): String {
    if (isEmpty()) return ""
    var buffer = ""
    forEachGrapheme { graph, index ->
        when {
            index < start -> return@forEachGrapheme true
            index > end -> return@forEachGrapheme false
        }
        buffer += graph
        true
    }
    return buffer
}

fun String.firstGraph(): String {
    var buffer = ""
    forEachGrapheme { graph, index ->
        buffer = graph
        false
    }
    return buffer
}
