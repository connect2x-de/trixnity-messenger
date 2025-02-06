package de.connect2x.trixnity.messenger.util

import com.ibm.icu.text.BreakIterator

actual val String.graphemeCount: Int
    get() {
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(this)
        var count = 0
        while (iterator.next() != BreakIterator.DONE) {
            ++count
        }
        return count
    }

actual inline fun String.forEachGrapheme(crossinline consumer: (graph: String, index: Int) -> Boolean) {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(this)
    var start = iterator.first()
    var end = iterator.next()
    var index = 0
    while (end != BreakIterator.DONE) {
        if (!consumer(substring(start, end), index)) break
        start = end
        end = iterator.next()
        ++index
    }
}
