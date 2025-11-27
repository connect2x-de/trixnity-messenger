package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module
import org.koin.dsl.module

interface GraphemeIterableProvider {
    operator fun invoke(string: String): GraphemeIterable
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

@Suppress("DEPRECATION")
@Deprecated(
    message = "The previous emoji detection logic, use GraphemeIterable.graphemeCount == 1 instead",
    level = DeprecationLevel.WARNING,
)
fun String.isEmoji(): Boolean = graphCount == 1
