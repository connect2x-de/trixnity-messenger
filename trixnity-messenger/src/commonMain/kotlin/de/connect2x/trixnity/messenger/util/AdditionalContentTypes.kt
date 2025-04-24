package de.connect2x.trixnity.messenger.util

import io.ktor.http.ContentType

val ContentType.Image.Webp: ContentType
    get() = ContentType("image", "webp")

val ContentType.Image.BMP: ContentType
    get() = ContentType("image", "BMP")

