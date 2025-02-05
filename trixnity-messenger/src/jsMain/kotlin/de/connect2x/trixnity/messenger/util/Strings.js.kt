package de.connect2x.trixnity.messenger.util

import js.intl.Granularity
import js.intl.Segmenter
import js.intl.SegmenterOptions

actual val String.graphemeCount: Int
    get() = (Segmenter("en", SegmenterOptions.invoke(Granularity.grapheme)).segment(this).unsafeCast<Array<*>>()).size
