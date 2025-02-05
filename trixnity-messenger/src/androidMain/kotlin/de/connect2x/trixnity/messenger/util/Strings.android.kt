package de.connect2x.trixnity.messenger.util

import android.icu.text.BreakIterator

actual val String.graphemeCount: Int
    get() {
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(this)
        var count = 0
        while(iterator.next() != BreakIterator.DONE) {
            ++count
        }
        return count
    }
