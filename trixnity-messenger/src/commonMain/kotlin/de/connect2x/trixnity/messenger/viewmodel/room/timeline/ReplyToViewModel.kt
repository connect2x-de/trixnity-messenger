package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReplyType.AudioReply
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReplyType.EmoteReply
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReplyType.FileReply
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReplyType.ImageReply
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReplyType.LocationReply
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReplyType.NoticeReply
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReplyType.TextReply
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReplyType.UnknownReply
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReplyType.VideoReply
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.util.previewImageByteArray
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.Location
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import net.folivo.trixnity.core.model.events.m.room.bodyWithoutFallback
import org.koin.core.component.get

private val log = KotlinLogging.logger { }


interface ReplyToViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        eventId: EventId,
        onCancelReplyTo: () -> Unit,
    ): ReplyToViewModel {
        return ReplyToViewModelImpl(
            viewModelContext,
            selectedRoomId,
            eventId,
            onCancelReplyTo,
        )
    }

    companion object : ReplyToViewModelFactory
}

interface ReplyToViewModel {
    val eventId: EventId
    val replyTo: StateFlow<ReplyType?>
    fun cancelReplyTo()
}

@OptIn(ExperimentalCoroutinesApi::class)
open class ReplyToViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    selectedRoomId: RoomId,
    override val eventId: EventId,
    private val onCancelReplyTo: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, ReplyToViewModel {

    private val thumbnails = get<Thumbnails>()
    override val replyTo: StateFlow<ReplyType?>

    private val thumbnailLoading = MutableStateFlow(false)
    private val thumbnailCache = MutableStateFlow<ByteArray?>(null)

    init {
        replyTo = combine(
            matrixClient.room.getTimelineEvent(selectedRoomId, eventId),
            matrixClient.room.getTimelineEvent(selectedRoomId, eventId)
                .flatMapLatest {
                    if (it == null) flowOf(null) else matrixClient.user.getById(
                        selectedRoomId,
                        it.sender
                    )
                },
        ) { timelineEvent, roomUser ->
            val sender = roomUser?.name ?: i18n.commonUnknown()
            when (val content = timelineEvent?.content?.getOrNull()) { // in case the event has to be decrypted
                is TextBased.Notice -> NoticeReply(content.bodyWithoutFallback, sender)
                is TextBased.Emote -> EmoteReply(content.bodyWithoutFallback, sender)
                is TextBased.Text -> TextReply(content.bodyWithoutFallback, sender)
                is FileBased.Image -> {
                    val thumbnail = if (thumbnailCache.value == null && thumbnailLoading.value.not()) {
                        thumbnailLoading.value = true
                        val t = thumbnails.loadThumbnail(
                            matrixClient,
                            content,
                            MutableStateFlow(null), // progress should not be needed as the thumbnail is available locally
                        )
                        thumbnailCache.value = t
                        thumbnailLoading.value = false
                        t
                    } else thumbnailCache.value
                    ImageReply(thumbnail, content.fileName ?: content.body, sender)
                }

                is FileBased.Video -> {
                    val thumbnail = if (thumbnailCache.value == null && thumbnailLoading.value.not()) {
                        thumbnailLoading.value = true
                        val t = thumbnails.loadThumbnail(
                            matrixClient,
                            content,
                            MutableStateFlow(null), // progress should not be needed as the thumbnail is available locally
                        )
                        thumbnailCache.value = t
                        thumbnailLoading.value = false
                        t
                    } else thumbnailCache.value
                    VideoReply(thumbnail, content.fileName ?: content.body, sender)
                }

                is FileBased.Audio -> AudioReply(content.fileName ?: content.body, sender)

                is FileBased.File -> FileReply(content.fileName ?: content.body, sender)

                is Location -> LocationReply(content.body, content.geoUri, sender)

                else -> UnknownReply(sender)
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    }

    override fun cancelReplyTo() {
        onCancelReplyTo()
    }
}

sealed interface ReplyType {
    val senderName: String

    data class NoticeReply(
        val text: String,
        override val senderName: String,
    ) : ReplyType

    data class EmoteReply(
        val text: String,
        override val senderName: String,
    ) : ReplyType

    data class TextReply(
        val text: String,
        override val senderName: String,
    ) : ReplyType

    data class ImageReply(
        val thumbnail: ByteArray?,
        val fileName: String,
        override val senderName: String,
    ) : ReplyType

    data class VideoReply(
        val thumbnail: ByteArray?,
        val fileName: String,
        override val senderName: String,
    ) : ReplyType

    data class AudioReply(
        val fileName: String,
        override val senderName: String,
    ) : ReplyType

    data class FileReply(
        val fileName: String,
        override val senderName: String,
    ) : ReplyType

    data class LocationReply(
        val body: String,
        val geoUri: String,
        override val senderName: String,
    ) : ReplyType

    data class UnknownReply(
        override val senderName: String,
    ) : ReplyType
}

class PreviewReplyToViewModel : ReplyToViewModel {
    override val eventId = EventId("1")
    override val replyTo: MutableStateFlow<ReplyType?> =
        MutableStateFlow(
            TextReply(
                "Hello World! Yes, this is a rather long and convoluted message that should span several lines.",
                senderName = "Martin"
            )
        )

    override fun cancelReplyTo() {
    }
}

class PreviewReplyToViewModel2 : ReplyToViewModel {
    override val eventId = EventId("1")
    override val replyTo: MutableStateFlow<ReplyType?> =
        MutableStateFlow(
            ImageReply(
                thumbnail = previewImageByteArray(),
                senderName = "Martin",
                fileName = "image with a very very - long name.png",
            )
        )

    override fun cancelReplyTo() {
    }
}
