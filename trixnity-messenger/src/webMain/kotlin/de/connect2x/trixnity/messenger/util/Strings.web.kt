@file:OptIn(ExperimentalWasmJsInterop::class)

package de.connect2x.trixnity.messenger.util

import js.intl.Granularity
import js.intl.SegmentData
import js.intl.Segmenter
import js.intl.grapheme
import js.iterable.JsIterable
import js.iterable.JsIterator
import js.iterable.isYield
import js.objects.unsafeJso
import js.reflect.Reflect
import js.symbol.Symbol
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.toJsString
import kotlin.js.unsafeCast

// Define our own external for this since kotlin.browser doesn't provide iterable bindings without going through dynamic
internal external interface Segments : JsIterable<SegmentData>

internal actual fun platformGraphemeIterableProvider(): GraphemeIterableProvider = JsGraphemeIterableProvider

internal object JsGraphemeIterableProvider : GraphemeIterableProvider {
    override fun invoke(string: String): GraphemeIterable {
        return JsGaphemeIterable(string)
    }
}

private class JsGaphemeIterable(inner: String) : GraphemeIterable {
    val segments =
        Segmenter("en".toJsString(), unsafeJso { granularity = Granularity.grapheme })
            .segment(inner)
            .unsafeCast<Segments>()

    override fun iterator(): GraphemeIterator = JsGaphemeIterator(iterator(segments))

    override val graphemeCount: Int
        get() = iterator(segments).asSequence().count()
}

private class JsGaphemeIterator(val inner: Iterator<SegmentData>) : GraphemeIterator {
    override fun next(): String = inner.next().segment

    override fun hasNext(): Boolean = inner.hasNext()
}

private fun <T : JsAny?> iterator(iterable: JsIterable<T>): Iterator<T> =
    iterable.unsafeCast<JsIterableFixed<T>>().iterator()

private external interface JsIterableFixed<out T : JsAny?> : JsAny

private operator fun <T : JsAny?> JsIterableFixed<T>.get(
    key: Symbol.iterator
): Function<JsIterableFixed<T>, JsIterator<T>> =
    checkNotNull(Reflect.get(this, key)).unsafeCast<Function<JsIterableFixed<T>, JsIterator<T>>>()

private external interface Function<C : JsAny, R : JsAny?> : JsAny {
    fun call(thisArg: C): R
}

private operator fun <T : JsAny?> JsIterableFixed<T>.iterator(): Iterator<T> {
    val iterator = this[Symbol.iterator].call(this)
    return generateSequence {
            val result = iterator.next()
            if (isYield(result)) result else null
        }
        .map { it.value }
        .iterator()
}
