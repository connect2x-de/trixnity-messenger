package de.connect2x.trixnity.messenger.viewmodel.util

import io.ktor.http.*

object MimeTypes {

    fun guessByFileName(fileName: String?): ContentType {
        val fileExtension = fileName?.split('.')?.lastOrNull()
        return guessByFileExtension(fileExtension)
    }

    fun guessByFileExtension(fileExtension: String?): ContentType {
        return when (fileExtension?.lowercase()) {
            // image
            "jpeg", "jpg" -> ContentType.Image.JPEG
            "png" -> ContentType.Image.PNG
            "gif" -> ContentType.Image.GIF
            // video
            "mp4" -> ContentType.Video.MP4
            "mov" -> ContentType.Video.QuickTime
            // audio
            "mp3" -> ContentType.Audio.MPEG
            "ogg" -> ContentType.Audio.OGG
            // text
            "txt" -> ContentType.Text.Plain
            "pdf" -> ContentType.Application.Pdf
            // zip
            "zip", "7z" -> ContentType.Application.Zip
            "gz" -> ContentType.Application.GZip
            // other
            else -> ContentType.Any
        }
    }

}