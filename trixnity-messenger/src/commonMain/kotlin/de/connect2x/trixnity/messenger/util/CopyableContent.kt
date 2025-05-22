package de.connect2x.trixnity.messenger.util

import io.ktor.http.ContentType

interface CopyableContent {
    /**
     * Text to copy incase type is not supported by the platform
     */
    val fallbackText: String
}

data class File(val file: ByteArray, val fileName: String, val fileType: ContentType) : CopyableContent {
    override val fallbackText = fileName
}

data class FormattedText(val text: String, val unformattedText: String) : CopyableContent {
    override val fallbackText = unformattedText
}

data class Text(val text: String) : CopyableContent {
    override val fallbackText = text
}

data class Location(val geoUri: String, val description: String) : CopyableContent {
    val coordinates =
        geoUri.removePrefix("geo:").substringBefore(";")
    override val fallbackText: String =
        if (description.isNotBlank()) "$description ($coordinates)" else coordinates
}
