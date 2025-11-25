package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.util.MatrixReferences
import de.connect2x.trixnity.messenger.util.html.AutoLinkifyVisitor
import de.connect2x.trixnity.messenger.util.html.HtmlNode
import de.connect2x.trixnity.messenger.util.html.HtmlVisitor
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.media.AudioPlayerViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.MentionHelper
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel.Message
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.core.MSC2448
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.bodyWithoutFallback
import net.folivo.trixnity.core.model.events.m.room.formattedBodyWithoutFallback
import org.koin.core.component.get

sealed interface RoomMessageTimelineElementViewModel<C : RoomMessageEventContent> : Message<C> {
    /**
     * This event's message (stripped of any fallbacks for rich replies).
     */
    val body: String

    /**
     * The HTML version of the message, if present. [spec](https://spec.matrix.org/v1.7/client-server-api/#mroommessage-msgtypes)
     */
    val formattedBody: String?

    /**
     * The HTML version of the message as a tree of HTML nodes, if present.
     */
    val formattedBodyContent: HtmlNode.HtmlElement?

    /**
     * Users, Events and Room mentioned in the event's message
     */
    val mentionsInBody: Map<IntRange, StateFlow<TimelineElementMention?>> // TODO typing should be consistent to mentionsInFormattedBody

    /**
     * Users, Events and Room mentioned in the event's formatted body
     */
    val mentionsInFormattedBody: StateFlow<Map<String, TimelineElementMention?>>

    /**
     * Open the mention in the UI
     */
    fun openMention(mention: TimelineElementMention)

    interface TextBased<C : RoomMessageEventContent> : RoomMessageTimelineElementViewModel<C> {
        interface Text : TextBased<RoomMessageEventContent.TextBased.Text>
        interface Notice : TextBased<RoomMessageEventContent.TextBased.Notice>
        interface Emote : TextBased<RoomMessageEventContent.TextBased.Emote>
    }

    interface FileBased<C : RoomMessageEventContent.FileBased> : RoomMessageTimelineElementViewModel<C> {
        val name: String
        val size: String?
        val mimeType: String?

        /**
         * If true, [body] and [formattedBody] contain the files caption
         */
        val hasCaption: Boolean

        @Deprecated(
            "This will be removed in the future for consistency with downloadMedia behaviour, please use loadMediaResultBytes instead",
            replaceWith = ReplaceWith("loadMediaResultBytes")
        )
        val loadMediaResult: StateFlow<ByteArray?>
        val loadMediaResultPlatformMedia: StateFlow<PlatformMedia?>
        val loadMediaResultBytes: StateFlow<ByteArray?>
        val loadMediaProgress: StateFlow<FileTransferProgressElement?>
        val loadMediaError: StateFlow<String?>
        fun loadMedia()
        fun cancelLoadMedia()

        val downloadMediaResult: StateFlow<PlatformMedia?>
        val downloadMediaProgress: StateFlow<FileTransferProgressElement?>
        val downloadMediaError: StateFlow<String?>
        fun downloadMedia(processFile: suspend (PlatformMedia) -> Unit = { })
        fun cancelDownloadMedia()

        interface File : FileBased<RoomMessageEventContent.FileBased.File>

        interface Image : FileBased<RoomMessageEventContent.FileBased.Image> {
            val thumbnail: StateFlow<ByteArray?>
            val thumbnailLoading: StateFlow<Boolean>
            val width: Int?
            val height: Int?
            val thumbnailWidth: Int?
            val thumbnailHeight: Int?

            @MSC2448
            val blurhash: String?
        }

        interface Audio : FileBased<RoomMessageEventContent.FileBased.Audio> {
            val duration: Long?
            val audioPlayer: AudioPlayerViewModel?
        }

        interface Video : FileBased<RoomMessageEventContent.FileBased.Video> {
            val duration: Long?
            val thumbnail: StateFlow<ByteArray?>
            val width: Int?
            val height: Int?
        }
    }

    interface Location : RoomMessageTimelineElementViewModel<RoomMessageEventContent.Location> {
        val name: String
        val geoUri: String
    }

    interface VerificationRequest : RoomMessageTimelineElementViewModel<RoomMessageEventContent.VerificationRequest> {
        val isActive: StateFlow<Boolean?>
        val stack: Value<ChildStack<VerificationRouter.Config, VerificationRouter.Wrapper>>
        fun cancel()
    }

    interface Unknown : RoomMessageTimelineElementViewModel<RoomMessageEventContent.Unknown> {
        val fallbackBody: String
    }
}

// This only implements common functionality for the actual classes
@Suppress("unused")
abstract class RoomMessageTimelineElementViewModelImpl<C : RoomMessageEventContent>(
    private val viewModelContext: MatrixClientViewModelContext,
    content: C,
    private val roomId: RoomId,
    private val onOpenMention: OpenMentionCallback,
) : MatrixClientViewModelContext by viewModelContext { // Do not inherit from RoomMessageTimelineElementViewModel to simplify pattern matching, etc.
    private val mentionHelper = MentionHelper(
        coroutineScope,
        matrixClient,
        roomId,
        get<Initials>(),
        get<RoomName>(),
        get<MatrixMessengerConfiguration>().maxMediaSizeInMemory,
    )

    val body: String = content.bodyWithoutFallback
    val formattedBody: String? = content.formattedBodyWithoutFallback
    val formattedBodyContent: HtmlNode.HtmlElement =
        content.formattedBodyWithoutFallback
            ?.let(HtmlVisitor::process)
            ?.let(AutoLinkifyVisitor::process)
            ?: HtmlNode.HtmlElement("#root", emptyMap(), listOf(HtmlNode.TextContent(content.body)))
                .let(AutoLinkifyVisitor::process)

    val mentionsInBody: Map<IntRange, StateFlow<TimelineElementMention?>> by lazy {
        MatrixReferences.findInText(body)
            .mapValues { (_, mention) -> mentionHelper.processMention(mention) }
    }

    val mentionsInFormattedBody: StateFlow<Map<String, TimelineElementMention?>> =
        mentionHelper.processMentions(formattedBodyContent)
            .stateIn(coroutineScope, SharingStarted.Eagerly, emptyMap())

    fun openMention(mention: TimelineElementMention) {
        onOpenMention(userId, mention)
    }
}
