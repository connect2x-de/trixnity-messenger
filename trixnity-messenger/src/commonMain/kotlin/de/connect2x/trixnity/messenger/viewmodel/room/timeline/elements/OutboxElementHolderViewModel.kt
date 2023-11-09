package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.RichRepliesComputations
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.RoomOutboxMessage
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.*
import net.folivo.trixnity.core.model.events.m.room.bodyWithoutFallback
import org.koin.core.component.get

interface OutboxElementHolderViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        key: String,
        outboxMessageFlow: Flow<RoomOutboxMessage<*>?>,
        selectedRoomId: RoomId,
        transactionId: String,
        showDateAboveFlow: Flow<Boolean>,
        showChatBubbleEdgeFlow: Flow<Boolean>,
        onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String) -> Unit,
    ): OutboxElementHolderViewModel {
        return OutboxElementHolderViewModelImpl(
            viewModelContext,
            key,
            outboxMessageFlow,
            selectedRoomId,
            transactionId,
            showDateAboveFlow,
            showChatBubbleEdgeFlow,
            onOpenModal
        )
    }

    companion object : OutboxElementHolderViewModelFactory
}

interface OutboxElementHolderViewModel : BaseTimelineElementHolderViewModel {
    val transactionId: String
    val sendError: StateFlow<String?>
    val canRetrySend: StateFlow<Boolean>
    val canAbortSend: StateFlow<Boolean>

    fun retrySend()
    fun abortSend()
}

open class OutboxElementHolderViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val key: String,
    outboxMessageFlow: Flow<RoomOutboxMessage<*>?>,
    private val selectedRoomId: RoomId,
    override val transactionId: String,
    showDateAboveFlow: Flow<Boolean>,
    showChatBubbleEdgeFlow: Flow<Boolean>,
    onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String) -> Unit,
) : MatrixClientViewModelContext by viewModelContext, OutboxElementHolderViewModel {

    private val richRepliesComputations = get<RichRepliesComputations>()
    private val i18n = get<I18n>()

    override val timelineElementViewModel: StateFlow<BaseTimelineElementViewModel?> =
        combine(
            outboxMessageFlow,
            showDateAboveFlow,
            showChatBubbleEdgeFlow
        ) { outboxMessage, showDateAbove, showChatBubbleEdge ->
            val content = outboxMessage?.content
            when (content) {
                is TextMessageEventContent -> {
                    get<TextMessageViewModelFactory>().create(
                        viewModelContext = this,
                        fallbackMessage = content.body,
                        referencedMessage = richRepliesComputations.getReferencedMessage(
                            matrixClient,
                            content.relatesTo,
                            selectedRoomId
                        ).stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null),
                        message = content.bodyWithoutFallback,
                        formattedBody = content.formattedBody,
                        sender = MutableStateFlow(""),
                        showSender = MutableStateFlow(false),
                        formattedDate = "",
                        formattedTime = null,
                        showDateAbove = showDateAbove,
                        isByMe = true,
                        showChatBubbleEdge = showChatBubbleEdge,
                        showBigGap = showChatBubbleEdge,
                        invitation = MutableStateFlow(null),
                    )
                }

                is ImageMessageEventContent -> {
                    get<ImageMessageViewModelFactory>().create(
                        viewModelContext = this,
                        sender = MutableStateFlow(""),
                        showSender = MutableStateFlow(false),
                        formattedDate = "",
                        formattedTime = null,
                        showDateAbove = showDateAbove,
                        isByMe = true,
                        showChatBubbleEdge = showChatBubbleEdge,
                        showBigGap = showChatBubbleEdge,
                        invitation = MutableStateFlow(null),
                        content = content,
                        onOpenModal = onOpenModal,
                        mediaUploadProgress = outboxMessage.mediaUploadProgress,
                    )
                }

                is VideoMessageEventContent -> {
                    get<VideoMessageViewModelFactory>().create(
                        viewModelContext = this,
                        sender = MutableStateFlow(""),
                        showSender = MutableStateFlow(false),
                        formattedDate = "",
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
                    get<AudioMessageViewModelFactory>().create(
                        viewModelContext = this,
                        sender = MutableStateFlow(""),
                        showSender = MutableStateFlow(false),
                        formattedDate = "",
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
                    get<FileMessageViewModelFactory>().create(
                        viewModelContext = this,
                        formattedDate = "",
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
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private fun createNullTimelineElementViewModel() =
        NullTimelineElementViewModel(
            viewModelContext = this,
            formattedDate = "",
            showDateAbove = false,
            invitation = MutableStateFlow(null),
        )

    override val sendError: StateFlow<String?> = outboxMessageFlow.map {
        when (val sendError = it?.sendError) {
            RoomOutboxMessage.SendError.NoEventPermission -> i18n.sendErrorEventPermission()
            RoomOutboxMessage.SendError.NoMediaPermission -> i18n.sendErrorMediaPermission()
            RoomOutboxMessage.SendError.MediaTooLarge -> i18n.sendErrorMediaTooLarge()
            is RoomOutboxMessage.SendError.BadRequest -> i18n.sendErrorUnknown(sendError.errorResponse.error)
            is RoomOutboxMessage.SendError.Unknown -> i18n.sendErrorUnknown(sendError.errorResponse.error)
            null -> null
        }
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val canAbortSend: StateFlow<Boolean> = MutableStateFlow(true)
    override val canRetrySend: StateFlow<Boolean> = outboxMessageFlow.map { it?.sendError != null }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override fun abortSend() {
        coroutineScope.launch {
            matrixClient.room.abortSendMessage(transactionId)
        }
    }

    override fun retrySend() {
        coroutineScope.launch {
            matrixClient.room.retrySendMessage(transactionId)
        }
    }
}