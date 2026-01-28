package de.connect2x.trixnity.messenger.util

import io.ktor.http.*

val ContentType.Image.Webp: ContentType
    get() = ContentType("image", "webp")

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
val ContentType.Image.BMP: ContentType
    get() = ContentType("image", "BMP")

