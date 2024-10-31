package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.RichRepliesComputations
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.RoomOutboxMessage
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.Location
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.Unknown
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.VerificationRequest
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
        onOpenModal: OpenModalCallback,
        onOpenMention: OpenMentionCallback,
    ): OutboxElementHolderViewModel {
        return OutboxElementHolderViewModelImpl(
            viewModelContext,
            key,
            outboxMessageFlow,
            selectedRoomId,
            transactionId,
            showDateAboveFlow,
            showChatBubbleEdgeFlow,
            onOpenModal,
            onOpenMention
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
    onOpenModal: OpenModalCallback,
    onOpenMention: OpenMentionCallback,
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
            val creationTime = outboxMessage?.createdAt?.toLocalDateTime(viewModelContext.get())
            if (content is RoomMessageEventContent)
                when (content) {
                    is TextBased.Notice -> {
                        get<NoticeMessageViewModelFactory>().create(
                            viewModelContext = this,
                            timelineEvent = null,
                            content = content,
                            fallbackMessage = content.body,
                            referencedMessage = richRepliesComputations.getReferencedMessage(
                                matrixClient,
                                content.relatesTo,
                                selectedRoomId
                            ).stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null),
                            message = content.bodyWithoutFallback,
                            formattedBody = content.formattedBody,
                            sender = MutableStateFlow(UserInfoElement("", UserId(""))),
                            showSender = MutableStateFlow(false),
                            formattedDate = creationTime?.let { formatDate(it) } ?: "",
                            formattedTime = creationTime?.let { formatTime(it) },
                            showDateAbove = showDateAbove,
                            isByMe = true,
                            showChatBubbleEdge = showChatBubbleEdge,
                            showBigGap = showChatBubbleEdge,
                            invitation = MutableStateFlow(null),
                            roomId = selectedRoomId,
                            onOpenMention = onOpenMention,
                        )
                    }

                    is TextBased.Emote -> {
                        get<EmoteMessageViewModelFactory>().create(
                            viewModelContext = this,
                            timelineEvent = null,
                            content = content,
                            fallbackMessage = content.body,
                            referencedMessage = richRepliesComputations.getReferencedMessage(
                                matrixClient,
                                content.relatesTo,
                                selectedRoomId
                            ).stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null),
                            message = content.bodyWithoutFallback,
                            formattedBody = content.formattedBody,
                            sender = MutableStateFlow(UserInfoElement("", UserId(""))),
                            showSender = MutableStateFlow(false),
                            formattedDate = creationTime?.let { formatDate(it) } ?: "",
                            formattedTime = creationTime?.let { formatTime(it) },
                            showDateAbove = showDateAbove,
                            isByMe = true,
                            showChatBubbleEdge = showChatBubbleEdge,
                            showBigGap = showChatBubbleEdge,
                            invitation = MutableStateFlow(null),
                            roomId = selectedRoomId,
                            onOpenMention = onOpenMention,
                        )
                    }

                    is TextBased.Text -> {
                        get<TextMessageViewModelFactory>().create(
                            viewModelContext = this,
                            timelineEvent = null,
                            content = content,
                            fallbackMessage = content.body,
                            referencedMessage = richRepliesComputations.getReferencedMessage(
                                matrixClient,
                                content.relatesTo,
                                selectedRoomId
                            ).stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null),
                            message = content.bodyWithoutFallback,
                            formattedBody = content.formattedBody,
                            sender = MutableStateFlow(UserInfoElement("", UserId(""))),
                            showSender = MutableStateFlow(false),
                            formattedDate = creationTime?.let { formatDate(it) } ?: "",
                            formattedTime = creationTime?.let { formatTime(it) },
                            showDateAbove = showDateAbove,
                            isByMe = true,
                            showChatBubbleEdge = showChatBubbleEdge,
                            showBigGap = showChatBubbleEdge,
                            invitation = MutableStateFlow(null),
                            roomId = selectedRoomId,
                            onOpenMention = onOpenMention,
                        )
                    }

                    is FileBased.Image -> {
                        get<ImageMessageViewModelFactory>().create(
                            viewModelContext = this,
                            timelineEvent = null,
                            content = content,
                            formattedDate = creationTime?.let { formatDate(it) } ?: "",
                            showDateAbove = showDateAbove,
                            formattedTime = creationTime?.let { formatTime(it) },
                            isByMe = true,
                            showChatBubbleEdge = showChatBubbleEdge,
                            showBigGap = showChatBubbleEdge,
                            showSender = MutableStateFlow(false),
                            sender = MutableStateFlow(UserInfoElement("", UserId(""))),
                            invitation = MutableStateFlow(null),
                            onOpenModal = onOpenModal,
                            mediaUploadProgress = outboxMessage.mediaUploadProgress,
                        )
                    }

                    is FileBased.Video -> {
                        get<VideoMessageViewModelFactory>().create(
                            viewModelContext = this,
                            timelineEvent = null,
                            content = content,
                            sender = MutableStateFlow(UserInfoElement("", UserId(""))),
                            showSender = MutableStateFlow(false),
                            formattedDate = creationTime?.let { formatDate(it) } ?: "",
                            formattedTime = creationTime?.let { formatTime(it) },
                            showDateAbove = showDateAbove,
                            isByMe = true,
                            showChatBubbleEdge = showChatBubbleEdge,
                            showBigGap = showChatBubbleEdge,
                            invitation = MutableStateFlow(null),
                            onOpenModal = onOpenModal,
                            mediaUploadProgress = outboxMessage.mediaUploadProgress
                        )
                    }

                    is FileBased.Audio -> {
                        get<AudioMessageViewModelFactory>().create(
                            viewModelContext = this,
                            timelineEvent = null,
                            content = content,
                            sender = MutableStateFlow(UserInfoElement("", UserId(""))),
                            showSender = MutableStateFlow(false),
                            formattedDate = creationTime?.let { formatDate(it) } ?: "",
                            formattedTime = creationTime?.let { formatTime(it) },
                            showDateAbove = showDateAbove,
                            isByMe = true,
                            showChatBubbleEdge = showChatBubbleEdge,
                            showBigGap = showChatBubbleEdge,
                            invitation = MutableStateFlow(null),
                            onOpenModal = onOpenModal,
                            mediaUploadProgress = outboxMessage.mediaUploadProgress
                        )
                    }

                    is FileBased.File -> {
                        get<FileMessageViewModelFactory>().create(
                            viewModelContext = this,
                            timelineEvent = null,
                            content = content,
                            formattedDate = creationTime?.let { formatDate(it) } ?: "",
                            showDateAbove = showDateAbove,
                            formattedTime = creationTime?.let { formatTime(it) },
                            isByMe = true,
                            showChatBubbleEdge = showChatBubbleEdge,
                            showBigGap = showChatBubbleEdge,
                            showSender = MutableStateFlow(false),
                            sender = MutableStateFlow(UserInfoElement("", UserId(""))),
                            invitation = MutableStateFlow(null),
                            mediaUploadProgress = outboxMessage.mediaUploadProgress,
                            onOpenModal = onOpenModal,
                        )
                    }

                    is Location -> {
                        get<LocationMessageViewModelFactory>().create(
                            viewModelContext = this,
                            timelineEvent = null,
                            content = content,
                            formattedDate = creationTime?.let { formatDate(it) } ?: "",
                            showDateAbove = showDateAbove,
                            formattedTime = creationTime?.let { formatTime(it) },
                            isByMe = true,
                            showChatBubbleEdge = showChatBubbleEdge,
                            showBigGap = showChatBubbleEdge,
                            showSender = MutableStateFlow(false),
                            sender = MutableStateFlow(UserInfoElement("", UserId(""))),
                            invitation = MutableStateFlow(null),
                        )
                    }

                    is Unknown,
                    is VerificationRequest -> createNullTimelineElementViewModel()
                } else createNullTimelineElementViewModel()
        }.stateIn(coroutineScope, SharingStarted.Lazily, null) // we need Lazily here as otherwise this might be computed multiple times

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
            is RoomOutboxMessage.SendError.Unknown -> i18n.sendErrorUnknown(sendError.errorResponse?.error)
            RoomOutboxMessage.SendError.EncryptionAlgorithmNotSupported -> i18n.sendErrorUnknown(sendError.toString())
            is RoomOutboxMessage.SendError.EncryptionError -> i18n.sendErrorUnknown(sendError.reason)
            null -> null
        }
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val canAbortSend: StateFlow<Boolean> = MutableStateFlow(true)
    override val canRetrySend: StateFlow<Boolean> = outboxMessageFlow.map { it?.sendError != null }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override fun abortSend() {
        coroutineScope.launch {
            matrixClient.room.cancelSendMessage(roomId = selectedRoomId, transactionId = transactionId)
        }
    }

    override fun retrySend() {
        coroutineScope.launch {
            matrixClient.room.retrySendMessage(roomId = selectedRoomId, transactionId = transactionId)
        }
    }
}
