package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import com.benasher44.uuid.uuid4
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId.Companion.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.takeWhileInclusive
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import net.folivo.trixnity.client.flatten
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getAccountData
import net.folivo.trixnity.client.room.getTimelineEventReactionAggregation
import net.folivo.trixnity.client.room.getTimelineEventReplaceAggregation
import net.folivo.trixnity.client.room.message.react
import net.folivo.trixnity.client.store.RoomOutboxMessage
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.membership
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.store.originalName
import net.folivo.trixnity.client.store.roomId
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.canSendEvent
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents.Direction
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.RedactedEventContent
import net.folivo.trixnity.core.model.events.m.FullyReadEventContent
import net.folivo.trixnity.core.model.events.m.ReactionEventContent
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import org.koin.core.component.get
import kotlin.time.Duration.Companion.hours


private val log = KotlinLogging.logger { }

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
        hasLoadingIndicatorBefore: Flow<Boolean>,
        hasLoadingIndicatorAfter: Flow<Boolean>,
        getReceipts: (RoomId) -> Flow<Map<EventId, Set<UserId>>>,
        onMessageReplace: (RoomId, EventId) -> Unit,
        onMessageReply: (RoomId, EventId) -> Unit,
        onMessageReport: (RoomId, EventId) -> Unit,
        onOpenMention: OpenMentionCallback,
    ): TimelineElementHolderViewModel =
        TimelineElementHolderViewModelImpl(
            viewModelContext = viewModelContext,
            key = key,
            timelineEventFlow = timelineEventFlow,
            roomId = roomId,
            eventId = eventId,
            senderUserId = sender,
            formattedDate = formattedDate,
            formattedTime = formattedTime,
            hasLoadingIndicatorBefore = hasLoadingIndicatorBefore,
            hasLoadingIndicatorAfter = hasLoadingIndicatorAfter,
            getReceipts = getReceipts,
            onMessageReplace = onMessageReplace,
            onMessageReply = onMessageReply,
            onMessageReport = onMessageReport,
            onOpenMention = onOpenMention,
        )

    companion object : TimelineElementHolderViewModelFactory
}

interface TimelineElementHolderViewModel : BaseTimelineElementHolderViewModel {
    val eventId: EventId

    val hasUnreadMarker: StateFlow<Boolean>
    val hasLoadingIndicatorBefore: StateFlow<Boolean>
    val hasLoadingIndicatorAfter: StateFlow<Boolean>

    val isRead: StateFlow<Boolean?>
    val isReadBy: StateFlow<List<UserInfoElement>?>

    val reactions: StateFlow<Map<String, Set<ReactionEvent>>>
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
    fun removeReaction(reaction: ReactionEvent)

    data class ReactionEvent(
        val eventId: EventId,
        val sender: UserInfoElement,
        val isMe: Boolean,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineElementHolderViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val key: String,
    timelineEventFlow: Flow<TimelineEvent>,
    val roomId: RoomId,
    override val eventId: EventId,
    private val senderUserId: UserId,
    override val formattedDate: String,
    override val formattedTime: String,
    hasLoadingIndicatorBefore: Flow<Boolean>,
    hasLoadingIndicatorAfter: Flow<Boolean>,
    private val getReceipts: (RoomId) -> Flow<Map<EventId, Set<UserId>>>,
    private val onMessageReplace: (RoomId, EventId) -> Unit,
    private val onMessageReply: (RoomId, EventId) -> Unit,
    private val onMessageReport: (RoomId, EventId) -> Unit,
    private val onOpenMention: OpenMentionCallback,
) : TimelineElementHolderViewModel, MatrixClientViewModelContext by viewModelContext {
    private val timelineEventFlow = timelineEventFlow.shareIn(coroutineScope, whileSubscribedWithTimeout)
    private val config = get<MatrixMessengerConfiguration>()

    private val initials = get<Initials>()
    private val timelineElementViewModelFactorySelector = get<TimelineElementViewModelFactorySelector>()
    private val repliedTimelineElementHolderViewModelFactory = get<RepliedTimelineElementHolderViewModelFactory>()

    override val hasLoadingIndicatorBefore =
        hasLoadingIndicatorBefore.stateIn(coroutineScope, whileSubscribedWithTimeout, false)
    override val hasLoadingIndicatorAfter =
        hasLoadingIndicatorAfter.stateIn(coroutineScope, whileSubscribedWithTimeout, false)

    private val previousSupportedTimelineEvent =
        timelineElementViewModelFactorySelector.nextSupportedTimelineEvent(
            matrixClient.room.getTimelineEvents(roomId, eventId, Direction.BACKWARDS)
                .drop(1)
        ).shareIn(coroutineScope, SharingStarted.WhileSubscribed(), replay = 1)

    private val nextSupportedTimelineEvent =
        timelineElementViewModelFactorySelector.nextSupportedTimelineEvent(
            matrixClient.room.getTimelineEvents(roomId, eventId, Direction.FORWARDS)
                .drop(1)
        ).shareIn(coroutineScope, SharingStarted.WhileSubscribed(), replay = 1)

    override val hasUnreadMarker: StateFlow<Boolean> =
        matrixClient.room.getAccountData<FullyReadEventContent>(roomId).flatMapLatest { fullyReadEvent ->
            if (fullyReadEvent?.eventId == eventId) {
                log.trace { "start compute unread marker at $eventId" }
                nextSupportedTimelineEvent.map { it != null && it.sender != userId }
            } else flowOf(false)
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, false)

    override val isReplaced: StateFlow<Boolean> =
        matrixClient.room.getTimelineEventReplaceAggregation(roomId, eventId).map {
            it.replacedBy != null
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, true)

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

    private fun getNewContentIfAvailable(msg: RoomOutboxMessage<*>?) =
        (msg?.content?.relatesTo as? RelatesTo.Replace)?.takeIf { it.eventId == eventId }?.newContent

    private val newContentIfReplaced = matrixClient.room.getOutbox(roomId).flatten()
        .map { it.reversed().firstNotNullOfOrNull(::getNewContentIfAvailable) }

    private data class TimelineElementViewModelWrapper(
        val viewModel: TimelineElementViewModel<*>,
        val lifecycle: LifecycleRegistry,
    )

    private val elementCache = MutableStateFlow<TimelineElementViewModelWrapper?>(null)
    override val element =
        combine(
            timelineEventFlow,
            newContentIfReplaced.distinctUntilChanged(),
        ) { timelineEvent, newContent ->
            val currentElement = elementCache.value
            currentElement?.lifecycle?.destroy()

            log.trace { "compute element (timelineEvent=$timelineEvent, newContent=$newContent)" }
            val content = newContent?.let { Result.success(it) } ?: timelineEvent.content

            val lifecycle = LifecycleRegistry()
            lifecycle.start()
            timelineElementViewModelFactorySelector.create(
                childContextWithOwnLifecycle(lifecycle),
                content,
                roomId,
                EventIdOrTransactionId(eventId),
                onOpenMention,
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

    override val repliedElement: StateFlow<RepliedTimelineElementHolderViewModel?> =
        flow {
            // we don't need to subscribe for changes or manage the child lifecycle as a reply cannot be changed in Matrix.
            val eventContent = timelineEventFlow.first().event.content
            if (eventContent !is MessageEventContent) return@flow
            val repliedEventId = eventContent.relatesTo?.replyTo?.eventId
            if (repliedEventId == null) return@flow
            emit(
                repliedTimelineElementHolderViewModelFactory.create(
                    childContext("repliedElement-$eventId"),
                    matrixClient.room.getTimelineEvent(roomId, repliedEventId),
                    roomId,
                    repliedEventId,
                    onOpenMention,
                )
            )
        }.stateIn(coroutineScope, Lazily, null) // only calculate once!

    override val isFirstInUserSequence: StateFlow<Boolean?> =
        previousSupportedTimelineEvent.map { timelineEvent ->
            timelineEvent?.sender != senderUserId
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    // TODO images are loaded for each holder into memory! This should be fixed.
    override val sender: StateFlow<UserInfoElement?> =
        matrixClient.user.getById(roomId, senderUserId).map { user ->
            user.toUserInfoElement(coroutineScope, matrixClient, initials, config.avatarMaxSize, senderUserId)
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val showSender: StateFlow<Boolean?> =
        matrixClient.room.getById(roomId)
            .filterNotNull()
            .map { it.isDirect }
            .flatMapLatest { isDirect ->
                if (isDirect) flowOf(false)
                else isFirstInUserSequence.filterNotNull()
            }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val showBigGapBefore: StateFlow<Boolean?> =
        previousSupportedTimelineEvent.map { timelineEvent ->
            when {
                timelineEvent?.sender != senderUserId -> true
                else -> {
                    val previousTimestamp =
                        Instant.fromEpochMilliseconds(timelineEvent.originTimestamp)
                    val thisTimestamp = Instant.fromEpochMilliseconds(timelineEventFlow.first().originTimestamp)
                    thisTimestamp - previousTimestamp > 1.hours
                }
            }
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val isByMe: Boolean = senderUserId == userId


    private sealed interface IsReadSearchResult {
        data object Unread : IsReadSearchResult
        data class Read(val readBy: Set<UserId>) : IsReadSearchResult
    }

    /**
     * TODO This algorithm has a few issues (mostly edge cases):
     *   - Ressource consumption: Too many same re-computations are done for each element.
     *     For example when far away from the last event and only ourself wrote messages.
     *   - Wrong results: On membership change depending on history visibility we may getting wrong results.
     *     For example when A sends a message and B joins, B may not be able to read at all but is marked as reader.
     *   Possible solution: lazily calculate Map<EventId,Set<UserId>> (sorted) in TimelineViewModel, which can be iterated through.
     *   This List must also forget "old" events, when not needed anymore and consider membership changes depending on history visibility.
     */
    private fun isReadSearch(roomId: RoomId, eventId: EventId): Flow<IsReadSearchResult> =
        getReceipts(roomId).flatMapLatest { receipts ->
            log.trace { "isReadSearch: roomId=$roomId eventId=$eventId" }
            matrixClient.room.getTimelineEvents(roomId, eventId, Direction.FORWARDS)
                .transform {
                    val timelineEvent = it.first()
                    val sender = timelineEvent.sender
                    val currentEventId = timelineEvent.eventId
                    val currentRoomId = timelineEvent.roomId
                    val foundReaders = buildSet {
                        addAll(receipts[currentEventId].orEmpty())
                        add(sender)
                        remove(senderUserId)
                        remove(userId)
                    }
                    when {
                        foundReaders.isNotEmpty() -> emit(IsReadSearchResult.Read(foundReaders))
                        currentRoomId != roomId -> emitAll(isReadSearch(currentRoomId, currentEventId)) // recursive!
                        else -> emit(IsReadSearchResult.Unread)
                    }
                }
        }

    override val isRead: StateFlow<Boolean?> =
        isReadSearch(roomId, eventId).map {
            when (it) {
                is IsReadSearchResult.Read -> true
                IsReadSearchResult.Unread -> false
            }
        }.takeWhileInclusive { !it }
            .stateIn(coroutineScope, Lazily, false) // Lazily to not unnecessary recompute

    override val isReadBy: StateFlow<List<UserInfoElement>?> =
        flow {
            val cumulatedReads = mutableSetOf<UserId>()
            isReadSearch(roomId, eventId)
                .collect {
                    when (it) {
                        is IsReadSearchResult.Read -> {
                            cumulatedReads.addAll(it.readBy)
                            emit(cumulatedReads.toList())
                        }

                        IsReadSearchResult.Unread -> {
                            if (cumulatedReads.isEmpty()) emit(cumulatedReads.toList())
                        }
                    }
                }
        }.flatMapLatest { userIds ->
            if (userIds.isEmpty()) flowOf(emptyList())
            else combine(userIds.map { userId -> matrixClient.user.getById(roomId, userId) }) { roomUsers ->
                roomUsers.mapNotNull { user ->
                    if (user == null) return@mapNotNull null
                    user.toUserInfoElement(coroutineScope, matrixClient, initials, config.avatarMaxSize, user.userId)
                }
            }
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    // TODO should consider outbox to get immediate feedback
    override val reactions =
        timelineEventFlow.flatMapLatest { timelineEvent ->
            when (timelineEvent.content?.getOrNull()) {
                is RedactedEventContent -> flowOf(emptyMap())
                else -> matrixClient.room.getTimelineEventReactionAggregation(roomId, eventId)
                    .flatMapLatest { reactions ->
                        combine(reactions.reactions.flatMap { (_, timelineEvents) ->
                            timelineEvents.map { timelineEvent ->
                                matrixClient.user.getById(roomId, timelineEvent.sender)
                            }
                        }) { users ->
                            reactions.reactions.mapValues { (_, events) ->
                                events.mapNotNull { event ->
                                    users.find { it?.userId == event.sender }?.let { sender ->
                                        TimelineElementHolderViewModel.ReactionEvent(
                                            eventId = event.eventId,
                                            sender = UserInfoElement(
                                                name = sender.originalName ?: sender.name,
                                                userId = sender.userId,
                                                initials = initials.compute(sender.originalName ?: sender.name),
                                                image = null // TODO
                                            ),
                                            isMe = event.sender == matrixClient.userId,
                                        )
                                    }
                                }.toSet()
                            }
                        }
                    }
            }
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, emptyMap())

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
        if (redactionInProgress.value.not()) {
            coroutineScope.launch {
                timelineEventFlow.first().let { timelineEvent ->
                    if (matrixClient.user.canRedactEvent(
                            timelineEvent.roomId,
                            timelineEvent.eventId
                        ).first()
                    ) {
                        launch {
                            _redactionInProgress.value = true
                            _redactionError.value = null
                            matrixClient.api.room.redactEvent(
                                roomId,
                                timelineEvent.eventId,
                                txnId = uuid4().toString()
                            ).onSuccess {
                                log.debug { "successfully redacted event ${timelineEvent.eventId}" }
                            }.onFailure {
                                log.error(it) { "could not redact event ${timelineEvent.eventId}" }
                                _redactionError.value = i18n.timelineElementRedactError()
                            }.also {
                                _redactionInProgress.value = false
                            }
                        }
                    } else {
                        log.warn { "try to redact timeline event $eventId, but is no room message or it is not by this user" }

                    }
                }
            }
        } else {
            log.warn { "try to redact timeline event $eventId, but is already marked for redaction" }
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

    override fun removeReaction(reaction: TimelineElementHolderViewModel.ReactionEvent) {
        coroutineScope.launch {
            matrixClient.api.room.redactEvent(
                roomId,
                reaction.eventId,
                txnId = uuid4().toString()
            )
        }
    }

    override fun toString(): String =
        "TimelineElementViewModel(showLoadingIndicator=${this@TimelineElementHolderViewModelImpl.hasLoadingIndicatorBefore.value}" +
                ", shouldShowUnreadMarker=${hasUnreadMarker.value})"
}

class PreviewTimelineElementViewModel1 : TimelineElementHolderViewModel {
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
    override val repliedElement: MutableStateFlow<RepliedTimelineElementHolderViewModel?> = MutableStateFlow(
        PreviewRepliedTimelineElementViewModel1()
    )
    override val hasUnreadMarker: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasLoadingIndicatorBefore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasLoadingIndicatorAfter: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isRead: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isReadBy: MutableStateFlow<List<UserInfoElement>> = MutableStateFlow(listOf())
    override val canBeReactedTo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isReplaced: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canBeEdited: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canBeRedacted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val canBeRepliedTo: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val canBeReported: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val reactions: MutableStateFlow<Map<String, Set<TimelineElementHolderViewModel.ReactionEvent>>> =
        MutableStateFlow(emptyMap())
    override val highlight: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override fun replace() {}
    override fun endReplace() {}
    override fun redact() {}
    override fun reply() {}
    override fun endReply() {}
    override fun report() {}
    override fun addReaction(reaction: String) {}
    override fun removeReaction(reaction: TimelineElementHolderViewModel.ReactionEvent) {}
}

class PreviewTimelineElementViewModel2 : TimelineElementHolderViewModel {
    override val eventId: EventId = EventId("\$2:localhost")
    override val key: String = eventId.full
    override val element: MutableStateFlow<TimelineElementViewModel<*>?> =
        MutableStateFlow(object : RoomMessageTimelineElementViewModel.TextBased.Text {
            override val body: String = "Hello too!"
            override val formattedBody: String = "Hello <b/>too!"
            override val mentionsInBody: Map<IntRange, StateFlow<TimelineElementMention>> = mapOf()
            override val mentionsInFormattedBody: Map<IntRange, StateFlow<TimelineElementMention>> = mapOf()
            override fun openMention(mention: TimelineElementMention) {}
        })
    override val isFirstInUserSequence: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val formattedTime: String = "12:24"
    override val formattedDate: String = "21.11.2024"
    override val isByMe: Boolean = false
    override val sender: MutableStateFlow<UserInfoElement?> =
        MutableStateFlow(UserInfoElement("Bob", UserId("bob", "server"), "B"))
    override val showSender: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val showBigGapBefore: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val repliedElement: MutableStateFlow<RepliedTimelineElementHolderViewModel?> = MutableStateFlow(null)
    override val hasUnreadMarker: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasLoadingIndicatorBefore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasLoadingIndicatorAfter: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isRead: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isReadBy: MutableStateFlow<List<UserInfoElement>> = MutableStateFlow(listOf())
    override val canBeReactedTo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isReplaced: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isReply: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val canBeEdited: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canBeRedacted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val canBeRepliedTo: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val canBeReported: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val reactions: MutableStateFlow<Map<String, Set<TimelineElementHolderViewModel.ReactionEvent>>> =
        MutableStateFlow(emptyMap())
    override val highlight: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override fun replace() {}
    override fun endReplace() {}
    override fun redact() {}
    override fun reply() {}
    override fun endReply() {}
    override fun report() {}
    override fun addReaction(reaction: String) {}
    override fun removeReaction(reaction: TimelineElementHolderViewModel.ReactionEvent) {}
}
