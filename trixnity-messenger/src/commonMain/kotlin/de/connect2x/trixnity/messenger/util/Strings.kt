package de.connect2x.trixnity.messenger.util

expect val String.graphemeCount: Int

expect inline fun String.forEachGrapheme(consumer: (graph: String, index: Int) -> Unit)

fun Sequence<Char>.takeGraphemes(count: Int): Sequence<Char> {
    var buffer = ""
    joinToString(separator = "").forEachGrapheme { graph, index ->
        if (index > count - 1) return@forEachGrapheme
        buffer += graph
    }
    return buffer.asSequence()
}
