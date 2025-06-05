package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId.Companion.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.EventReactions
import de.connect2x.trixnity.messenger.viewmodel.util.GetEventReactions
import de.connect2x.trixnity.messenger.viewmodel.util.GetEventReaders
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.flatten
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getTimelineEventReplaceAggregation
import net.folivo.trixnity.client.room.message.react
import net.folivo.trixnity.client.store.RoomOutboxMessage
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.membership
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.store.roomId
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.canSendEvent
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents.Direction
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.RedactedEventContent
import net.folivo.trixnity.core.model.events.m.ReactionEventContent
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import org.koin.core.component.get
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid


private val log = KotlinLogging.logger {}

interface TimelineElementHolderViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        key: String,
        timelineEventFlow: Flow<TimelineEvent>,
        roomId: RoomId,
        eventId: EventId,
        sender: UserId,
        formattedDate: String,
        formattedTime: String,
        showUnreadMarker: Flow<Boolean>,
        showLoadingIndicatorBefore: Flow<Boolean>,
        showLoadingIndicatorAfter: Flow<Boolean>,
        ignoreReplacedEvents: Boolean,
        getReceipts: (RoomId) -> Flow<Map<EventId, Set<UserId>>>,
        onMessageReplace: (RoomId, EventId) -> Unit,
        onMessageReply: (RoomId, EventId) -> Unit,
        onMessageReport: (RoomId, EventId) -> Unit,
        onOpenMention: OpenMentionCallback,
        onOpenMetadata: (eventId: EventId) -> Unit,
        jumpTo: (roomId: RoomId, eventId: EventId) -> Unit,
        showOriginal: Boolean = false,
    ): TimelineElementHolderViewModel =
        TimelineElementHolderViewModelImpl(
            viewModelContext = viewModelContext,
            key = key,
            timelineEventFlow = timelineEventFlow,
            showOriginal = showOriginal,
            roomId = roomId,
            eventId = eventId,
            senderUserId = sender,
            formattedDate = formattedDate,
            formattedTime = formattedTime,
            showUnreadMarker = showUnreadMarker,
            showLoadingIndicatorBefore = showLoadingIndicatorBefore,
            showLoadingIndicatorAfter = showLoadingIndicatorAfter,
            ignoreReplacedEvents = ignoreReplacedEvents,
            getReceipts = getReceipts,
            onMessageReplace = onMessageReplace,
            onMessageReply = onMessageReply,
            onMessageReport = onMessageReport,
            onOpenMention = onOpenMention,
            onOpenMetadata = onOpenMetadata,
            jumpTo = jumpTo
        )

    companion object : TimelineElementHolderViewModelFactory
}

interface TimelineElementHolderViewModel : BaseTimelineElementHolderViewModel {
    val roomId: RoomId
    val eventId: EventId

    val showUnreadMarker: StateFlow<Boolean>
    val showLoadingIndicatorBefore: StateFlow<Boolean>
    val showLoadingIndicatorAfter: StateFlow<Boolean>

    val isRead: StateFlow<Boolean?>
    val isSent: StateFlow<Boolean>
    val readers: StateFlow<List<UserInfoElement>?>

    val reactions: StateFlow<EventReactions?>
    val canBeReactedTo: StateFlow<Boolean>

    val isReplaced: StateFlow<Boolean>

    val canBeEdited: StateFlow<Boolean>
    val canBeRedacted: StateFlow<Boolean>
    val canBeRepliedTo: StateFlow<Boolean>
    val canBeReported: StateFlow<Boolean>

    val redactionInProgress: StateFlow<Boolean>
    val redactionError: StateFlow<String?>

    val highlight: StateFlow<Boolean>

    fun replace()
    fun endReplace()
    fun redact()
    fun reply()
    fun endReply()
    fun report()
    fun addReaction(reaction: String)
    fun removeReaction(reaction: String)
    fun openTimelineElementMetadata()
}

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineElementHolderViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val key: String,
    timelineEventFlow: Flow<TimelineEvent>,
    showOriginal: Boolean = false,
    override val roomId: RoomId,
    override val eventId: EventId,
    private val senderUserId: UserId,
    override val formattedDate: String,
    override val formattedTime: String,
    showUnreadMarker: Flow<Boolean>,
    showLoadingIndicatorBefore: Flow<Boolean>,
    showLoadingIndicatorAfter: Flow<Boolean>,
    private val ignoreReplacedEvents: Boolean,
    private val getReceipts: (RoomId) -> Flow<Map<EventId, Set<UserId>>>,
    private val onMessageReplace: (RoomId, EventId) -> Unit,
    private val onMessageReply: (RoomId, EventId) -> Unit,
    private val onMessageReport: (RoomId, EventId) -> Unit,
    private val onOpenMention: OpenMentionCallback,
    private val onOpenMetadata: (eventId: EventId) -> Unit,
    private val jumpTo: (roomId: RoomId, eventId: EventId) -> Unit
) : TimelineElementHolderViewModel, MatrixClientViewModelContext by viewModelContext {
    private val timelineEventFlow = timelineEventFlow.shareIn(coroutineScope, whileSubscribedWithTimeout, replay = 1)
    private val timelineElementViewModelFactorySelector: TimelineElementViewModelFactorySelector = get()
    private val timelineElementHolderViewModelFactory: TimelineElementHolderViewModelFactory = get()
    private val config: MatrixMessengerConfiguration = get()
    private val timeZone: TimeZone = get()
    private val initials: Initials = get()

    private val elementCache: MutableStateFlow<TimelineElementViewModelWrapper?> = MutableStateFlow(null)
    private val repliedElementCache: MutableStateFlow<RepliedTimelineElementViewModelWrapper?> = MutableStateFlow(null)
    private val editInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val replyToInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionError: MutableStateFlow<String?> = MutableStateFlow(null)

    private val previousSupportedTimelineEvent =
        timelineElementViewModelFactorySelector.nextSupportedTimelineEvent(
            matrixClient.room.getTimelineEvents(roomId, eventId, Direction.BACKWARDS)
                .drop(1)
        ).shareIn(coroutineScope, WhileSubscribed(), replay = 1)

    private val nextSupportedTimelineEvent =
        timelineElementViewModelFactorySelector.nextSupportedTimelineEvent(
            matrixClient.room.getTimelineEvents(roomId, eventId, Direction.FORWARDS)
                .drop(1)
        ).shareIn(coroutineScope, WhileSubscribed(), replay = 1)

    override val showUnreadMarker: StateFlow<Boolean> =
        showUnreadMarker
            .flatMapLatest { showUnreadMarker ->
                if (showUnreadMarker) {
                    log.trace { "start compute unread marker at $eventId" }
                    nextSupportedTimelineEvent.filterNotNull().map { it.sender != userId }
                } else {
                    flowOf(false)
                }
            }
            .stateIn(coroutineScope, whileSubscribedWithTimeout, false)

    @OptIn(FlowPreview::class)
    override val showLoadingIndicatorBefore =
        showLoadingIndicatorBefore
            .debounce { if (it) 1.seconds else Duration.ZERO } // prevent indicator on fast loading
            .stateIn(coroutineScope, whileSubscribedWithTimeout, false)

    @OptIn(FlowPreview::class)
    override val showLoadingIndicatorAfter =
        showLoadingIndicatorAfter
            .debounce { if (it) 1.seconds else Duration.ZERO } // prevent indicator on fast loading
            .stateIn(coroutineScope, whileSubscribedWithTimeout, false)

    private fun isEventReplaced(message: RoomOutboxMessage<*>?): Boolean =
        (message?.content?.relatesTo as? RelatesTo.Replace)?.eventId == eventId

    private val outboxElementIfReplaced: SharedFlow<RoomOutboxMessage<*>?> =
        if (showOriginal) {
            MutableStateFlow(null)
        } else {
            matrixClient.room.getOutbox(roomId).flatten()
                .map { outbox -> outbox.reversed().firstOrNull { isEventReplaced(it) } }
                .shareIn(coroutineScope, WhileSubscribed(), replay = 1)
        }

    private val newContentIfReplaced: SharedFlow<MessageEventContent?> =
        outboxElementIfReplaced.map { (it?.content?.relatesTo as? RelatesTo.Replace)?.newContent }
            .shareIn(coroutineScope, WhileSubscribed(), replay = 1)

    override val isReplaced: StateFlow<Boolean> =
        combine(
            newContentIfReplaced,
            matrixClient.room.getTimelineEventReplaceAggregation(roomId, eventId)
        ) { newContentIfReplaced, replaceAggregation ->
            newContentIfReplaced != null || replaceAggregation.replacedBy != null
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, false)

    override val canBeReactedTo: StateFlow<Boolean> =
        combine(
            timelineEventFlow,
            matrixClient.user.canSendEvent<ReactionEventContent>(roomId)
        ) { timelineEvent, canSendReactEvent ->
            timelineEvent.content?.getOrNull() !is RedactedEventContent && canSendReactEvent
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, false)

    override val canBeRepliedTo: StateFlow<Boolean> =
        matrixClient.user.canSendEvent<RoomMessageEventContent>(roomId)
            .stateIn(coroutineScope, whileSubscribedWithTimeout, false)
    override val canBeReported: StateFlow<Boolean> =
        matrixClient.user.getById(roomId, userId = matrixClient.userId)
            .map { it?.membership == Membership.JOIN }
            .stateIn(coroutineScope, whileSubscribedWithTimeout, false)

    override val highlight: StateFlow<Boolean> =
        combine(editInProgress, replyToInProgress) { editInProgress, replyToInProgress ->
            editInProgress || replyToInProgress
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, false)

    private data class TimelineElementViewModelWrapper(
        val viewModel: TimelineElementViewModel<*>,
        val lifecycle: LifecycleRegistry,
    )

    override val element =
        combine(
            timelineEventFlow.distinctUntilChangedBy { it.content },
            newContentIfReplaced.distinctUntilChanged(),
        ) { timelineEvent, newContent ->
            val currentElement = elementCache.value
            currentElement?.lifecycle?.destroy()

            log.trace { "compute element (timelineEvent=$timelineEvent, newContent=$newContent)" }
            val content = when {
                timelineEvent.event.content is RedactedEventContent -> timelineEvent.content
                newContent != null -> Result.success(newContent)
                else -> timelineEvent.content?.map { content ->
                    val relatesTo = (content as? MessageEventContent)?.relatesTo
                    if (showOriginal && relatesTo is RelatesTo.Replace) {
                        val replacement = relatesTo.newContent
                        if (replacement != null)
                            return@map replacement
                    }
                    content
                }
            }

            val lifecycle = LifecycleRegistry()
            lifecycle.start()
            timelineElementViewModelFactorySelector.create(
                childContextWithOwnLifecycle(lifecycle),
                timelineEvent.event.content,
                content,
                roomId,
                EventIdOrTransactionId(eventId),
                onOpenMention,
                ignoreReplacedEvents,
            ).also {
                elementCache.value = TimelineElementViewModelWrapper(it, lifecycle)
            }
        }.stateIn(coroutineScope, Eagerly, null)

    override val isReply: StateFlow<Boolean?> = flow {
        val eventContent = timelineEventFlow.first().event.content
        if (eventContent !is MessageEventContent) {
            emit(false)
            return@flow
        }
        val repliedEventId = eventContent.relatesTo?.replyTo?.eventId
        if (repliedEventId == null) emit(false)
        else emit(true)
    }.stateIn(coroutineScope, Lazily, null)

    private data class RepliedTimelineElementViewModelWrapper(
        val viewModel: TimelineElementHolderViewModel,
        val lifecycle: LifecycleRegistry,
    )

    override val repliedElement =
        timelineEventFlow.map { timelineEvent ->
            val currentElement = repliedElementCache.value
            currentElement?.lifecycle?.destroy()

            val eventContent = timelineEvent.event.content
            if (eventContent !is MessageEventContent) return@map null
            val repliedEventId = eventContent.relatesTo?.replyTo?.eventId
                ?: return@map null // Emit nothing if replied element can't be resolved.
            val repliedTimelineEventFlow = matrixClient.room.getTimelineEvent(roomId, repliedEventId).filterNotNull()
            val repliedTimelineEvent = repliedTimelineEventFlow.first()

            val lifecycle = LifecycleRegistry()
            lifecycle.start()

            timelineElementHolderViewModelFactory.create(
                viewModelContext = childContext("replied-element"),
                key = "replied-element",
                timelineEventFlow = repliedTimelineEventFlow,
                roomId = repliedTimelineEvent.roomId,
                eventId = repliedEventId,
                sender = repliedTimelineEvent.sender,
                formattedDate = formatDate(
                    Instant.fromEpochMilliseconds(repliedTimelineEvent.originTimestamp)
                        .toLocalDateTime(timeZone)
                ),
                formattedTime = formatTime(
                    Instant.fromEpochMilliseconds(repliedTimelineEvent.originTimestamp)
                        .toLocalDateTime(timeZone)
                ),
                showLoadingIndicatorBefore = flowOf(false),
                showLoadingIndicatorAfter = flowOf(false),
                showUnreadMarker = flowOf(false),
                ignoreReplacedEvents = true,
                getReceipts = getReceipts,
                onMessageReplace = { _, _ -> },
                onMessageReply = { _, _ -> },
                onMessageReport = { _, _ -> },
                onOpenMention = { _, _ -> },
                onOpenMetadata = {},
                jumpTo = jumpTo
            ).also {
                repliedElementCache.value = RepliedTimelineElementViewModelWrapper(it, lifecycle)
            }
        }.stateIn(coroutineScope, Lazily, null)

    override val isFirstInUserSequence: StateFlow<Boolean?> =
        previousSupportedTimelineEvent.map { timelineEvent ->
            timelineEvent?.sender != senderUserId
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    // TODO: images are loaded for each holder into memory! This should be fixed.
    override val sender: StateFlow<UserInfoElement?> =
        matrixClient.user.getById(roomId, senderUserId).map { user ->
            user.toUserInfoElement(coroutineScope, matrixClient, initials, config.avatarMaxSize, senderUserId)
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val showSender: StateFlow<Boolean?> = when {
        senderUserId == userId -> flowOf(false)
        else -> matrixClient.room.getById(roomId)
            .filterNotNull()
            .map { it.isDirect }
            .flatMapLatest { isDirect ->
                if (isDirect) flowOf(false)
                else previousSupportedTimelineEvent.map { timelineEvent ->
                    timelineEvent?.sender != senderUserId || timelineEvent.event is StateEvent
                }
            }
    }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val showBigGapBefore: StateFlow<Boolean?> =
        previousSupportedTimelineEvent.map { timelineEvent ->
            when {
                timelineEvent?.sender != senderUserId -> true
                else -> {
                    val previousTimestamp =
                        Instant.fromEpochMilliseconds(timelineEvent.originTimestamp)
                    val thisTimestamp = Instant.fromEpochMilliseconds(timelineEventFlow.first().originTimestamp)
                    thisTimestamp - previousTimestamp > config.showBigGapBeforeThreshold
                }
            }
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val isByMe: Boolean = senderUserId == userId

    private val lastReplacement =
        if (showOriginal) {
            MutableStateFlow(null)
        } else {
            matrixClient.room.getTimelineEventReplaceAggregation(roomId, eventId)
                .map { it.replacedBy }
                .shareIn(coroutineScope, WhileSubscribed(), replay = 1)
        }

    private val getEventReaders = get<GetEventReaders>()

    override val readers =
        lastReplacement.flatMapLatest {
            getEventReaders.isReadBy(
                matrixClient = matrixClient,
                roomId = roomId,
                eventId = it ?: eventId,
                sender = senderUserId,
                getReceipts = getReceipts,
                initials = initials,
                avatarMaxSize = config.avatarMaxSize,
            )
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    private val getEventReactions = get<GetEventReactions>()
    override val reactions =
        getEventReactions(
            matrixClient = matrixClient,
            roomId = roomId,
            eventId = eventId,
            initials = initials,
            avatarMaxSize = config.avatarMaxSize
        ).stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val canBeEdited: StateFlow<Boolean> = timelineEventFlow
        .filterNotNull()
        .map {
            it.event.sender == matrixClient.userId && it.content?.getOrNull() is TextBased
        }
        .stateIn(coroutineScope, whileSubscribedWithTimeout, false)

    override val canBeRedacted: StateFlow<Boolean> = channelFlow {
        timelineEventFlow
            .filterNotNull()
            .flatMapLatest { timelineEvent ->
                matrixClient.user.canRedactEvent(timelineEvent.roomId, timelineEvent.eventId)
            }
            .collectLatest { send(it) }
    }.stateIn(coroutineScope, whileSubscribedWithTimeout, false)

    override val isRead: StateFlow<Boolean> =
        lastReplacement.flatMapLatest {
            getEventReaders.isRead(
                matrixClient = matrixClient,
                roomId = roomId,
                eventId = it ?: eventId,
                sender = senderUserId,
                getReceipts = getReceipts,
            )
        }.stateIn(coroutineScope, Lazily, false) // Lazily to not unnecessary recompute

    override val isSent: StateFlow<Boolean> = outboxElementIfReplaced
        .map { it == null || it.sentAt != null }
        .stateIn(coroutineScope, WhileSubscribed(), true)

    override fun replace() {
        editInProgress.value = true
        coroutineScope.launch {
            timelineEventFlow.first().let { onMessageReplace(it.roomId, it.eventId) }
        }
    }

    override fun endReplace() {
        editInProgress.value = false
    }

    override fun redact() {
        if (redactionInProgress.getAndUpdate { true }.not()) {
            coroutineScope.launch {
                timelineEventFlow.first().let { timelineEvent ->
                    if (matrixClient.user.canRedactEvent(
                            timelineEvent.roomId,
                            timelineEvent.eventId,
                        ).first()
                    ) {
                        redactionError.value = null
                        matrixClient.api.room.redactEvent(
                            roomId,
                            timelineEvent.eventId,
                            txnId = Uuid.random().toString()
                        ).onSuccess {
                            log.debug { "successfully redacted event ${timelineEvent.eventId}" }
                        }.onFailure {
                            log.error(it) { "could not redact event ${timelineEvent.eventId}" }
                            redactionError.value = i18n.timelineElementRedactError()
                        }
                    } else log.warn {
                        "try to redact timeline event $eventId," +
                                " but is no room message or it is not by this user"
                    }
                }
            }.invokeOnCompletion { redactionInProgress.value = false }
        } else log.warn {
            "try to redact timeline event $eventId," +
                    " but is already marked for redaction"
        }
    }

    override fun reply() {
        replyToInProgress.value = true
        coroutineScope.launch {
            onMessageReply(roomId, eventId)
        }
    }

    override fun endReply() {
        replyToInProgress.value = false
    }

    override fun report() {
        coroutineScope.launch {
            onMessageReport(roomId, eventId)
        }
    }

    override fun addReaction(reaction: String) {
        coroutineScope.launch {
            matrixClient.room.sendMessage(roomId) {
                react(eventId, reaction)
            }
        }
    }

    override fun removeReaction(reaction: String) {
        coroutineScope.launch {
            val eventId = reactions.value?.byUser?.get(matrixClient.userId)?.reactions?.get(reaction)
            if (eventId != null) {
                matrixClient.api.room.redactEvent(
                    roomId,
                    eventId,
                    txnId = Uuid.random().toString(),
                )
            } else {
                log.warn { "could not remove reaction, because not present in loaded reactions" }
            }
        }
    }

    override fun openTimelineElementMetadata() {
        onOpenMetadata(this.eventId)
    }

    override fun jumpTo() {
        jumpTo(roomId, eventId)
    }
}

class PreviewTimelineElementViewModel1 : TimelineElementHolderViewModel {
    override val roomId: RoomId = RoomId("!room")
    override val eventId: EventId = EventId("\$1:localhost")
    override val key: String = eventId.full
    override val isSent: StateFlow<Boolean> = MutableStateFlow(false)
    override val element: MutableStateFlow<TimelineElementViewModel<*>?> =
        MutableStateFlow(object : RoomMessageTimelineElementViewModel.TextBased.Text {
            override val body: String = "Hello everyone!"
            override val formattedBody: String = "Hello <b/>everyone!"
            override val mentionsInBody: Map<IntRange, MutableStateFlow<TimelineElementMention>> = mapOf()
            override val mentionsInFormattedBody: Map<IntRange, MutableStateFlow<TimelineElementMention>> = mapOf()
            override fun openMention(mention: TimelineElementMention) {}
        })
    override val isFirstInUserSequence: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val formattedTime: String = "12:12"
    override val formattedDate: String = "21.11.2024"
    override val isByMe: Boolean = true
    override val sender: MutableStateFlow<UserInfoElement?> = MutableStateFlow(null)
    override val showSender: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val showBigGapBefore: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val isReply: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val repliedElement: MutableStateFlow<TimelineElementHolderViewModel?> =
        MutableStateFlow(PreviewTimelineElementViewModel2())
    override val showUnreadMarker: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLoadingIndicatorBefore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLoadingIndicatorAfter: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isRead: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val readers: MutableStateFlow<List<UserInfoElement>> = MutableStateFlow(listOf())
    override val canBeReactedTo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isReplaced: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canBeEdited: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canBeRedacted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val canBeRepliedTo: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val canBeReported: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val reactions: MutableStateFlow<EventReactions> = MutableStateFlow(EventReactions(setOf()))
    override val highlight: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override fun replace() {}
    override fun endReplace() {}
    override fun redact() {}
    override fun reply() {}
    override fun endReply() {}
    override fun report() {}
    override fun addReaction(reaction: String) {}
    override fun removeReaction(reaction: String) {}
    override fun openTimelineElementMetadata() {}
    override fun jumpTo() {}
}

class PreviewTimelineElementViewModel2 : TimelineElementHolderViewModel {
    override val roomId: RoomId = RoomId("!room")
    override val eventId: EventId = EventId("\$2:localhost")
    override val key: String = eventId.full
    override val isSent: StateFlow<Boolean> = MutableStateFlow(false)
    override val element: MutableStateFlow<TimelineElementViewModel<*>?> =
        MutableStateFlow(object : RoomMessageTimelineElementViewModel.TextBased.Text {
            override val body: String = "Hello!"
            override val formattedBody: String = "Hello!"
            override val mentionsInBody: Map<IntRange, StateFlow<TimelineElementMention>> = mapOf()
            override val mentionsInFormattedBody: Map<IntRange, StateFlow<TimelineElementMention>> = mapOf()
            override fun openMention(mention: TimelineElementMention) {}
        })
    override val isFirstInUserSequence: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val formattedTime: String = "12:24"
    override val formattedDate: String = "21.11.2024"
    override val isByMe: Boolean = false
    override val sender: MutableStateFlow<UserInfoElement?> =
        MutableStateFlow(UserInfoElement(UserId("bob", "server"), "Bob", "B"))
    override val showSender: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val showBigGapBefore: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val repliedElement: MutableStateFlow<TimelineElementHolderViewModel?> = MutableStateFlow(null)
    override val showUnreadMarker: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLoadingIndicatorBefore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLoadingIndicatorAfter: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isRead: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val readers: MutableStateFlow<List<UserInfoElement>> = MutableStateFlow(listOf())
    override val canBeReactedTo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isReplaced: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isReply: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val canBeEdited: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canBeRedacted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val canBeRepliedTo: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val canBeReported: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val reactions: MutableStateFlow<EventReactions> = MutableStateFlow(EventReactions(setOf()))
    override val highlight: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override fun replace() {}
    override fun endReplace() {}
    override fun redact() {}
    override fun reply() {}
    override fun endReply() {}
    override fun report() {}
    override fun addReaction(reaction: String) {}
    override fun removeReaction(reaction: String) {}
    override fun openTimelineElementMetadata() {}
    override fun jumpTo() {}
}
