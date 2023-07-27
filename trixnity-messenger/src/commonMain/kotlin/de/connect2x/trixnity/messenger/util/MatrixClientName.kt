package de.connect2x.trixnity.messenger.util

import korlibs.crypto.encoding.fromBase64
import korlibs.crypto.encoding.toBase64
import korlibs.io.lang.UTF8
import korlibs.io.lang.toByteArray
import korlibs.io.lang.toString

fun String.cleanAccountName(): String {
    return this.toByteArray(UTF8).toBase64()
}

fun String.getAccountName(): String {
    return this.fromBase64().toString(UTF8)
}