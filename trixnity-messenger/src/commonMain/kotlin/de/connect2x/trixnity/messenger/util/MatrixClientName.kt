package de.connect2x.trixnity.messenger.util

import io.ktor.util.*

fun String.cleanAccountName(): String {
    return this.encodeBase64()
}

fun String.getAccountName(): String {
    return this.decodeBase64String()
}