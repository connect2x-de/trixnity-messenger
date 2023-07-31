package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.messenger.util.I18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReferencedMessage
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReferencedMessage.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.bodyWithoutFallback

private val log = KotlinLogging.logger { }

interface RichRepliesComputations {
    fun getReferencedMessage(
        matrixClient: MatrixClient,
        relatesTo: RelatesTo?,
        inRoom: RoomId,
    ): Flow<ReferencedMessage?>
}

class RichRepliesComputationsImpl(
    private val i18n: I18n,
    private val thumbnails: Thumbnails,
    private val fileNameComputations: FileNameComputations,
) : RichRepliesComputations {

    @OptIn(FlowPreview::class)
    override fun getReferencedMessage(
        matrixClient: MatrixClient,
        relatesTo: RelatesTo?,
        inRoom: RoomId,
    ): Flow<ReferencedMessage?> {
        return if (relatesTo == null) {
            flowOf(null)
        } else {
            val replyToEventId = relatesTo.replyTo?.eventId
            if (replyToEventId != null) {
                val timelineEventFlow =
                    matrixClient.room.getTimelineEvent(inRoom, replyToEventId) {
                        allowReplaceContent = false
                    }
                combine(
                    timelineEventFlow.filterNotNull(),
                    timelineEventFlow.filterNotNull()
                        .flatMapConcat { matrixClient.user.getById(inRoom, it.event.sender) }
                ) { timelineEvent, roomUser ->
                    val sender = roomUser?.name ?: i18n.commonUnknown()
                    val content = timelineEvent.content?.getOrNull()
                    log.debug { "referenced message: ${timelineEvent.eventId} (of type ${content?.let { it::class } ?: "unknown"})" }
                    when (content) {
                        is RoomMessageEventContent.TextMessageEventContent ->
                            ReferencedTextMessage(sender, content.bodyWithoutFallback)

                        is RoomMessageEventContent.NoticeMessageEventContent ->
                            ReferencedTextMessage(sender, content.bodyWithoutFallback)

                        is RoomMessageEventContent.EmoteMessageEventContent ->
                            ReferencedTextMessage(sender, content.bodyWithoutFallback)

                        is RoomMessageEventContent.ImageMessageEventContent -> {
                            val thumbnail = thumbnails.loadThumbnail(
                                matrixClient,
                                content,
                                MutableStateFlow(null), // progress should not be needed as the thumbnail is available locally
                            )
                            ReferencedImageMessage(
                                sender,
                                thumbnail,
                                fileNameComputations.getOrCreateFileName(
                                    content.bodyWithoutFallback,
                                    content.info?.mimeType,
                                    ContentType.Image.Any
                                ),
                            )
                        }

                        is RoomMessageEventContent.VideoMessageEventContent -> {
                            val thumbnail = thumbnails.loadThumbnail(
                                matrixClient,
                                content,
                                MutableStateFlow(null), // progress should not be needed as the thumbnail is available locally
                            )
                            ReferencedVideoMessage(
                                sender,
                                thumbnail,
                                fileNameComputations.getOrCreateFileName(
                                    content.bodyWithoutFallback,
                                    content.info?.mimeType,
                                    ContentType.Video.Any
                                ),
                            )
                        }

                        is RoomMessageEventContent.AudioMessageEventContent -> ReferencedAudioMessage(
                            sender,
                            fileNameComputations.getOrCreateFileName(
                                content.bodyWithoutFallback,
                                content.info?.mimeType,
                                ContentType.Audio.Any
                            ),
                        )

                        is RoomMessageEventContent.FileMessageEventContent -> ReferencedFileMessage(
                            sender,
                            content.fileName ?: content.bodyWithoutFallback
                        )

                        else -> ReferencedUnknownMessage(sender)
                    }
                }
            } else {
                flowOf(null)
            }
        }
    }
}