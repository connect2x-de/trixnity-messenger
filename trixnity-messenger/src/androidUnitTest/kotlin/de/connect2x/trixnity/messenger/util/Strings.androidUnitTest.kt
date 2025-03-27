package de.connect2x.trixnity.messenger.util

actual fun testGraphemeIterableProvider(): GraphemeIterableProvider
    = Icu4jGraphemeIterableProvider
