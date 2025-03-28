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
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) : TimelineElementHolderViewModel, MatrixClientViewModelContext by viewModelContext {
    private val timelineEventFlow = timelineEventFlow.shareIn(coroutineScope, whileSubscribedWithTimeout, replay = 1)
    private val config = get<MatrixMessengerConfiguration>()

    private val timeZone = get<TimeZone>()
    private val initials = get<Initials>()
    private val timelineElementViewModelFactorySelector = get<TimelineElementViewModelFactorySelector>()
    private val timelineElementHolderViewModelFactory = get<TimelineElementHolderViewModelFactory>()

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

    private fun getNewContentIfAvailable(msg: RoomOutboxMessage<*>?) =
        (msg?.content?.relatesTo as? RelatesTo.Replace)?.takeIf { it.eventId == eventId }?.newContent

    private val newContentIfReplaced =
        if (showOriginal) {
            MutableStateFlow(null)
        } else {
            matrixClient.room.getOutbox(roomId).flatten()
                .map { it.reversed().firstNotNullOfOrNull(::getNewContentIfAvailable) }
                .shareIn(coroutineScope, WhileSubscribed(), replay = 1)
        }

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

    private val _editInProgress = MutableStateFlow(false)
    private val _redactionInProgress = MutableStateFlow(false)
    override val redactionInProgress: StateFlow<Boolean> = _redactionInProgress.asStateFlow()
    private val _redactionError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val redactionError: StateFlow<String?> = _redactionError.asStateFlow()
    override val canBeRepliedTo: StateFlow<Boolean> =
        matrixClient.user.canSendEvent<RoomMessageEventContent>(roomId)
            .stateIn(coroutineScope, whileSubscribedWithTimeout, false)

    override val canBeReported: StateFlow<Boolean> =
        matrixClient.user.getById(roomId, userId = matrixClient.userId)
            .map { it?.membership == Membership.JOIN }
            .stateIn(coroutineScope, whileSubscribedWithTimeout, false)

    private val _replyToInProgress = MutableStateFlow(false)

    override val highlight: StateFlow<Boolean> =
        combine(_editInProgress, _replyToInProgress) { editInProgress, replyToInProgress ->
            editInProgress || replyToInProgress
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, false)

    private data class TimelineElementViewModelWrapper(
        val viewModel: TimelineElementViewModel<*>,
        val lifecycle: LifecycleRegistry,
    )

    private val elementCache = MutableStateFlow<TimelineElementViewModelWrapper?>(null)
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

    private val repliedElementCache = MutableStateFlow<RepliedTimelineElementViewModelWrapper?>(null)
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
                eventId = repliedTimelineEvent.eventId,
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
    override val isRead =
        lastReplacement.flatMapLatest {
            getEventReaders.isRead(
                matrixClient = matrixClient,
                roomId = roomId,
                eventId = it ?: eventId,
                sender = senderUserId,
                getReceipts = getReceipts,
            )
        }.stateIn(coroutineScope, Lazily, false) // Lazily to not unnecessary recompute

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

    override fun replace() {
        _editInProgress.value = true
        coroutineScope.launch {
            timelineEventFlow.first().let { onMessageReplace(it.roomId, it.eventId) }
        }
    }

    override fun endReplace() {
        _editInProgress.value = false
    }

    override val canBeRedacted: StateFlow<Boolean> = channelFlow {
        timelineEventFlow
            .filterNotNull()
            .flatMapLatest { timelineEvent ->
                matrixClient.user.canRedactEvent(timelineEvent.roomId, timelineEvent.eventId)
            }
            .collectLatest { send(it) }
    }.stateIn(coroutineScope, whileSubscribedWithTimeout, false)

    override fun redact() {
        if (_redactionInProgress.getAndUpdate { true }.not()) {
            coroutineScope.launch {
                timelineEventFlow.first().let { timelineEvent ->
                    if (matrixClient.user.canRedactEvent(
                            timelineEvent.roomId,
                            timelineEvent.eventId,
                        ).first()
                    ) {
                        _redactionError.value = null
                        matrixClient.api.room.redactEvent(
                            roomId,
                            timelineEvent.eventId,
                            txnId = Uuid.random().toString()
                        ).onSuccess {
                            log.debug { "successfully redacted event ${timelineEvent.eventId}" }
                        }.onFailure {
                            log.error(it) { "could not redact event ${timelineEvent.eventId}" }
                            _redactionError.value = i18n.timelineElementRedactError()
                        }
                    } else log.warn {
                        "try to redact timeline event $eventId," +
                                " but is no room message or it is not by this user"
                    }
                }
            }.invokeOnCompletion { _redactionInProgress.value = false }
        } else log.warn {
            "try to redact timeline event $eventId," +
                    " but is already marked for redaction"
        }
    }

    override fun reply() {
        _replyToInProgress.value = true
        coroutineScope.launch {
            onMessageReply(roomId, eventId)
        }
    }

    override fun endReply() {
        _replyToInProgress.value = false
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
}

class PreviewTimelineElementViewModel1 : TimelineElementHolderViewModel {
    override val roomId: RoomId = RoomId("!room")
    override val eventId: EventId = EventId("\$1:localhost")
    override val key: String = eventId.full
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
}

class PreviewTimelineElementViewModel2 : TimelineElementHolderViewModel {
    override val roomId: RoomId = RoomId("!room")
    override val eventId: EventId = EventId("\$2:localhost")
    override val key: String = eventId.full
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
}
