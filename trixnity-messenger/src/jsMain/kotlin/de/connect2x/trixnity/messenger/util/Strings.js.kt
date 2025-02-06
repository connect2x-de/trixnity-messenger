package de.connect2x.trixnity.messenger.util

import js.intl.Granularity
import js.intl.Segmenter
import js.intl.SegmenterOptions

@PublishedApi
internal fun String.splitGraphemes(): Array<String> {
    return Segmenter("en", SegmenterOptions.invoke(Granularity.grapheme))
        .segment(this)
        .unsafeCast<Array<String>>()
}

actual val String.graphCount: Int
    get() = splitGraphemes().size

actual inline fun String.forEachGraph(crossinline consumer: (graph: String, index: Int) -> Boolean) {
    val segments = splitGraphemes()
    for (i in segments.indices) {
        if(!consumer(segments[i], i)) break
    }
}
