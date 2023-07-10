package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.ComponentContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.RichRepliesComputations
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.timezone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.store.RoomOutboxMessage
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.*
import net.folivo.trixnity.core.model.events.m.room.bodyWithoutFallback
import org.koin.core.component.get

interface OutboxElementViewModelFactory {
    fun newOutboxElementViewModel(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        outboxMessage: RoomOutboxMessage<*>,
        showDateAbove: Boolean,
        showChatBubbleEdge: Boolean,
        onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String) -> Unit,
    ): OutboxElementViewModel {
        return OutboxElementViewModelImpl(
            viewModelContext,
            selectedRoomId,
            outboxMessage,
            showDateAbove,
            showChatBubbleEdge,
            onOpenModal,
        )
    }
}

interface OutboxElementViewModel : ITimelineElementViewModel, ComponentContext

open class OutboxElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    outboxMessage: RoomOutboxMessage<*>,
    showDateAbove: Boolean,
    showChatBubbleEdge: Boolean,
    onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String) -> Unit,
) : MatrixClientViewModelContext by viewModelContext, OutboxElementViewModel {

    private val richRepliesComputations = get<RichRepliesComputations>()

    override val eventId: EventId? = null // does not have an EventId yet

    override val shouldShowUnreadMarkerFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLoadingIndicatorBefore: StateFlow<Boolean> = MutableStateFlow(false)
    override val showLoadingIndicatorAfter: StateFlow<Boolean> = MutableStateFlow(false)

    override val canBeEdited: StateFlow<Boolean> = MutableStateFlow(false)
    override val canBeRedacted: StateFlow<Boolean> = MutableStateFlow(false)
    override val redactionInProgress: StateFlow<Boolean> = MutableStateFlow(false)
    override val redactionError: StateFlow<String?> = MutableStateFlow(null)
    override val canBeRepliedTo: StateFlow<Boolean> = MutableStateFlow(false)
    override val highlight: StateFlow<Boolean> = MutableStateFlow(false)
    override fun edit() {
    }

    override fun endEdit() {
    }

    override fun redact() {
        // TODO should we allow redacting messages before sending?
    }

    override fun replyTo() {
        // not possible to reply to your outbox messages
    }

    override fun endReplyTo() {
    }

    private val clock = get<Clock>()
    override val viewModel: MutableStateFlow<BaseTimelineElementViewModel?> = MutableStateFlow(
        kotlin.run {
            val content = outboxMessage.content
            val formattedDate = formatDate(clock.now().toLocalDateTime(TimeZone.of(timezone())))
            when (content) {
                is TextMessageEventContent -> {
                    get<TextMessageViewModelFactory>().newTextMessageViewModel(
                        viewModelContext = this,
                        fallbackMessage = content.body,
                        referencedMessage = richRepliesComputations.getReferencedMessage(
                            matrixClient,
                            content.relatesTo,
                            selectedRoomId
                        ).stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null),
                        message = content.bodyWithoutFallback,
                        sender = MutableStateFlow(""),
                        showSender = MutableStateFlow(false),
                        formattedDate = formattedDate,
                        formattedTime = null,
                        showDateAbove = showDateAbove,
                        isByMe = true,
                        showChatBubbleEdge = showChatBubbleEdge,
                        showBigGap = showChatBubbleEdge,
                        invitation = MutableStateFlow(null),
                    )
                }

                is ImageMessageEventContent -> {
                    get<ImageMessageViewModelFactory>().newImageMessageViewModel(
                        viewModelContext = this,
                        sender = MutableStateFlow(""),
                        showSender = MutableStateFlow(false),
                        formattedDate = formattedDate,
                        formattedTime = null,
                        showDateAbove = showDateAbove,
                        isByMe = true,
                        showChatBubbleEdge = showChatBubbleEdge,
                        showBigGap = showChatBubbleEdge,
                        invitation = MutableStateFlow(null),
                        content = content,
                        onOpenModal = onOpenModal,
                        transactionId = outboxMessage.transactionId,
                        mediaUploadProgress = outboxMessage.mediaUploadProgress,
                    )
                }

                is VideoMessageEventContent -> {
                    get<VideoMessageViewModelFactory>().newVideoMessageViewModel(
                        viewModelContext = this,
                        sender = MutableStateFlow(""),
                        showSender = MutableStateFlow(false),
                        formattedDate = formattedDate,
                        formattedTime = null,
                        showDateAbove = showDateAbove,
                        isByMe = true,
                        showChatBubbleEdge = showChatBubbleEdge,
                        showBigGap = showChatBubbleEdge,
                        invitation = MutableStateFlow(null),
                        content = content,
                        onOpenModal = onOpenModal,
                    )
                }

                is AudioMessageEventContent -> {
                    get<AudioMessageViewModelFactory>().newAudioMessageViewModel(
                        viewModelContext = this,
                        sender = MutableStateFlow(""),
                        showSender = MutableStateFlow(false),
                        formattedDate = formattedDate,
                        formattedTime = null,
                        showDateAbove = showDateAbove,
                        isByMe = true,
                        showChatBubbleEdge = showChatBubbleEdge,
                        showBigGap = showChatBubbleEdge,
                        invitation = MutableStateFlow(null),
                        content = content,
                        onOpenModal = onOpenModal,
                    )
                }

                is FileMessageEventContent -> {
                    get<FileMessageViewModelFactory>().newFileMessageViewModel(
                        viewModelContext = this,
                        formattedDate = formattedDate,
                        showDateAbove = showDateAbove,
                        formattedTime = null,
                        isByMe = true,
                        showChatBubbleEdge = showChatBubbleEdge,
                        showBigGap = showChatBubbleEdge,
                        showSender = MutableStateFlow(false),
                        sender = MutableStateFlow(""),
                        invitation = MutableStateFlow(null),
                        content = content,
                    )
                }

                else -> createNullTimelineElementViewModel()
            }
        }
    )

    private fun createNullTimelineElementViewModel() =
        NullTimelineElementViewModel(
            viewModelContext = this,
            formattedDate = "",
            showDateAbove = false,
            invitation = MutableStateFlow(null),
        )
}