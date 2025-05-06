package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.util.CopyableContent.File
import de.connect2x.trixnity.messenger.util.CopyableContent.FormattedText
import de.connect2x.trixnity.messenger.util.CopyableContent.Location
import de.connect2x.trixnity.messenger.util.CopyableContent.Text
import de.connect2x.trixnity.messenger.viewmodel.util.limitedByteArrayOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased

private val log = KotlinLogging.logger {}

suspend fun RoomMessageEventContent.handleContent(
    write: suspend (CopyableContent) -> Unit,
    matrixClient: MatrixClient,
    maxMediaSize: Long
) = when (this) {
    is RoomMessageEventContent.FileBased -> {
        file?.let { encryptedFile ->
            matrixClient.media.getEncryptedMedia(encryptedFile).getOrNull()?.limitedByteArrayOrNull(maxMediaSize) {
                log.error(it) { "Failed to copy file" }
            }?.let { decryptedFile ->
                write(File(decryptedFile, body, info?.mimeType?.let(ContentType.Companion::parse) ?: ContentType.Any))
            }
        }
    }

    is RoomMessageEventContent.Location -> write(Location(geoUri, body))
    is TextBased.Emote -> write(Text("${matrixClient.userId} * $body"))
    is TextBased.Text, is TextBased.Notice -> write(
        formattedBody?.let { formattedBody ->
            FormattedText(formattedBody, body)
        } ?: Text(body)
    )

    is RoomMessageEventContent.Unknown, is RoomMessageEventContent.VerificationRequest -> {
        log.trace { "Tried to copy non-copyable message of type $type" }
    }
}
