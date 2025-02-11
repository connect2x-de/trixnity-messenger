package de.connect2x.trixnity.messenger.util

import js.intl.Granularity
import js.intl.Segmenter
import js.intl.SegmenterOptions

actual val String.graphCount: Int
    get() {
        val segments: dynamic = Segmenter("en", SegmenterOptions.invoke(Granularity.grapheme)).segment(this)
        val iterator = segments.iterator()
        var count = 0
        while (iterator.hasNext()) {
            if (iterator.next().done as Boolean) break
            ++count
        }
        return count
    }

actual inline fun String.forEachGraph(crossinline consumer: (graph: String, index: Int) -> Boolean) {
    val segments: dynamic = Segmenter("en", SegmenterOptions.invoke(Granularity.grapheme)).segment(this)
    val iterator = segments.iterator()
    var index = 0
    while (iterator.hasNext()) {
        val segment = iterator.next()
        if (segment.done as Boolean) break
        consumer(segment.segment, index)
        ++index
    }
}
