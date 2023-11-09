package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReferencedMessage
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReferencedMessage.*
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.avatarUrl
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.bodyWithoutFallback
import net.folivo.trixnity.utils.toByteArray

private val log = KotlinLogging.logger { }

interface RichRepliesComputations {
    fun getReferencedMessage(
        matrixClient: MatrixClient,
        relatesTo: RelatesTo?,
        inRoom: RoomId,
    ): Flow<ReferencedMessage?>
}

class RichRepliesComputationsImpl(
    private val initials: Initials,
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
                        .flatMapConcat {
                            matrixClient.user.getById(inRoom, it.event.sender)
                                .map { user ->
                                    UserInfoElement(
                                        name = user?.name ?: it.event.sender.full,
                                        initials = user?.name?.let(initials::compute),
                                        image = user?.avatarUrl?.let { avatarUrl ->
                                            matrixClient.media.getThumbnail(
                                                avatarUrl,
                                                avatarSize().toLong(),
                                                avatarSize().toLong()
                                            ).fold(
                                                onSuccess = { it },
                                                onFailure = {
                                                    log.error(it) { "Cannot load avatar image for user '${user.name}'." }
                                                    null
                                                }
                                            )?.toByteArray()
                                        }
                                    )
                                }
                        }
                ) { timelineEvent, sender ->
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