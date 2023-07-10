package de.connect2x.trixnity.messenger.util

import io.ktor.util.*

fun String.cleanAccountName(): String {
    return this.encodeToByteArray().encodeBase64()
}