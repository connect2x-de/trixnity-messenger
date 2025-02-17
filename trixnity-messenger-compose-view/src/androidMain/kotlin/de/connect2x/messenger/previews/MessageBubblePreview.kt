package de.connect2x.messenger.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.messenger.compose.view.room.timeline.element.message.FileRoomMessageTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.ImageRoomMessageTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.TextBasedRoomMessageTimelineElementView
import de.connect2x.messenger.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.PreviewTimelineElementViewModel1
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.MessageUserReactions.ReactionEvent
import de.connect2x.trixnity.messenger.viewmodel.util.previewImageByteArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.utils.ByteArrayFlow


@Preview
@Composable
fun TextMessageBubblePreview() {
    val holder = PreviewTimelineElementViewModel1()
    holder.showSender.value = true
    val userInfoElement = UserInfoElement(
        name = "Martin",
        userId = UserId("@martin:localhost"),
        initials = "M",
    )
    holder.sender.value = userInfoElement
    holder.isFirstInUserSequence.value = true
    holder.showBigGapBefore.value = true
    holder.reactions.value = mapOf(
        "🎉" to setOf(
            ReactionEvent(
                eventId = EventId("1"),
                sender = userInfoElement,
                isMe = false,
            )
        )
    )
    val element = object : RoomMessageTimelineElementViewModel.TextBased.Text {
        override val body: String = "Hello everyone!"
        override val formattedBody: String = "Hello <b/>everyone!"
        override val mentionsInBody: Map<IntRange, StateFlow<TimelineElementMention>> = mapOf()
        override val mentionsInFormattedBody: Map<IntRange, StateFlow<TimelineElementMention>> = mapOf()
        override fun openMention(timelineElementMention: TimelineElementMention) {}
    }
    InitMessengerPreview {
        TextBasedRoomMessageTimelineElementView(
            holder,
            element,
        )
    }
}

@Preview
@Composable
fun ImageMessageBubblePreview() {
    val holder = PreviewTimelineElementViewModel1()
    holder.showSender.value = true
    holder.sender.value = UserInfoElement(
        name = "Martin",
        userId = UserId("@martin:localhost"),
        initials = "M",
    )
    holder.isFirstInUserSequence.value = true
    holder.showBigGapBefore.value = true
    val element = object : RoomMessageTimelineElementViewModel.FileBased.Image {
        override val thumbnail: StateFlow<ByteArray?> = MutableStateFlow(previewImageByteArray())
        override val width: Int? = 40
        override val height: Int? = 40

        override val name: String = "kiwi.png"
        override val description: String? = null
        override val size: String? = "465kb"
        override val mimeType: String? = "image/png"
        override val loadMediaResult: StateFlow<ByteArray?> = MutableStateFlow(previewImageByteArray())
        override val loadMediaProgress: StateFlow<FileTransferProgressElement?> = MutableStateFlow(null)
        override val loadMediaError: StateFlow<String?> = MutableStateFlow(null)
        override fun loadMedia() {}
        override fun cancelLoadMedia() {}
        override val downloadMediaResult: StateFlow<PlatformMedia?> =
            MutableStateFlow(InMemoryPlatformMedia(flowOf(previewImageByteArray())))
        override val downloadMediaProgress: StateFlow<FileTransferProgressElement?> = MutableStateFlow(null)
        override val downloadMediaError: StateFlow<String?> = MutableStateFlow(null)
        override fun downloadMedia(processFile: suspend (PlatformMedia) -> Unit) {}
        override fun cancelDownloadMedia() {}
    }
    InitMessengerPreview {
        ImageRoomMessageTimelineElementView().createInTimeline(
            holder,
            element,
        )
    }
}

@Preview
@Composable
fun FileMessageBubblePreview() {
    val holder = PreviewTimelineElementViewModel1()
    holder.showSender.value = true
    holder.sender.value = UserInfoElement(
        name = "Martin",
        userId = UserId("@martin:localhost"),
        initials = "M",
    )
    holder.isFirstInUserSequence.value = true
    holder.showBigGapBefore.value = true
    val element = object : RoomMessageTimelineElementViewModel.FileBased.File {
        override val name: String = "kiwi.txt"
        override val description: String? = "A file."
        override val size: String? = "465kb"
        override val mimeType: String? = "text/plain"
        override val loadMediaResult: StateFlow<ByteArray?> = MutableStateFlow("Kiwi".toByteArray())
        override val loadMediaProgress: StateFlow<FileTransferProgressElement?> = MutableStateFlow(null)
        override val loadMediaError: StateFlow<String?> = MutableStateFlow(null)
        override fun loadMedia() {}
        override fun cancelLoadMedia() {}
        override val downloadMediaResult: StateFlow<PlatformMedia?> =
            MutableStateFlow(InMemoryPlatformMedia(flowOf("Kiwi".toByteArray())))
        override val downloadMediaProgress: StateFlow<FileTransferProgressElement?> = MutableStateFlow(
            FileTransferProgressElement(0.33f, "280kb/465")
        )
        override val downloadMediaError: StateFlow<String?> = MutableStateFlow(null)
        override fun downloadMedia(processFile: suspend (PlatformMedia) -> Unit) {}
        override fun cancelDownloadMedia() {}
    }
    InitMessengerPreview {
        FileRoomMessageTimelineElementView().createInTimeline(
            holder,
            element,
        )
    }
}

class InMemoryPlatformMedia(private val delegate: ByteArrayFlow) : PlatformMedia,
    ByteArrayFlow by delegate {
    override fun transformByteArrayFlow(transformer: (ByteArrayFlow) -> ByteArrayFlow): PlatformMedia =
        InMemoryPlatformMedia(delegate.let(transformer))
}
