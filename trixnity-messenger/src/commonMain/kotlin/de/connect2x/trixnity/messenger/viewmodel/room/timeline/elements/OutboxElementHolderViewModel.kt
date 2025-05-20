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
import de.connect2x.trixnity.messenger.viewmodel.util.byEventId
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatProgress
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.flatten
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.RoomOutboxMessage
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.utils.concurrentMutableMap
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
        jumpTo: (roomId: RoomId, eventId: EventId) -> Unit
    ): OutboxElementHolderViewModel = OutboxElementHolderViewModelImpl(
        viewModelContext = viewModelContext,
        key = key,
        outboxMessageFlow = outboxMessageFlow,
        roomId = roomId,
        transactionId = transactionId,
        formattedDate = formattedDate,
        formattedTime = formattedTime,
        onOpenMention = onOpenMention,
        jumpTo = jumpTo
    )

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
    private val jumpTo: (roomId: RoomId, eventId: EventId) -> Unit
) : MatrixClientViewModelContext by viewModelContext, OutboxElementHolderViewModel {

    private val timeZone = get<TimeZone>()
    private val i18n = get<I18n>()
    private val timelineElementViewModelFactorySelector = get<TimelineElementViewModelFactorySelector>()
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
                outboxMessage.content,
                Result.success(outboxMessage.content),
                roomId,
                EventIdOrTransactionId(transactionId),
                onOpenMention,
                ignoreReplacedEvents = true,
            ).also {
                elementCache.value = TimelineElementViewModelWrapper(it, lifecycle)
            }
        }.stateIn(coroutineScope, Eagerly, null)

    override val isReply: StateFlow<Boolean?> =
        outboxMessageFlow.map { outboxMessage ->
            if (outboxMessage == null) return@map false
            val repliedEventId = outboxMessage.content.relatesTo?.replyTo?.eventId
            return@map repliedEventId != null
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    private val getReceiptsByEventCache = concurrentMutableMap<RoomId, Flow<Map<EventId, Set<UserId>>>>()
    private fun getReceipts(roomId: RoomId): Flow<Map<EventId, Set<UserId>>> =
        flow {
            emitAll(
                getReceiptsByEventCache.read { get(roomId) }
                    ?: getReceiptsByEventCache.write {
                        getOrPut(roomId) {
                            matrixClient.user.getAllReceipts(roomId)
                                .byEventId(userId)
                                .stateIn(coroutineScope, WhileSubscribed(), emptyMap())
                        }
                    }
            )
        }

    private data class TimelineElementHolderViewModelWrapper(
        val eventId: EventId,
        val viewModel: TimelineElementHolderViewModel,
        val lifecycle: LifecycleRegistry,
    )

    private val timelineElementHolderViewModelFactory = get<TimelineElementHolderViewModelFactory>()
    private val repliedElementCache = MutableStateFlow<TimelineElementHolderViewModelWrapper?>(null)
    override val repliedElement: StateFlow<TimelineElementHolderViewModel?> =
        outboxMessageFlow.map { outboxMessage ->
            repliedElementCache.value?.lifecycle?.destroy()
            if (outboxMessage == null) return@map null
            val repliedEventId = outboxMessage.content.relatesTo?.replyTo?.eventId ?: return@map null
            val repliedElementCacheValue = repliedElementCache.value
            if (repliedElementCacheValue?.eventId == repliedEventId)
                return@map repliedElementCacheValue.viewModel
            val timelineEventFlow = matrixClient.room.getTimelineEvent(roomId, repliedEventId).filterNotNull()
            val timelineEvent = timelineEventFlow.first()
            repliedElementCache.value?.lifecycle?.destroy()
            val lifecycle = LifecycleRegistry()
            lifecycle.start()
            timelineElementHolderViewModelFactory.create(
                viewModelContext = childContextWithOwnLifecycle(lifecycle),
                key = "element",
                timelineEventFlow = timelineEventFlow,
                roomId = roomId,
                eventId = repliedEventId,
                sender = timelineEvent.sender,
                formattedDate = formatDate(
                    Instant.fromEpochMilliseconds(timelineEvent.originTimestamp)
                        .toLocalDateTime(timeZone)
                ),
                formattedTime = formatTime(
                    Instant.fromEpochMilliseconds(timelineEvent.originTimestamp)
                        .toLocalDateTime(timeZone)
                ),
                showLoadingIndicatorBefore = flowOf(false),
                showLoadingIndicatorAfter = flowOf(false),
                showUnreadMarker = flowOf(false),
                ignoreReplacedEvents = true,
                getReceipts = ::getReceipts,
                onMessageReplace = { _, _ -> },
                onMessageReply = { _, _ -> },
                onMessageReport = { _, _ -> },
                onOpenMention = { _, _ -> },
                onOpenMetadata = {},
                jumpTo = jumpTo
            ).also {
                repliedElementCache.value = TimelineElementHolderViewModelWrapper(repliedEventId, it, lifecycle)
            }
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    private val initials = get<Initials>()
    override val sender: StateFlow<UserInfoElement?> =
        matrixClient.user.getById(roomId, userId).map { user ->
            user.toUserInfoElement(coroutineScope, matrixClient, initials, config.avatarMaxSize, userId)
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val isByMe: Boolean = true

    override val isSent: StateFlow<Boolean> = outboxMessageFlow
        .map { it == null || it.sentAt != null }
        .stateIn(coroutineScope, WhileSubscribed(), true)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val previousSentTimelineEventFlow =
        combine(
            matrixClient.room.getOutbox(roomId).flatten(),
            matrixClient.room.getLastTimelineEvents(roomId)
                .filterNotNull()
                .flatMapLatest { lastTimelineEvents ->
                    timelineElementViewModelFactorySelector.nextSupportedTimelineEvent(
                        lastTimelineEvents,
                        filter = {
                            it.event.unsigned?.transactionId != transactionId
                        }
                    ).filterNotNull()
                }
        ) { outbox, nextSupportedTimelineEvent ->
            val firstOutboxElement =
                outbox.firstOrNull { it.transactionId != nextSupportedTimelineEvent.event.unsigned?.transactionId }

            if (firstOutboxElement?.transactionId == transactionId) nextSupportedTimelineEvent
            else null
        }.shareIn(coroutineScope, whileSubscribedWithTimeout, replay = 1)

    override val isFirstInUserSequence: StateFlow<Boolean?> =
        previousSentTimelineEventFlow.map { previousSentTimelineEvent ->
            when {
                previousSentTimelineEvent == null -> false
                previousSentTimelineEvent.sender == matrixClient.userId -> false
                else -> true
            }
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val showSender: StateFlow<Boolean?> = MutableStateFlow(false).asStateFlow()

    private val clock = get<Clock>()
    override val showBigGapBefore: StateFlow<Boolean?> =
        previousSentTimelineEventFlow
            .map { previousSentTimelineEvent ->
                when {
                    previousSentTimelineEvent == null -> false
                    previousSentTimelineEvent.sender != matrixClient.userId -> true
                    else -> {
                        val previousTimestamp = Instant.fromEpochMilliseconds(previousSentTimelineEvent.originTimestamp)
                        val thisTimestamp = clock.now()
                        thisTimestamp - previousTimestamp > config.showBigGapBeforeThreshold
                    }
                }
            }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

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

    override fun jumpTo() {}
}
