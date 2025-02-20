package de.connect2x.trixnity.messenger.util

/*
 * See https://unicode.org/reports/tr51/tr51-28.html,
 * https://unicode.org/emoji/charts/full-emoji-list.html and
 * https://unicode.org/emoji/charts/full-emoji-modifiers.html
 * for the ranges.
 */
private const val SURROGATE_PAIR: String = """[\uD83C-\uDBFF\uDC00-\uDFFF]+"""
private const val SYMBOLICS: String = """[\u203C-\u3299]|[\u00A9\u00AE\u2122\u3030]"""
private const val KEYCAPS: String = """[\u0023-\u0039]\uFE0F?\u20E3"""
private const val VAR_SELECTOR: String = """\uFE0F"""
private const val FLAGS: String = """(?:\uD83C[\uDDE6-\uDDFF]){2}"""
private const val DIACRITICS: String = """[\u0300-\u036F]"""

private val emojiPattern: Regex =
    Regex("^($SURROGATE_PAIR|$SYMBOLICS|$KEYCAPS|$FLAGS|$DIACRITICS|$VAR_SELECTOR)+$")

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
