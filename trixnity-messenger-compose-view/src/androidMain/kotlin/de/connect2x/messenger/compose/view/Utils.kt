package de.connect2x.messenger.compose.view

import android.content.ClipData

fun ClipData.toSequence(): Sequence<ClipData.Item> = sequence {
    for (i in 0 until itemCount) {
        yield(getItemAt(i))
    }
}

fun ClipData.toList(): List<ClipData.Item> = toSequence().toList()
