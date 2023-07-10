package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReplyType.*
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.FileNameComputations
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.util.previewImageByteArray
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.Event.MessageEvent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.*
import net.folivo.trixnity.core.model.events.m.room.bodyWithoutFallback
import org.koin.core.component.get

private val log = KotlinLogging.logger { }


interface ReplyToViewModelFactory {
    fun newReplyToViewModel(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        event: MessageEvent<*>,
        onCancelReplyTo: () -> Unit,
    ): ReplyToViewModel {
        return ReplyToViewModelImpl(
            viewModelContext,
            selectedRoomId,
            event,
            onCancelReplyTo,
        )
    }
}

interface ReplyToViewModel {
    val event: MessageEvent<*>
    val replyTo: StateFlow<ReplyType?>
    fun cancelReplyTo()
}

open class ReplyToViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    selectedRoomId: RoomId,
    override val event: MessageEvent<*>,
    private val onCancelReplyTo: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, ReplyToViewModel {

    private val thumbnails = get<Thumbnails>()
    private val fileNameComputations = FileNameComputations(get())
    override val replyTo: StateFlow<ReplyType?>

    private val thumbnailLoading = MutableStateFlow(false)
    private val thumbnailCache = MutableStateFlow<ByteArray?>(null)

    init {
        replyTo = combine(
            matrixClient.room.getTimelineEvent(selectedRoomId, event.id),
            matrixClient.user.getById(selectedRoomId, event.sender),
        ) { timelineEvent, roomUser ->
            val sender = roomUser?.name ?: i18n.commonUnknown()
            when (val content = timelineEvent?.content?.getOrNull()) { // in case the event has to be decrypted
                is TextMessageEventContent -> TextReply(content.bodyWithoutFallback, sender)
                is NoticeMessageEventContent -> TextReply(content.bodyWithoutFallback, sender)
                is EmoteMessageEventContent -> TextReply(content.bodyWithoutFallback, sender)
                is ImageMessageEventContent -> {
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
                    ImageReply(
                        thumbnail,
                        fileNameComputations.getOrCreateFileName(
                            content.bodyWithoutFallback,
                            content.info?.mimeType,
                            ContentType.Image.Any
                        ),
                        sender
                    )
                }

                is VideoMessageEventContent -> {
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
                    VideoReply(
                        thumbnail, fileNameComputations.getOrCreateFileName(
                            content.bodyWithoutFallback,
                            content.info?.mimeType,
                            ContentType.Video.Any
                        ), sender
                    )
                }

                is AudioMessageEventContent -> AudioReply(
                    fileNameComputations.getOrCreateFileName(
                        content.bodyWithoutFallback,
                        content.info?.mimeType,
                        ContentType.Audio.Any
                    ),
                    sender,
                )

                is FileMessageEventContent -> FileReply(
                    content.fileName ?: content.bodyWithoutFallback,
                    sender,
                )

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

    data class UnknownReply(
        override val senderName: String,
    ) : ReplyType
}

class PreviewReplyToViewModel : ReplyToViewModel {
    override val event = MessageEvent(
        content = TextMessageEventContent("Hello World!"),
        id = EventId("1"),
        sender = UserId("alice", "localhost"),
        roomId = RoomId("room1", "localhost"),
        originTimestamp = 0L,
    )
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
    override val event = MessageEvent(
        content = TextMessageEventContent("Hello World!"),
        id = EventId("1"),
        sender = UserId("alice", "localhost"),
        roomId = RoomId("room1", "localhost"),
        originTimestamp = 0L,
    )
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