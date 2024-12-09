package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId.Companion.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.formatProgress
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.flatten
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.RoomOutboxMessage
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

interface OutboxElementHolderViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        key: String,
        outboxMessageFlow: Flow<RoomOutboxMessage<*>?>,
        roomId: RoomId,
        transactionId: String,
        formattedDate: String,
        formattedTime: String,
        onOpenMention: OpenMentionCallback,
    ): OutboxElementHolderViewModel {
        return OutboxElementHolderViewModelImpl(
            viewModelContext = viewModelContext,
            key = key,
            outboxMessageFlow = outboxMessageFlow,
            roomId = roomId,
            transactionId = transactionId,
            formattedDate = formattedDate,
            formattedTime = formattedTime,
            onOpenMention = onOpenMention,
        )
    }

    companion object : OutboxElementHolderViewModelFactory

}

interface OutboxElementHolderViewModel : BaseTimelineElementHolderViewModel {
    val transactionId: String

    val uploadProgress: StateFlow<FileTransferProgressElement?>
    val sendError: StateFlow<String?>

    val canRetrySend: StateFlow<Boolean>
    val canAbortSend: StateFlow<Boolean>

    fun retrySend()
    fun abortSend()
}

class OutboxElementHolderViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val key: String,
    outboxMessageFlow: Flow<RoomOutboxMessage<*>?>,
    private val roomId: RoomId,
    override val transactionId: String,
    override val formattedDate: String,
    override val formattedTime: String,
    onOpenMention: OpenMentionCallback,
) : MatrixClientViewModelContext by viewModelContext, OutboxElementHolderViewModel {

    private val i18n = get<I18n>()
    private val timelineElementViewModelFactorySelector = get<TimelineElementViewModelFactorySelector>()
    private val repliedTimelineElementHolderViewModelFactory = get<RepliedTimelineElementHolderViewModelFactory>()
    private val config = get<MatrixMessengerConfiguration>()

    private data class TimelineElementViewModelWrapper(
        val viewModel: TimelineElementViewModel<*>,
        val lifecycle: LifecycleRegistry,
    )

    private val elementCache = MutableStateFlow<TimelineElementViewModelWrapper?>(null)
    override val element =
        outboxMessageFlow.map { outboxMessage ->
            elementCache.value?.lifecycle?.destroy()

            log.trace { "compute element (outboxMessage=$outboxMessage)" }
            if (outboxMessage == null) return@map TimelineElementViewModel.Empty
            val lifecycle = LifecycleRegistry()
            lifecycle.start()
            timelineElementViewModelFactorySelector.create(
                childContextWithOwnLifecycle(lifecycle),
                Result.success(outboxMessage.content),
                roomId,
                EventIdOrTransactionId(transactionId),
                onOpenMention,
            ).also {
                elementCache.value = TimelineElementViewModelWrapper(it, lifecycle)
            }
        }.stateIn(coroutineScope, Eagerly, null)

    private val repliedElementCache = MutableStateFlow<TimelineElementViewModelWrapper?>(null)
    override val repliedElement: StateFlow<RepliedTimelineElementHolderViewModel?> =
        outboxMessageFlow.map { outboxMessage ->
            repliedElementCache.value?.lifecycle?.destroy()
            if (outboxMessage == null) return@map null
            val repliedEventId = outboxMessage.content.relatesTo?.replyTo?.eventId
            if (repliedEventId == null) return@map null
            val lifecycle = LifecycleRegistry()
            lifecycle.start()
            repliedTimelineElementHolderViewModelFactory.create(
                childContextWithOwnLifecycle(lifecycle),
                matrixClient.room.getTimelineEvent(roomId, repliedEventId),
                roomId,
                repliedEventId,
                onOpenMention,
            )
        }.stateIn(coroutineScope, Eagerly, null)

    private val initials = get<Initials>()
    override val sender: StateFlow<UserInfoElement?> =
        matrixClient.user.getById(roomId, userId).map { user ->
            user?.toUserInfoElement(matrixClient, initials, config.avatarMaxSize)
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val isByMe: Boolean = true

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isFirstInUserSequence: StateFlow<Boolean?> =
        combine(
            matrixClient.room.getLastTimelineEvents(roomId).filterNotNull()
                .mapLatest { lastTimelineEvents ->
                    lastTimelineEvents.map { it.first() }.firstOrNull { timelineEvent ->
                        timelineElementViewModelFactorySelector.supports(timelineEvent.content)
                    }
                },
            matrixClient.room.getOutbox(roomId).flatten(),
        ) { lastTimelineEvent, outbox ->
            val lastTimelineEventTransactionId = lastTimelineEvent?.event?.unsigned?.transactionId
            val firstOutboxTransactionId =
                outbox.firstOrNull { it.transactionId != lastTimelineEventTransactionId }?.transactionId
            log.trace { "transactionId=$transactionId, lastTimelineEventTransactionId=$lastTimelineEventTransactionId, firstOutboxTransactionId=$firstOutboxTransactionId, sender=${lastTimelineEvent?.sender}" }
            firstOutboxTransactionId == transactionId && lastTimelineEvent?.sender != userId
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val showSender: StateFlow<Boolean?> =
        matrixClient.room.getById(roomId)
            .filterNotNull()
            .map { it.isDirect }
            .flatMapLatest { isDirect ->
                if (isDirect) flowOf(false)
                else isFirstInUserSequence.filterNotNull()
            }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val showBigGapBefore: StateFlow<Boolean?> = MutableStateFlow(false).asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val uploadProgress: StateFlow<FileTransferProgressElement?> =
        outboxMessageFlow.flatMapLatest { outboxMessage ->
            outboxMessage?.mediaUploadProgress?.map {
                val total = it?.total
                if (total == null) {
                    null
                } else {
                    FileTransferProgressElement(
                        percent = if (total > 0) {
                            it.transferred / total.toFloat()
                        } else {
                            0f
                        },
                        formattedProgress = formatProgress(it)
                    )
                }
            } ?: flowOf(null)
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val sendError: StateFlow<String?> = outboxMessageFlow.map { outboxMessage ->
        if (outboxMessage == null) return@map null
        when (val sendError = outboxMessage.sendError) {
            RoomOutboxMessage.SendError.NoEventPermission -> i18n.sendErrorEventPermission()
            RoomOutboxMessage.SendError.NoMediaPermission -> i18n.sendErrorMediaPermission()
            RoomOutboxMessage.SendError.MediaTooLarge -> i18n.sendErrorMediaTooLarge()
            is RoomOutboxMessage.SendError.BadRequest -> i18n.sendErrorUnknown(sendError.errorResponse.error)
            is RoomOutboxMessage.SendError.Unknown -> i18n.sendErrorUnknown(sendError.errorResponse?.error)
            RoomOutboxMessage.SendError.EncryptionAlgorithmNotSupported -> i18n.sendErrorUnknown(sendError.toString())
            is RoomOutboxMessage.SendError.EncryptionError -> i18n.sendErrorUnknown(sendError.reason)
            null -> null
        }
    }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val canAbortSend: StateFlow<Boolean> = MutableStateFlow(true)
    override val canRetrySend: StateFlow<Boolean> = outboxMessageFlow.map { it?.sendError != null }
        .stateIn(coroutineScope, whileSubscribedWithTimeout, false)

    override fun abortSend() {
        coroutineScope.launch {
            matrixClient.room.cancelSendMessage(roomId = roomId, transactionId = transactionId)
        }
    }

    override fun retrySend() {
        coroutineScope.launch {
            matrixClient.room.retrySendMessage(roomId = roomId, transactionId = transactionId)
        }
    }
}
