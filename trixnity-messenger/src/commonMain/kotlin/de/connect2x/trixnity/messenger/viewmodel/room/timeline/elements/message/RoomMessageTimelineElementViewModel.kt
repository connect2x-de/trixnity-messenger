package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.messenger.util.html.HtmlNode
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel.Message
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file.FileBasedTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter
import kotlinx.coroutines.flow.StateFlow

sealed interface RoomMessageTimelineElementViewModel<C : RoomMessageEventContent> : Message<C> {
    /** This event's message (stripped of any fallbacks for rich replies). */
    val body: String

    /**
     * The HTML version of the message, if present.
     * [spec](https://spec.matrix.org/v1.7/client-server-api/#mroommessage-msgtypes)
     */
    val formattedBody: String?

    /** The HTML version of the message as a tree of HTML nodes, if present. */
    val formattedBodyContent: HtmlNode.HtmlElement?

    /** Users, Events and Room mentioned in the event's message */
    val mentionsInBody:
        Map<IntRange, StateFlow<TimelineElementMention?>> // TODO typing should be consistent to mentionsInFormattedBody

    /** Users, Events and Room mentioned in the event's formatted body */
    val mentionsInFormattedBody: StateFlow<Map<String, TimelineElementMention?>>

    /** Open the mention in the UI */
    fun openMention(mention: TimelineElementMention)

    suspend fun loadInlineImage(uri: String, width: Int, height: Int): ByteArray?

    interface TextBased<C : RoomMessageEventContent> : RoomMessageTimelineElementViewModel<C> {
        interface Text : TextBased<RoomMessageEventContent.TextBased.Text>

        interface Notice : TextBased<RoomMessageEventContent.TextBased.Notice>

        interface Emote : TextBased<RoomMessageEventContent.TextBased.Emote>
    }

    interface FileBased<C : RoomMessageEventContent.FileBased> :
        RoomMessageTimelineElementViewModel<C>, FileBasedTimelineElementViewModel {

        /** If true, [body] and [formattedBody] contain the files caption */
        val hasCaption: Boolean

        @Deprecated(
            "This will be removed in the future for consistency with downloadMedia behaviour, please use loadMediaResultBytes instead",
            replaceWith = ReplaceWith("loadMediaResultBytes"),
        )
        val loadMediaResult: StateFlow<ByteArray?>
            get() = loadMediaResultBytes

        interface File : FileBased<RoomMessageEventContent.FileBased.File>, FileBasedTimelineElementViewModel.File

        interface Image : FileBased<RoomMessageEventContent.FileBased.Image>, FileBasedTimelineElementViewModel.Image

        interface Audio : FileBased<RoomMessageEventContent.FileBased.Audio>, FileBasedTimelineElementViewModel.Audio

        interface Video : FileBased<RoomMessageEventContent.FileBased.Video>, FileBasedTimelineElementViewModel.Video
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
