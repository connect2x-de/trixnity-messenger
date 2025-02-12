package de.connect2x.trixnity.messenger.util

import js.intl.Granularity
import js.intl.SegmentData
import js.intl.Segmenter
import js.intl.SegmenterOptions
import js.iterable.JsIterable

// Define our own external for this since kotlin.browser doesn't provide iterable bindings without going through dynamic
internal external interface Segments : JsIterable<SegmentData>

actual val String.graphCount: Int
    get() {
        val segments = Segmenter("en", SegmenterOptions(Granularity.grapheme)).segment(this).unsafeCast<Segments>()
        var count = 0
        for (segment in segments) ++count
        return count
    }

actual inline fun String.forEachGraph(crossinline consumer: (graph: String, index: Int) -> Boolean) {
    val segments = Segmenter("en", SegmenterOptions(Granularity.grapheme)).segment(this).unsafeCast<Segments>()
    var index = 0
    for (segment in segments) {
        if(!consumer(segment.segment, index++)) break
    }
}
