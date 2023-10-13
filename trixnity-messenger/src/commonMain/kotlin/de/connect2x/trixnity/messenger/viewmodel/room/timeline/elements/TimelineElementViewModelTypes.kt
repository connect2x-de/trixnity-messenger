package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

interface BaseTimelineElementViewModel {
    val invitation: StateFlow<String?> // in case the element has the invitation element above
    val formattedDate: String // used for sticky header
    val showDateAbove: Boolean
}

class NullTimelineElementViewModel(
    viewModelContext: MatrixClientViewModelContext,
    invitation: Flow<String?>,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
) : BaseTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
}

interface TimelineElementWithTimestampViewModel : BaseTimelineElementViewModel {
    val formattedTime: String? // if null it is in the outbox
}

interface RoomMessageViewModel : TimelineElementWithTimestampViewModel {
    val isByMe: Boolean
    val showChatBubbleEdge: Boolean
    val showBigGap: Boolean
    val showSender: StateFlow<Boolean>
    val sender: StateFlow<String>
}

interface TextBasedViewModel : RoomMessageViewModel {
    /**
     * Used when rendering of [referencedMessage] and [message] is too complicated or infeasible.
     */
    val fallbackMessage: String

    /**
     * Can be a message that is replied to or a thread.
     */
    val referencedMessage: StateFlow<ReferencedMessage?>

    /**
     * This event's message (stripped of any fallbacks for rich replies).
     */
    val message: String
}

sealed interface ReferencedMessage {
    val sender: String

    data class ReferencedTextMessage(
        override val sender: String,
        val message: String,
    ) : ReferencedMessage {
        fun messageShortened(maxLines: Int = 4, ellipsis: String = "..."): String {
            val chunks = message
                .lines()
                .chunked(maxLines)
            return if (chunks.size > 1 && chunks[1].firstOrNull()?.isNotBlank() == true) {
                (chunks[0].dropLast(1) + "$ellipsis").joinToString("\n")
            } else message
        }
    }

    data class ReferencedImageMessage(
        override val sender: String,
        val thumbnail: ByteArray?,
        val fileName: String,
    ) : ReferencedMessage

    data class ReferencedVideoMessage(
        override val sender: String,
        val thumbnail: ByteArray?,
        val fileName: String,
    ) : ReferencedMessage

    data class ReferencedAudioMessage(
        override val sender: String,
        val fileName: String,
    ) : ReferencedMessage

    data class ReferencedFileMessage(
        override val sender: String,
        val fileName: String,
    ) : ReferencedMessage

    data class ReferencedUnknownMessage(
        override val sender: String,
    ) : ReferencedMessage
}
