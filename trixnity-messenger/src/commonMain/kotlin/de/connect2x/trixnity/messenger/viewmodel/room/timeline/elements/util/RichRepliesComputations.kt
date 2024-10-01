package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReferencedMessage
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReferencedMessage.ReferencedAudioMessage
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReferencedMessage.ReferencedFileMessage
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReferencedMessage.ReferencedImageMessage
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReferencedMessage.ReferencedLocationMessage
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReferencedMessage.ReferencedTextMessage
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReferencedMessage.ReferencedUnknownMessage
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReferencedMessage.ReferencedVideoMessage
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.avatarUrl
import net.folivo.trixnity.client.store.eventId
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
    private val initials: Initials,
    private val thumbnails: Thumbnails,
) : RichRepliesComputations {

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
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
                        .flatMapLatest {
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
                                            )
                                                .onFailure { exc ->
                                                    if (exc !is CancellationException)
                                                        log.error(exc) { "Cannot load avatar image for user '${user.name}'." }
                                                }
                                                .getOrNull()
                                        },
                                        userId = it.event.sender
                                    )
                                }
                        }
                ) { timelineEvent, sender ->
                    val content = timelineEvent.content?.getOrNull()
                    log.debug { "referenced message: ${timelineEvent.eventId} (of type ${content?.let { it::class } ?: "unknown"})" }
                    if (content is RoomMessageEventContent)
                        when (content) {
                            is RoomMessageEventContent.TextBased ->
                                ReferencedTextMessage(sender, content.bodyWithoutFallback)

                            is RoomMessageEventContent.FileBased.Image -> {
                                val thumbnail = thumbnails.loadThumbnail(
                                    matrixClient,
                                    content,
                                    MutableStateFlow(null), // progress should not be needed as the thumbnail is available locally
                                )
                                ReferencedImageMessage(
                                    sender,
                                    thumbnail,
                                    content.fileName ?: content.body,
                                )
                            }

                            is RoomMessageEventContent.FileBased.Video -> {
                                val thumbnail = thumbnails.loadThumbnail(
                                    matrixClient,
                                    content,
                                    MutableStateFlow(null), // progress should not be needed as the thumbnail is available locally
                                )
                                ReferencedVideoMessage(
                                    sender,
                                    thumbnail,
                                    content.fileName ?: content.body,
                                )
                            }

                            is RoomMessageEventContent.FileBased.Audio -> ReferencedAudioMessage(
                                sender,
                                content.fileName ?: content.body,
                            )

                            is RoomMessageEventContent.FileBased.File -> ReferencedFileMessage(
                                sender,
                                content.fileName ?: content.bodyWithoutFallback
                            )

                            is RoomMessageEventContent.Location -> ReferencedLocationMessage(
                                sender,
                                content.geoUri,
                                content.body
                            )

                            is RoomMessageEventContent.Unknown,
                            is RoomMessageEventContent.VerificationRequest -> ReferencedUnknownMessage(sender)
                        } else ReferencedUnknownMessage(sender)
                }
            } else {
                flowOf(null)
            }
        }
    }
}
