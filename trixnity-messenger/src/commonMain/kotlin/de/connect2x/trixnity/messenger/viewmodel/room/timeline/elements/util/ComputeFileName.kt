package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import io.ktor.http.*
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import okio.ByteString.Companion.toByteString

fun interface ComputeFileName {
    operator fun invoke(content: RoomMessageEventContent.FileBased): String
}

class ComputeFileNameImpl(private val config: MatrixMessengerConfiguration) :
    ComputeFileName {

    override fun invoke(content: RoomMessageEventContent.FileBased): String {
        if (content is RoomMessageEventContent.FileBased.File)
            content.fileName?.also { return it }

        val fileName =
            (content.file?.url ?: content.url)
                ?.encodeToByteArray()?.toByteString()?.base64Url()?.substringBefore("=")
                ?: "unknown"
        val extension = content.info?.mimeType?.let(ContentType::parse)?.fileExtensions()?.firstOrNull()
        val appName = config.appName
        return if (extension != null) "$appName-$fileName.$extension"
        else "$appName-$fileName" // TODO as fallback: look into first bytes for file extension (https://en.wikipedia.org/wiki/List_of_file_signatures)
    }
}