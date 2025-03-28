package de.connect2x.trixnity.messenger.util

internal actual fun platformGraphemeIterableProvider(): GraphemeIterableProvider {
    return Icu4jGraphemeIterableProvider
}
