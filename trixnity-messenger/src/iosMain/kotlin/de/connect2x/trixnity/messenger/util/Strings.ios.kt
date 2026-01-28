package de.connect2x.trixnity.messenger.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSMaxRange
import platform.Foundation.NSString
import platform.Foundation.rangeOfComposedCharacterSequenceAtIndex
import platform.Foundation.substringWithRange

internal actual fun platformGraphemeIterableProvider(): GraphemeIterableProvider = NsGraphemeIterableProvider

internal object NsGraphemeIterableProvider : GraphemeIterableProvider {
    override fun invoke(string: String): GraphemeIterable {
        return NsGaphemeIterable(string)
    }
}

private class NsGaphemeIterable(
    inner: String
) : GraphemeIterable {

    @Suppress("CAST_NEVER_SUCCEEDS")
    val nsString = requireNotNull(inner as NSString)

    override val graphemeCount: Int
        @OptIn(ExperimentalForeignApi::class)
        get() {
            var index = 0UL
            var count = 0
            while (index < nsString.length) {
                val range = nsString.rangeOfComposedCharacterSequenceAtIndex(index)
                index = NSMaxRange(range)
                count++
            }
            return count
        }

    override fun iterator(): GraphemeIterator = NsGaphemeIterator(nsString)
}

private class NsGaphemeIterator(val inner: NSString) : GraphemeIterator {

    var index = 0UL

    @OptIn(ExperimentalForeignApi::class)
    override fun next(): String {
        val range = inner.rangeOfComposedCharacterSequenceAtIndex(index)
        val substring = inner.substringWithRange(range)
        index = NSMaxRange(range)
        return substring
    }

    override fun hasNext(): Boolean =
        index < inner.length
}
