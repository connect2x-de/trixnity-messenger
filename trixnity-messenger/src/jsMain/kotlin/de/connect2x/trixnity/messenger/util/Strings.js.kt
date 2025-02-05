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

actual val String.graphemeCount: Int
    get() = splitGraphemes().size

actual inline fun String.forEachGrapheme(crossinline consumer: (graph: String, index: Int) -> Unit) {
    val segments = splitGraphemes()
    for (i in segments.indices) {
        consumer(segments[i], i)
    }
}
