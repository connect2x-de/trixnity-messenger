@file:OptIn(MSC2448::class)

package de.connect2x.messenger.compose.view.previews

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.messenger.compose.view.previews.util.InitMessengerPreview
import de.connect2x.messenger.compose.view.room.timeline.element.message.FileRoomMessageTimelineElementViewImpl
import de.connect2x.messenger.compose.view.room.timeline.element.message.ImageRoomMessageTimelineElementViewImpl
import de.connect2x.messenger.compose.view.room.timeline.element.message.TextBasedRoomMessageTimelineElementView
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.util.html.HtmlNode
import de.connect2x.trixnity.messenger.util.html.HtmlVisitor
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.PreviewTimelineElementViewModel1
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.EventReaction
import de.connect2x.trixnity.messenger.viewmodel.util.EventReactions
import de.connect2x.trixnity.messenger.viewmodel.util.previewImageByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.core.MSC2448
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArray

@SuppressLint("StateFlowValueCalledInComposition")
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
    holder.reactions.value =
        EventReactions(
            setOf(
                EventReaction(
                    value = "x",
                    eventId = EventId("1"),
                    sender = userInfoElement,
                    isByMe = false,
                )
            )
        )

    val element = object : RoomMessageTimelineElementViewModel.TextBased.Text {
        override val body: String = "Hello everyone!"
        override val formattedBody: String = "Hello <b/>everyone!"
        override val formattedBodyContent: HtmlNode.HtmlElement? = HtmlVisitor.process(formattedBody)
        override val mentionsInBody: Map<IntRange, MutableStateFlow<TimelineElementMention>> = mapOf()
        override val mentionsInFormattedBody: StateFlow<Map<String, TimelineElementMention?>> =
            MutableStateFlow(mapOf())

        override fun openMention(mention: TimelineElementMention) {}
    }
    InitMessengerPreview {
        TextBasedRoomMessageTimelineElementView(
            holder,
            element,
            isPreview = false,
            index = 0,
        )
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
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
        override val thumbnailWidth: Int? = 40
        override val thumbnailHeight: Int? = 40
        override val blurhash: String? = null
        override val thumbnailLoading: StateFlow<Boolean> = MutableStateFlow(false)

        override val name: String = "kiwi.png"
        override val size: String? = "465kb"
        override val mimeType: String? = "image/png"
        override val hasCaption: Boolean = true
        override val loadMediaResultPlatformMedia: StateFlow<PlatformMedia?> =
            MutableStateFlow(InMemoryPlatformMedia(flowOf(previewImageByteArray())))
        override val loadMediaResultBytes: StateFlow<ByteArray?> = MutableStateFlow(previewImageByteArray())

        @Deprecated(
            "This will be removed in the future for consistency with downloadMedia behaviour, please use loadMediaResultBytes instead",
            replaceWith = ReplaceWith("loadMediaResultBytes")
        )
        override val loadMediaResult: StateFlow<ByteArray?> = loadMediaResultBytes
        override val loadMediaProgress: StateFlow<FileTransferProgressElement?> = MutableStateFlow(null)
        override val loadMediaError: StateFlow<String?> = MutableStateFlow(null)
        override fun loadMedia() {}
        override fun cancelLoadMedia() {}
        override val downloadMediaResult: StateFlow<PlatformMedia?> =
            MutableStateFlow(InMemoryPlatformMedia(flowOf(previewImageByteArray())))
        override val downloadMediaProgress: StateFlow<FileTransferProgressElement?> = MutableStateFlow(null)
        override val downloadMediaError: StateFlow<String?> = MutableStateFlow(null)
        override fun downloadMedia(processFile: suspend (PlatformMedia) -> Unit, onDownloadCancelled: () -> Unit) {}
        override fun cancelDownloadMedia() {}
        override val body: String = "Ein Kiwi :D"
        override val formattedBody: String? = null
        override val formattedBodyContent: HtmlNode.HtmlElement? = null
        override val mentionsInBody: Map<IntRange, StateFlow<TimelineElementMention?>> = emptyMap()
        override val mentionsInFormattedBody: StateFlow<Map<String, TimelineElementMention?>> =
            MutableStateFlow(emptyMap())

        override fun openMention(mention: TimelineElementMention) {
        }
    }
    InitMessengerPreview {
        ImageRoomMessageTimelineElementViewImpl().createInTimeline(
            holder,
            element,
            0,
        )
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
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
        override val size: String? = "465kb"
        override val mimeType: String? = "text/plain"
        override val hasCaption: Boolean = true
        override val loadMediaResultPlatformMedia: StateFlow<PlatformMedia?> =
            MutableStateFlow(InMemoryPlatformMedia(flowOf("Kiwi".toByteArray())))
        override val loadMediaResultBytes: StateFlow<ByteArray?> = MutableStateFlow("Kiwi".toByteArray())

        @Deprecated(
            "This will be removed in the future for consistency with downloadMedia behaviour, please use loadMediaResultBytes instead",
            replaceWith = ReplaceWith("loadMediaResultBytes")
        )
        override val loadMediaResult: StateFlow<ByteArray?> = loadMediaResultBytes
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
        override fun downloadMedia(processFile: suspend (PlatformMedia) -> Unit, onDownloadCancelled: () -> Unit) {}
        override fun cancelDownloadMedia() {}
        override val body: String = "Ein Kiwi :D"
        override val formattedBody: String? = null
        override val formattedBodyContent: HtmlNode.HtmlElement? = null
        override val mentionsInBody: Map<IntRange, StateFlow<TimelineElementMention?>> = emptyMap()
        override val mentionsInFormattedBody: StateFlow<Map<String, TimelineElementMention?>> =
            MutableStateFlow(emptyMap())

        override fun openMention(mention: TimelineElementMention) {
        }
    }
    InitMessengerPreview {
        FileRoomMessageTimelineElementViewImpl().createInTimeline(
            holder,
            element,
            0,
        )
    }
}

class InMemoryPlatformMedia(private val delegate: ByteArrayFlow) : PlatformMedia,
    ByteArrayFlow by delegate {
    override fun transformByteArrayFlow(transformer: (ByteArrayFlow) -> ByteArrayFlow): PlatformMedia =
        InMemoryPlatformMedia(delegate.let(transformer))

    override suspend fun toByteArray(
        coroutineScope: CoroutineScope?,
        expectedSize: Long?,
        maxSize: Long?
    ): ByteArray? = delegate.toByteArray()
}
