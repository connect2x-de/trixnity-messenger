package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.client.toMxcUri
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi

private val mediaPathPattern = Regex("/[A-Za-z0-9_-]+")

@TrixnityMessengerPrivateApi
fun isMatrixMediaUri(uri: String?): Boolean {
    if (uri == null) return false
    val parsed = runCatching { uri.toMxcUri() }.getOrNull() ?: return false
    val prefix = "${parsed.protocol.name}://"
    if (!uri.startsWith(prefix) || '?' in uri || '#' in uri || parsed.user != null || parsed.password != null)
        return false
    val authorityAndPath = uri.removePrefix(prefix)
    if (authorityAndPath.substringBefore('/').isEmpty()) return false
    val mediaPath = "/${authorityAndPath.substringAfter('/', missingDelimiterValue = "")}"
    return mediaPath == parsed.encodedPath && mediaPathPattern.matches(mediaPath)
}
