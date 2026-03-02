package de.connect2x.trixnity.messenger.util

import js.intl.Granularity
import js.intl.SegmentData
import js.intl.Segmenter
import js.intl.SegmenterOptions
import js.intl.grapheme
import js.iterable.JsIterable
import js.iterable.iterator

// Define our own external for this since kotlin.browser doesn't provide iterable bindings without going through dynamic
internal external interface Segments : JsIterable<SegmentData>

internal actual fun platformGraphemeIterableProvider(): GraphemeIterableProvider = JsGraphemeIterableProvider

internal object JsGraphemeIterableProvider : GraphemeIterableProvider {
    override fun invoke(string: String): GraphemeIterable {
        return JsGaphemeIterable(string)
    }
}

private class JsGaphemeIterable(
    inner: String
) : GraphemeIterable {
    val segments = Segmenter("en", SegmenterOptions(granularity = Granularity.grapheme))
        .segment(inner).unsafeCast<Segments>()

    override fun iterator(): GraphemeIterator = JsGaphemeIterator(segments.iterator())

    override val graphemeCount: Int
        get() = segments.iterator().asSequence().count()
}

private class JsGaphemeIterator(val inner: Iterator<SegmentData>) : GraphemeIterator {
    override fun next(): String = inner.next().segment
    override fun hasNext(): Boolean = inner.hasNext()
}
