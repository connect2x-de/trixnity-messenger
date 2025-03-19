package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module
import org.koin.dsl.module

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

interface GraphemeIterableProvider {
    operator fun invoke(string: String) : GraphemeIterable
}

interface GraphemeIterable : Iterable<String> {
    val graphemeCount: Int

    override fun iterator(): GraphemeIterator
}

interface GraphemeIterator : Iterator<String>

fun platformStringsModule(): Module = module {
    single<GraphemeIterableProvider> { PlatformGraphemeIterableProvider }
}

internal expect fun platformGraphemeIterableProvider(): GraphemeIterableProvider

object PlatformGraphemeIterableProvider : GraphemeIterableProvider {
    private val provider = platformGraphemeIterableProvider()

    override fun invoke(string: String): GraphemeIterable = provider(string)
}

@Deprecated(
    message = "This cannot be overridden by DI, please use platformStringModule instead",
    level = DeprecationLevel.WARNING
)
val String.graphCount: Int
    get() = PlatformGraphemeIterableProvider(this).graphemeCount

@Deprecated(
    message = "This cannot be overridden by DI, please use platformStringModule instead",
    level = DeprecationLevel.WARNING
)
inline fun String.forEachGraph(crossinline consumer: (graph: String, index: Int) -> Boolean) {
    PlatformGraphemeIterableProvider(this).forEachIndexed { index, graph ->
        println(graph)
        if (!consumer(graph, index)) return
    }
}

@Deprecated(
    message = "This cannot be overridden by DI, please use platformStringModule instead",
    level = DeprecationLevel.WARNING
)
@Suppress("DEPRECATION")
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

@Deprecated(
    message = "This cannot be overridden by DI, please use platformStringModule instead",
    level = DeprecationLevel.WARNING
)
@Suppress("DEPRECATION")
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
