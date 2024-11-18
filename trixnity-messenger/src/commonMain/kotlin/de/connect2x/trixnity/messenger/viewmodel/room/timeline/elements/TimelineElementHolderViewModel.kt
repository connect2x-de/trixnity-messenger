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
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.flatten
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getAccountData
import net.folivo.trixnity.client.room.getTimelineEventReactionAggregation
import net.folivo.trixnity.client.room.message.react
import net.folivo.trixnity.client.store.RoomOutboxMessage
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.isReplaced
import net.folivo.trixnity.client.store.membership
import net.folivo.trixnity.client.store.originalName
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
import net.folivo.trixnity.core.model.events.m.FullyReadEventContent
import net.folivo.trixnity.core.model.events.m.ReactionEventContent
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import org.koin.core.component.get
import kotlin.time.Duration.Companion.seconds


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
        canLoadBefore: Flow<Boolean>,
        canLoadAfter: Flow<Boolean>,
        getReceipts: (RoomId) -> Flow<Map<EventId, Set<UserId>>>,
        onMessageEdited: (EventId) -> Unit,
        onMessageRepliedTo: (EventId) -> Unit,
        onMessageReportTo: (EventId) -> Unit,
        onOpenMention: OpenMentionCallback,
        onOpenMedia: OpenMediaCallback,
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
            canLoadBefore = canLoadBefore,
            canLoadAfter = canLoadAfter,
            getReceipts = getReceipts,
            onMessageEdited = onMessageEdited,
            onMessageRepliedTo = onMessageRepliedTo,
            onMessageReportTo = onMessageReportTo,
            onOpenMention = onOpenMention,
            onOpenMedia = onOpenMedia,
        )

    companion object : TimelineElementHolderViewModelFactory
}

interface TimelineElementHolderViewModel : BaseTimelineElementHolderViewModel {
    val eventId: EventId

    val hasUnreadMarker: StateFlow<Boolean>
    val hasLoadingIndicatorBefore: StateFlow<Boolean>
    val hasLoadingIndicatorAfter: StateFlow<Boolean>

    val isRead: StateFlow<Boolean>
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

    fun edit()
    fun endEdit()
    fun redact()
    fun replyTo()
    fun endReplyTo()
    fun report()
    fun addReaction(reaction: String)
    fun removeReaction(reaction: ReactionEvent)

    data class ReactionEvent(
        val eventId: EventId,
        val sender: UserInfoElement,
        val isMe: Boolean,
    )
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class TimelineElementHolderViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val key: String,
    protected val timelineEventFlow: Flow<TimelineEvent>,
    protected val roomId: RoomId,
    override val eventId: EventId,
    private val senderUserId: UserId,
    override val formattedDate: String,
    override val formattedTime: String,
    canLoadBefore: Flow<Boolean>,
    canLoadAfter: Flow<Boolean>,
    private val getReceipts: (RoomId) -> Flow<Map<EventId, Set<UserId>>>,
    private val onMessageEdited: (EventId) -> Unit,
    private val onMessageRepliedTo: (EventId) -> Unit,
    private val onMessageReportTo: (EventId) -> Unit,
    private val onOpenMention: OpenMentionCallback,
    private val onOpenMedia: OpenMediaCallback,
) : TimelineElementHolderViewModel, MatrixClientViewModelContext by viewModelContext {
    private val config = get<MatrixMessengerConfiguration>()

    private val initials = get<Initials>()
    private val timelineElementViewModelFactorySelector = get<TimelineElementViewModelFactorySelector>()
    private val repliedTimelineElementHolderViewModelFactory = get<RepliedTimelineElementHolderViewModelFactory>()

    override val hasLoadingIndicatorBefore =
        canLoadBefore.stateIn(coroutineScope, WhileSubscribed(), false)
    override val hasLoadingIndicatorAfter =
        canLoadAfter.stateIn(coroutineScope, WhileSubscribed(), false)

    override val hasUnreadMarker: StateFlow<Boolean> =
        matrixClient.room.getAccountData<FullyReadEventContent>(roomId).transformLatest { fullyReadEvent ->
            if (fullyReadEvent?.eventId == eventId) {
                log.trace { "start compute unread marker at $eventId" }
                matrixClient.room.getTimelineEvents(roomId, eventId, Direction.FORWARDS)
                    .first { timelineEventFlow ->
                        val timelineEvent = timelineEventFlow.first()
                        val isByMe = timelineEvent.event.sender == matrixClient.userId
                        val origEventContent = timelineEvent.event.content
                        timelineElementViewModelFactorySelector.supports(origEventContent)
                                && isByMe.not()
                    }
                log.debug { "enable unread marker at $eventId" }
                emit(true)
            } else emit(false)
        }.stateIn(coroutineScope, WhileSubscribed(), false)

    override val isReplaced: StateFlow<Boolean> =
        timelineEventFlow.map { it.isReplaced == true }
            .stateIn(coroutineScope, WhileSubscribed(), false)

    override val canBeReactedTo: StateFlow<Boolean> =
        combine(
            timelineEventFlow,
            matrixClient.user.canSendEvent<ReactionEventContent>(roomId)
        ) { timelineEvent, canSendReactEvent ->
            timelineEvent.content?.getOrNull() !is RedactedEventContent && canSendReactEvent
        }.stateIn(coroutineScope, WhileSubscribed(), false)

    private val _editInProgress = MutableStateFlow(false)
    private val _redactionInProgress = MutableStateFlow(false)
    override val redactionInProgress: StateFlow<Boolean> = _redactionInProgress.asStateFlow()
    private val _redactionError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val redactionError: StateFlow<String?> = _redactionError.asStateFlow()
    override val canBeRepliedTo: StateFlow<Boolean> =
        matrixClient.user.canSendEvent<RoomMessageEventContent>(roomId)
            .stateIn(coroutineScope, WhileSubscribed(), false)

    override val canBeReported: StateFlow<Boolean> =
        matrixClient.user.getById(roomId, userId = matrixClient.userId)
            .map { it?.membership == Membership.JOIN }
            .stateIn(coroutineScope, WhileSubscribed(), false)

    private val _replyToInProgress = MutableStateFlow(false)

    override val highlight: StateFlow<Boolean> =
        combine(_editInProgress, _replyToInProgress) { editInProgress, replyToInProgress ->
            editInProgress || replyToInProgress
        }.stateIn(coroutineScope, WhileSubscribed(), false)

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
                childContext("element", lifecycle),
                content,
                roomId,
                EventIdOrTransactionId(eventId),
                onOpenMention,
                onOpenMedia,
            ).also {
                elementCache.value = TimelineElementViewModelWrapper(it, lifecycle)
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(5.seconds), null)

    override val repliedElement: StateFlow<RepliedTimelineElementHolderViewModel?> =
        flow {
            // we don't need to subscribe for changes or manage the child lifecycle as a reply cannot be changed in Matrix.
            val eventContent = timelineEventFlow.first().event.content
            if (eventContent !is MessageEventContent) return@flow
            val repliedEventId = eventContent.relatesTo?.replyTo?.eventId
            if (repliedEventId == null) return@flow
            emit(
                repliedTimelineElementHolderViewModelFactory.create(
                    childContext("repliedElement"),
                    matrixClient.room.getTimelineEvent(roomId, repliedEventId),
                    roomId,
                    repliedEventId,
                    onOpenMention,
                    onOpenMedia
                )
            )
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    override val sender: StateFlow<UserInfoElement?> =
        matrixClient.user.getById(roomId, senderUserId).map { user ->
            user?.toUserInfoElement(matrixClient, initials, config.avatarMaxSize)
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    override val isByMe: Boolean = senderUserId == userId

    override val isFirstInUserSequence: StateFlow<Boolean?> =
        flow {
            val timelineEvent = matrixClient.room.getTimelineEvents(roomId, eventId, Direction.BACKWARDS)
                .map { it.first() }
                .firstOrNull { timelineEvent ->
                    timelineElementViewModelFactorySelector.supports(timelineEvent.event.content)
                }
            emit(timelineEvent?.sender != senderUserId)
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    private sealed interface IsReadSearchResult {
        data class Read(val readBy: Set<UserId>) : IsReadSearchResult
        data class RoomUpgraded(val roomId: RoomId, val eventId: EventId) : IsReadSearchResult
    }

    private fun isReadSearch(roomId: RoomId, eventId: EventId): Flow<IsReadSearchResult> =
        getReceipts(roomId).transformLatest { receipts ->
            matrixClient.room.getTimelineEvents(roomId, eventId, Direction.FORWARDS)
                .collect {
                    val timelineEvent = it.first()
                    val sender = timelineEvent.sender
                    val currentEventId = timelineEvent.eventId
                    val currentRoomId = timelineEvent.roomId
                    val foundReceipts = receipts[currentEventId].orEmpty() - sender
                    when {
                        foundReceipts.isNotEmpty() -> emit(IsReadSearchResult.Read(foundReceipts))
                        sender != senderUserId && sender != userId -> emit(IsReadSearchResult.Read(setOf(sender)))
                        timelineEvent.roomId != roomId ->
                            emit(IsReadSearchResult.RoomUpgraded(currentRoomId, currentEventId))

                        else -> {}
                    }
                }
        }

    private tailrec suspend fun suspendUntilRead(roomId: RoomId, eventId: EventId): Boolean =
        when (val result = isReadSearch(roomId, eventId).first()) {
            is IsReadSearchResult.Read -> true
            is IsReadSearchResult.RoomUpgraded -> suspendUntilRead(result.roomId, result.eventId)
        }

    override val isRead: StateFlow<Boolean> =
        flow {
            suspendUntilRead(roomId, eventId)
            emit(true)
        }.stateIn(coroutineScope, WhileSubscribed(), false)

    private fun isReadBy(
        roomId: RoomId,
        eventId: EventId,
    ): Flow<UserId> =
        flow {
            var result: IsReadSearchResult.RoomUpgraded? = null
            while (currentCoroutineContext().isActive) {
                val (nextRoomId, nextEventId) = if (result != null) result.roomId to result.eventId else roomId to eventId
                result = isReadSearch(nextRoomId, nextEventId)
                    .onEach { if (it is IsReadSearchResult.Read) it.readBy.forEach { userId -> emit(userId) } }
                    .filterIsInstance<IsReadSearchResult.RoomUpgraded>()
                    .first()
            }
        }

    override val isReadBy: StateFlow<List<UserInfoElement>?> =
        flow {
            val cumulatedReads = mutableSetOf<UserId>()
            isReadBy(roomId, eventId)
                .collect {
                    cumulatedReads.add(it)
                    emit(cumulatedReads.toSet())
                }
        }.flatMapLatest { userIds ->
            combine(userIds.map { userId -> matrixClient.user.getById(roomId, userId) }) { roomUsers ->
                roomUsers.mapNotNull { user ->
                    if (user == null) return@mapNotNull null
                    user.toUserInfoElement(matrixClient, initials, config.avatarMaxSize)
                }
            }
        }.stateIn(coroutineScope, WhileSubscribed(), null)

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
                                                image = null
                                            ),
                                            isMe = event.sender == matrixClient.userId,
                                        )
                                    }
                                }.toSet()
                            }
                        }
                    }
            }
        }.stateIn(coroutineScope, WhileSubscribed(), emptyMap())

    private fun findInviterId(
        timelineEvent: TimelineEvent?,
    ): Flow<UserId?> {
        return timelineEvent?.let { te ->
            val event = te.event
            val content = event.content
            if (event is StateEvent &&
                event.stateKey == matrixClient.userId.full &&
                content is MemberEventContent &&
                content.membership == Membership.INVITE
            ) {
                flowOf(event.sender)
            } else {
                matrixClient.room.getNextTimelineEvent(te)
                    ?.flatMapLatest { nextTimelineEvent ->
                        findInviterId(nextTimelineEvent)
                    }
                    ?: flowOf(null)
            }
        } ?: flowOf(null)
    }

    override val canBeEdited: StateFlow<Boolean> = timelineEventFlow
        .filterNotNull()
        .map {
            it.event.sender == matrixClient.userId && it.content?.getOrNull() is TextBased
        }
        .stateIn(coroutineScope, WhileSubscribed(), false)

    override fun edit() {
        _editInProgress.value = true
        coroutineScope.launch {
            timelineEventFlow.first().eventId.let { onMessageEdited(it) }
        }
    }

    override fun endEdit() {
        _editInProgress.value = false
    }

    override val canBeRedacted: StateFlow<Boolean> = channelFlow {
        timelineEventFlow
            .filterNotNull()
            .flatMapLatest { timelineEvent ->
                matrixClient.user.canRedactEvent(
                    timelineEvent.roomId,
                    timelineEvent.eventId
                )
            }
            .collectLatest { send(it) }
    }.stateIn(coroutineScope, WhileSubscribed(), false)

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

    override fun replyTo() {
        _replyToInProgress.value = true
        coroutineScope.launch {
            onMessageRepliedTo(eventId)
        }
    }

    override fun endReplyTo() {
        _replyToInProgress.value = false
    }

    override fun report() {
        coroutineScope.launch {
            onMessageReportTo(eventId)
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
        "TimelineElementViewModel(showLoadingIndicator=${hasLoadingIndicatorBefore.value}" +
                ", shouldShowUnreadMarker=${hasUnreadMarker.value})"
}

class PreviewTimelineElementViewModel1 : TimelineElementHolderViewModel {
    override val eventId: EventId = EventId("\$1:localhost")
    override val key: String = eventId.full
    override val element: StateFlow<TimelineElementViewModel<*>?> =
        MutableStateFlow(object : RoomMessageTimelineElementViewModel.TextBased.Text {
            override val body: String = "Hello everyone!"
            override val formattedBody: String = "Hello <b/>everyone!"
            override val mentionsInBody: Map<IntRange, StateFlow<TimelineElementMention>> = mapOf()
            override val mentionsInFormattedBody: Map<IntRange, StateFlow<TimelineElementMention>> = mapOf()
            override fun openMention(timelineElementMention: TimelineElementMention) {}
        })
    override val isFirstInUserSequence: StateFlow<Boolean?> = MutableStateFlow(false)
    override val formattedTime: String = "12:12"
    override val formattedDate: String = "21.11.2024"
    override val isByMe: Boolean = true
    override val sender: StateFlow<UserInfoElement?> = MutableStateFlow(null)
    override val repliedElement: StateFlow<RepliedTimelineElementHolderViewModel?> = MutableStateFlow(null)
    override val hasUnreadMarker: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasLoadingIndicatorBefore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasLoadingIndicatorAfter: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isRead: StateFlow<Boolean> = MutableStateFlow(false)
    override val isReadBy: StateFlow<List<UserInfoElement>> = MutableStateFlow(listOf())
    override val canBeReactedTo: StateFlow<Boolean> = MutableStateFlow(false)
    override val isReplaced: StateFlow<Boolean> = MutableStateFlow(false)
    override val canBeEdited: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canBeRedacted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val canBeRepliedTo: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val canBeReported: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val reactions: StateFlow<Map<String, Set<TimelineElementHolderViewModel.ReactionEvent>>> =
        MutableStateFlow(emptyMap())
    override val highlight: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override fun edit() {}
    override fun endEdit() {}
    override fun redact() {}
    override fun replyTo() {}
    override fun endReplyTo() {}
    override fun report() {}
    override fun addReaction(reaction: String) {}
    override fun removeReaction(reaction: TimelineElementHolderViewModel.ReactionEvent) {}
}

class PreviewTimelineElementViewModel2 : TimelineElementHolderViewModel {
    override val eventId: EventId = EventId("\$2:localhost")
    override val key: String = eventId.full
    override val element: StateFlow<TimelineElementViewModel<*>?> =
        MutableStateFlow(object : RoomMessageTimelineElementViewModel.TextBased.Text {
            override val body: String = "Hello too!"
            override val formattedBody: String = "Hello <b/>too!"
            override val mentionsInBody: Map<IntRange, StateFlow<TimelineElementMention>> = mapOf()
            override val mentionsInFormattedBody: Map<IntRange, StateFlow<TimelineElementMention>> = mapOf()
            override fun openMention(timelineElementMention: TimelineElementMention) {}
        })
    override val isFirstInUserSequence: StateFlow<Boolean?> = MutableStateFlow(false)
    override val formattedTime: String = "12:24"
    override val formattedDate: String = "21.11.2024"
    override val isByMe: Boolean = false
    override val sender: StateFlow<UserInfoElement?> =
        MutableStateFlow(UserInfoElement("Bob", UserId("bob", "server"), "B"))
    override val repliedElement: StateFlow<RepliedTimelineElementHolderViewModel?> = MutableStateFlow(null)
    override val hasUnreadMarker: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasLoadingIndicatorBefore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasLoadingIndicatorAfter: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isRead: StateFlow<Boolean> = MutableStateFlow(false)
    override val isReadBy: StateFlow<List<UserInfoElement>> = MutableStateFlow(listOf())
    override val canBeReactedTo: StateFlow<Boolean> = MutableStateFlow(false)
    override val isReplaced: StateFlow<Boolean> = MutableStateFlow(false)
    override val canBeEdited: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canBeRedacted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val canBeRepliedTo: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val canBeReported: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val reactions: StateFlow<Map<String, Set<TimelineElementHolderViewModel.ReactionEvent>>> =
        MutableStateFlow(emptyMap())
    override val highlight: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override fun edit() {}
    override fun endEdit() {}
    override fun redact() {}
    override fun replyTo() {}
    override fun endReplyTo() {}
    override fun report() {}
    override fun addReaction(reaction: String) {}
    override fun removeReaction(reaction: TimelineElementHolderViewModel.ReactionEvent) {}
}
