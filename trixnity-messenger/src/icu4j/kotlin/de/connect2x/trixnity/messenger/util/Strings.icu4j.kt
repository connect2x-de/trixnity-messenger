package de.connect2x.trixnity.messenger.util

import com.ibm.icu.text.BreakIterator

internal object Icu4jGraphemeIterableProvider : GraphemeIterableProvider {
    override fun invoke(string: String): GraphemeIterable {
        return Icu4jGaphemeIterable(string)
    }
}

private class Icu4jGaphemeIterable(val inner: String) : GraphemeIterable {
    override val graphemeCount: Int
        get() {
            val iterator = BreakIterator.getCharacterInstance()
            iterator.setText(inner)

            var count = 0
            while (iterator.next() != BreakIterator.DONE) {
                count++
            }
            return count
        }

    override fun iterator(): GraphemeIterator
            = Icu4jGaphemeIterator(inner)
}

private class Icu4jGaphemeIterator(val inner: String) : GraphemeIterator {

    private val iterator = BreakIterator.getCharacterInstance().apply {
        setText(inner)
    }

    var start = iterator.first()
    var end = iterator.next()

    override fun next(): String {
        val subString = inner.substring(start, end)
        start = end
        end = iterator.next()
        return subString
    }

    override fun hasNext(): Boolean = end != BreakIterator.DONE
}
