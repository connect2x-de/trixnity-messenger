package de.connect2x.trixnity.messenger.util

import io.ktor.http.ContentType

sealed interface CopyableContent {
    data class File(val file: ByteArray, val fileName: String, val fileType: ContentType) : CopyableContent
    data class FormattedText(val text: String, val unformattedText: String) : CopyableContent
    data class Text(val text: String) : CopyableContent
    data class Location(val geoUri: String, val description: String) : CopyableContent {
        val coordinates = this.geoUri
            .removePrefix("geo:").substringBefore(";")
    }
}

