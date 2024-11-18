package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel.Message
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.ByteArrayFlow

sealed interface RoomMessageTimelineElementViewModel<C : RoomMessageEventContent> : Message<C> {
    sealed interface TextBased<C : RoomMessageEventContent.TextBased> : RoomMessageTimelineElementViewModel<C> {
        /**
         * This event's message (stripped of any fallbacks for rich replies).
         */
        val body: String

        /**
         * The HTML version of the message, if present. [spec](https://spec.matrix.org/v1.7/client-server-api/#mroommessage-msgtypes)
         */
        val formattedBody: String?

        /**
         * Users, Events and Room mentioned in the event's message
         */
        val mentionsInBody: Map<IntRange, StateFlow<TimelineElementMention?>>

        /**
         * Users, Events and Room mentioned in the event's formatted body
         */
        val mentionsInFormattedBody: Map<IntRange, StateFlow<TimelineElementMention?>>?

        /**
         * Open the mention in the UI
         */
        fun openMention(mention: TimelineElementMention)

        interface Text : TextBased<RoomMessageEventContent.TextBased.Text>
        interface Notice : TextBased<RoomMessageEventContent.TextBased.Notice>
        interface Emote : TextBased<RoomMessageEventContent.TextBased.Emote>
    }

    sealed interface FileBased<C : RoomMessageEventContent.FileBased> : RoomMessageTimelineElementViewModel<C> {
        val name: String
        val description: String?
        val size: String?
        val mimeType: String?

        /**
         * Usually opens a full screen preview.
         */
        fun open()

        val downloadProgress: StateFlow<FileTransferProgressElement?>
        val downloadSuccessful: StateFlow<Boolean?>
        val downloadError: StateFlow<String?>
        fun download(processFile: suspend (ByteArrayFlow) -> Unit)
        fun cancelDownload()

        interface File : FileBased<RoomMessageEventContent.FileBased.File>

        interface Image : FileBased<RoomMessageEventContent.FileBased.Image> {
            val thumbnail: StateFlow<ByteArray?>
            val width: Int?
            val height: Int?
            fun getDimensions(maxWidth: Int, maxHeight: Int): Pair<Int, Int>?
        }

        interface Audio : FileBased<RoomMessageEventContent.FileBased.Audio> {
            val duration: Long?
        }

        interface Video : FileBased<RoomMessageEventContent.FileBased.Video> {
            val duration: Long?
            val thumbnail: StateFlow<ByteArray?>
            val width: Int?
            val height: Int?
            fun getDimensions(maxWidth: Int, maxHeight: Int): Pair<Int, Int>?
        }
    }

    interface Location : RoomMessageTimelineElementViewModel<RoomMessageEventContent.Location> {
        val name: String
        val geoUri: String
    }

    interface VerificationRequest : RoomMessageTimelineElementViewModel<RoomMessageEventContent.VerificationRequest> {
        val isActive: StateFlow<Boolean?>
        val reachedEndState: StateFlow<Pair<Boolean, String>?>
        val stack: Value<ChildStack<VerificationRouter.Config, VerificationRouter.Wrapper>>
        fun cancel()
    }

    interface Unknown : RoomMessageTimelineElementViewModel<RoomMessageEventContent.Unknown> {
        val fallbackBody: String
    }
}
