package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.benasher44.uuid.uuid4
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.MessageMention
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.isDifferentDay
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.react
import net.folivo.trixnity.client.store.RoomOutboxMessage
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.avatarUrl
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.isReplaced
import net.folivo.trixnity.client.store.membership
import net.folivo.trixnity.client.store.roomId
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.canSendEvent
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.RedactedEventContent
import net.folivo.trixnity.core.model.events.RoomEventContent
import net.folivo.trixnity.core.model.events.m.ReactionEventContent
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import org.koin.core.component.get
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


private val log = KotlinLogging.logger { }

interface TimelineElementHolderViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        key: String,
        timelineEventFlow: Flow<TimelineEvent?>,
        selectedRoomId: RoomId,
        eventId: EventId,
        canLoadMoreBefore: Flow<Boolean>,
        canLoadMoreAfter: Flow<Boolean>,
        isDirect: StateFlow<Boolean>,
        isReadFlow: Flow<Boolean>,
        shouldShowUnreadMarkerFlow: Flow<Boolean>,
        readBy: Flow<List<String>>,
        reactions: Flow<Map<String, Set<TimelineElementHolderViewModel.ReactionEvent>>>,
        onMessageEdited: (EventId) -> Unit,
        onMessageRepliedTo: (EventId) -> Unit,
        onMessageReportTo: (EventId) -> Unit,
        onOpenModal: OpenModalCallback,
        onOpenMention: OpenMentionCallback,
    ): TimelineElementHolderViewModel =
        TimelineElementHolderViewModelImpl(
            viewModelContext,
            key,
            timelineEventFlow,
            selectedRoomId,
            eventId,
            canLoadMoreBefore,
            canLoadMoreAfter,
            isDirect,
            isReadFlow,
            readBy,
            reactions,
            shouldShowUnreadMarkerFlow,
            onMessageEdited,
            onMessageRepliedTo,
            onOpenModal = onOpenModal,
            onMessageReportTo = onMessageReportTo,
            onOpenMention = onOpenMention,
        )

    companion object : TimelineElementHolderViewModelFactory
}

interface TimelineElementHolderViewModel : BaseTimelineElementHolderViewModel {
    val eventId: EventId

    val shouldShowUnreadMarkerFlow: StateFlow<Boolean>
    val showLoadingIndicatorBefore: StateFlow<Boolean>
    val showLoadingIndicatorAfter: StateFlow<Boolean>

    val isDirect: StateFlow<Boolean>
    val isRead: StateFlow<Boolean>

    val reactionsOpen: MutableStateFlow<Boolean>
    val canBeReactedTo: StateFlow<Boolean>

    val infoOpen: MutableStateFlow<Boolean>
    val canGetInfo: StateFlow<Boolean>

    val isReplaced: StateFlow<Boolean>
    val canBeEdited: StateFlow<Boolean>
    val canBeRedacted: StateFlow<Boolean>
    val redactionInProgress: StateFlow<Boolean>
    val redactionError: StateFlow<String?>
    val canBeRepliedTo: StateFlow<Boolean>
    val highlight: StateFlow<Boolean>
    val canBeReported: StateFlow<Boolean>
    val reactions: StateFlow<Map<String, Set<ReactionEvent>>>
    fun edit()
    fun endEdit()
    fun redact()
    fun replyTo()
    fun endReplyTo()
    fun reportTo()
    fun addReaction(reaction: String)
    fun removeReaction(reaction: ReactionEvent)

    /** returns no Flow! -> for current value, recompute every time needed (Flow computation would be expensive) */
    suspend fun isReadBy(): List<String>

    data class ReactionEvent(
        val eventId: EventId,
        val sender: UserInfoElement,
        val isMe: Boolean,
        val timestamp: Instant?,
    )
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
open class TimelineElementHolderViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val key: String,
    protected val timelineEventFlow: Flow<TimelineEvent?>,
    protected val selectedRoomId: RoomId,
    override val eventId: EventId,
    canLoadMoreBefore: Flow<Boolean>,
    canLoadMoreAfter: Flow<Boolean>,
    override val isDirect: StateFlow<Boolean>,
    isReadFlow: Flow<Boolean>,
    private val readBy: Flow<List<String>>,
    _reactions: Flow<Map<String, Set<TimelineElementHolderViewModel.ReactionEvent>>>,
    shouldShowUnreadMarkerFlow: Flow<Boolean>,
    private val onMessageEdited: (EventId) -> Unit,
    private val onMessageRepliedTo: (EventId) -> Unit,
    private val onMessageReportTo: (EventId) -> Unit,
    private val onOpenModal: OpenModalCallback,
    private val onOpenMention: OpenMentionCallback,
) : TimelineElementHolderViewModel, MatrixClientViewModelContext by viewModelContext {
    private val timelineElementRules = get<TimelineElementRules>()
    private val timelineSubViewmodelFactory = get<TimelineEventSubViewmodelFactory>()
    private val initials = get<Initials>()

    override val showLoadingIndicatorBefore =
        canLoadMoreBefore.stateIn(coroutineScope, WhileSubscribed(), false)
    override val showLoadingIndicatorAfter =
        canLoadMoreAfter.stateIn(coroutineScope, WhileSubscribed(), false)

    override val shouldShowUnreadMarkerFlow: StateFlow<Boolean> =
        shouldShowUnreadMarkerFlow.stateIn(coroutineScope, WhileSubscribed(), false)

    override val isRead: StateFlow<Boolean> =
        isReadFlow.stateIn(coroutineScope, WhileSubscribed(), false)
    override val isReplaced: StateFlow<Boolean> =
        timelineEventFlow.map { it?.isReplaced == true }
            .stateIn(coroutineScope, WhileSubscribed(), false)

    override val reactionsOpen = MutableStateFlow(false)
    override val canBeReactedTo: StateFlow<Boolean> =
        combine(
            timelineEventFlow,
            matrixClient.user.canSendEvent<ReactionEventContent>(selectedRoomId)
        ) { timelineEvent, canSendReactEvent ->
            timelineEvent?.content?.getOrNull() !is RedactedEventContent && canSendReactEvent
        }.stateIn(coroutineScope, WhileSubscribed(), false)

    private val _editInProgress = MutableStateFlow(false)
    private val _redactionInProgress = MutableStateFlow(false)
    override val redactionInProgress: StateFlow<Boolean> = _redactionInProgress.asStateFlow()
    private val _redactionError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val redactionError: StateFlow<String?> = _redactionError.asStateFlow()
    override val canBeRepliedTo: StateFlow<Boolean> =
        matrixClient.user.canSendEvent<RoomMessageEventContent>(selectedRoomId)
            .stateIn(coroutineScope, WhileSubscribed(), false)

    override val canBeReported: StateFlow<Boolean> =
        matrixClient.user.getById(selectedRoomId, userId = matrixClient.userId)
            .map { it?.membership == Membership.JOIN }
            .stateIn(coroutineScope, WhileSubscribed(), false)

    override val infoOpen = MutableStateFlow(false)
    override val canGetInfo: StateFlow<Boolean> = timelineEventFlow
        .filterNotNull()
        .map {
            it.event.sender == matrixClient.userId
        }
        .stateIn(coroutineScope, WhileSubscribed(), false)

    private val _replyToInProgress = MutableStateFlow(false)

    private val subViewModelCache = MutableStateFlow<Pair<Int, Flow<BaseTimelineElementViewModel>>?>(null)

    override val highlight: StateFlow<Boolean> =
        combine(_editInProgress, _replyToInProgress) { editInProgress, replyToInProgress ->
            editInProgress || replyToInProgress
        }.stateIn(coroutineScope, WhileSubscribed(), false)

    // since this is a rather expensive operation do not compute as a flow
    override suspend fun isReadBy(): List<String> = readBy.first()

    private fun getNewContentIfAvailable(msg: RoomOutboxMessage<*>?) =
        (msg?.content?.relatesTo as? RelatesTo.Replace)?.takeIf { it.eventId == eventId }?.newContent

    private val newContentIfReplaced = matrixClient.room.getOutbox(selectedRoomId)
        .flatMapLatest { roomOutboxMessageFlows ->
            val mappedFlows = roomOutboxMessageFlows
                .map { roomOutboxMessageFlow ->
                    roomOutboxMessageFlow.map(::getNewContentIfAvailable).onStart { emit(null) }
                }
            combine(
                mappedFlows
            ) {
                it.filterNotNull().lastOrNull()
            }.onStart { emit(null) }
        }

    override val timelineElementViewModel = combine(
        timelineEventFlow
            .filterNotNull()
            .distinctUntilChanged(),
        timelineEventFlow
            .filterNotNull()
            .flatMapLatest {
                findPreviousVisibleTimelineEvent(it) ?: flowOf(null)
            }
            .distinctUntilChanged(),
        timelineEventFlow
            .filterNotNull()
            .map { it.content }
            .debounce {
                // in case we are still decrypting, wait if the decryption finishes in the first x milliseconds, if not, show to the user; otherwise return the content immediately
                if (it == null) 400.milliseconds else 0.milliseconds
            }
            .distinctUntilChanged(),
        newContentIfReplaced
            .distinctUntilChanged(),
    ) { timelineEvent, previousTimelineEvent, contentResult, newContent ->

        log.trace { "compute timelineElementViewModel ($timelineEvent, $previousTimelineEvent, $contentResult)" }
        val subViewModel = subViewModelCache.value
        if (subViewModel != null && subViewModel.first == keyFn(timelineEvent, newContent ?: contentResult?.getOrNull())) {
            subViewModel.second
        } else {
            val sender = matrixClient.user.getById(selectedRoomId, timelineEvent.event.sender)
                .map { user ->
                    UserInfoElement(
                        name = user?.name ?: timelineEvent.event.sender.full,
                        initials = user?.name?.let(initials::compute),
                        userId = user?.userId ?: timelineEvent.event.sender,
                        image = user?.avatarUrl?.let { avatarUrl ->
                            matrixClient.media.getThumbnail(
                                avatarUrl,
                                avatarSize().toLong(),
                                avatarSize().toLong()
                            )
                                .onFailure { exc ->
                                    if (exc !is CancellationException) {
                                        log.error(exc) { "Cannot load avatar image for user '${user.name}'." }
                                    }
                                }
                                .getOrNull()
                        },
                    )
                }

            val invitation =
                if (timelineEvent.previousEventId == null) {
                    findInviterId(timelineEvent).flatMapLatest { inviterId ->
                        getInviterDisplayName(inviterId)
                    }
                } else flowOf(null)

            val event = timelineEvent.event
            val content = newContent ?: (contentResult?.fold(
                    onSuccess = { it },
                    onFailure = {
                        log.error(it) { "cannot decrypt message event" }
                        event.content
                    }
                ) ?: event.content)

            timelineSubViewmodelFactory.createEventSubViewmodel(
                this,
                timelineEventFlow,
                selectedRoomId,
                content,
                previousTimelineEvent,
                sender,
                invitation,
                isDirect,
                onOpenModal,
                onOpenMention,
            ).also {
                subViewModelCache.value = keyFn(timelineEvent, content) to it
            }
        }
    }.flatMapLatest { it }
        .stateIn(coroutineScope, Lazily, null) // we need Lazily here as otherwise this might be computed multiple times

    private fun keyFn(timelineEvent: TimelineEvent, content: RoomEventContent?) =
        timelineEvent.eventId.hashCode() + (content?.hashCode() ?: 0)

    override val reactions =
        timelineEventFlow.flatMapLatest { timelineEvent ->
            when (timelineEvent?.content?.getOrNull()) {
                is RedactedEventContent, is EncryptionEventContent -> flowOf(emptyMap())
                else -> _reactions
            }
        }.stateIn(coroutineScope, WhileSubscribed(), emptyMap())

    private suspend fun findPreviousVisibleTimelineEvent(timelineEvent: TimelineEvent): Flow<TimelineEvent?>? {
        val previousTimelineEventOrNull = matrixClient.room.getPreviousTimelineEvent(timelineEvent)
        return previousTimelineEventOrNull?.first() //do NOT  wait until the previous event has been decrypted
            ?.let { previousTimelineEvent ->
                if (timelineElementRules.areVisible.any { it.isInstance(previousTimelineEvent.event.content) }) {
                    previousTimelineEventOrNull
                } else {
                    findPreviousVisibleTimelineEvent(previousTimelineEvent)
                }
            }
    }

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

    private fun getInviterDisplayName(
        inviterId: UserId?,
    ): Flow<String?> {
        return inviterId?.let {
            matrixClient.user.getById(selectedRoomId, inviterId)
                .map { user -> user?.name ?: inviterId.full }
        }
            ?.map { inviter -> i18n.invitationFrom(inviter) }
            ?: flowOf(null)
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
            timelineEventFlow.first()?.eventId?.let { onMessageEdited(it) }
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
                timelineEventFlow.first()?.let { timelineEvent ->
                    if (matrixClient.user.canRedactEvent(
                            timelineEvent.roomId,
                            timelineEvent.eventId
                        ).first()
                    ) {
                        launch {
                            _redactionInProgress.value = true
                            _redactionError.value = null
                            matrixClient.api.room.redactEvent(
                                selectedRoomId,
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
                } ?: log.warn { "try to redact a timeline event, but it is 'null'" }
            }
        } else {
            log.warn { "try to redact timeline event $eventId, but is already marked for redaction" }
        }
    }

    override fun replyTo() {
        _replyToInProgress.value = true
        coroutineScope.launch {
            timelineEventFlow.first()?.event?.let {
                if (it is MessageEvent<*>) onMessageRepliedTo(it.id)
                else log.warn { "Try to reply to non-message event is not allowed." }
            }
        }
    }

    override fun endReplyTo() {
        _replyToInProgress.value = false
    }

    override fun reportTo() {
        coroutineScope.launch {
            timelineEventFlow.first()?.event?.let {
                log.trace { "reportToMessage initiated ${it.id}" }
                onMessageReportTo(it.id)
            }
        }
    }

    override fun addReaction(reaction: String) {
        coroutineScope.launch {
            matrixClient.room.sendMessage(selectedRoomId) {
                react(eventId, reaction)
            }
        }
    }

    override fun removeReaction(reaction: TimelineElementHolderViewModel.ReactionEvent) {
        coroutineScope.launch {
            matrixClient.api.room.redactEvent(
                selectedRoomId,
                reaction.eventId,
                txnId = uuid4().toString()
            )
        }
    }

    override fun toString(): String =
        "TimelineElementViewModel(showLoadingIndicator=${showLoadingIndicatorBefore.value}" +
                ", shouldShowUnreadMarker=${shouldShowUnreadMarkerFlow.value}" +
                ", isDirect=${isDirect.value})"
}

class PreviewTimelineElementViewModel1 : TimelineElementHolderViewModel {
    override val eventId: EventId = EventId("\$1:localhost")
    override val key: String = eventId.full
    override val timelineElementViewModel: StateFlow<BaseTimelineElementViewModel?> =
        MutableStateFlow(object : TextBasedViewModel {
            override val fallbackMessage: String = "Hello everyone!"
            override val referencedMessage: MutableStateFlow<ReferencedMessage?> =
                MutableStateFlow(null)
            override val message: String = "Hello everyone!"
            override val formattedBody: String = "Hello <b>everyone!</b>"
            override val isByMe: Boolean = false
            override val showChatBubbleEdge: Boolean = true
            override val showBigGap: Boolean = true
            override val showSender: MutableStateFlow<Boolean> = MutableStateFlow(true)
            override val sender: MutableStateFlow<UserInfoElement> =
                MutableStateFlow(UserInfoElement("Benedict", UserId("benedict:matrix.org")))
            override val formattedTime: String = "11:04"
            override val invitation: MutableStateFlow<String?> = MutableStateFlow(null)
            override val formattedDate: String = "23.11.22"
            override val showDateAbove: Boolean = true
            override val mentionsInMessage: Map<IntRange, StateFlow<MessageMention>> = mapOf()
            override val mentionsInFormattedBody: Map<IntRange, StateFlow<MessageMention>> = mapOf()
            override fun openMention(messageMention: MessageMention) {
            }
        })
    override val shouldShowUnreadMarkerFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLoadingIndicatorBefore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLoadingIndicatorAfter: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isDirect: StateFlow<Boolean> = MutableStateFlow(false)
    override val isRead: StateFlow<Boolean> = MutableStateFlow(false)
    override val reactionsOpen: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canBeReactedTo: StateFlow<Boolean> = MutableStateFlow(false)
    override val infoOpen: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canGetInfo: StateFlow<Boolean> = MutableStateFlow(false)
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
    override fun edit() {
    }

    override fun endEdit() {
    }

    override fun redact() {
    }

    override fun replyTo() {
    }

    override fun endReplyTo() {
    }

    override fun reportTo() {
    }

    override fun addReaction(reaction: String) {
    }

    override fun removeReaction(reaction: TimelineElementHolderViewModel.ReactionEvent) {
    }

    override suspend fun isReadBy(): List<String> = listOf("Bob", "Alice")
}

class PreviewTimelineElementViewModel2 : TimelineElementHolderViewModel {
    override val eventId: EventId = EventId("\$2:localhost")
    override val key: String = eventId.full
    override val timelineElementViewModel: MutableStateFlow<BaseTimelineElementViewModel?> =
        MutableStateFlow(null)
    override val shouldShowUnreadMarkerFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLoadingIndicatorBefore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLoadingIndicatorAfter: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isDirect: StateFlow<Boolean> = MutableStateFlow(false)
    override val isRead: StateFlow<Boolean> = MutableStateFlow(false)
    override val reactionsOpen: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canBeReactedTo: StateFlow<Boolean> = MutableStateFlow(false)
    override val infoOpen: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canGetInfo: StateFlow<Boolean> = MutableStateFlow(true)
    override val isReplaced: StateFlow<Boolean> = MutableStateFlow(false)
    override val canBeEdited: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canBeRedacted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val canBeRepliedTo: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val canBeReported: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val highlight: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val reactions: StateFlow<Map<String, Set<TimelineElementHolderViewModel.ReactionEvent>>> =
        MutableStateFlow(emptyMap())

    override fun edit() {
    }

    override fun endEdit() {
    }

    override fun redact() {
    }

    override fun replyTo() {
    }

    override fun endReplyTo() {
    }

    override fun reportTo() {
    }

    override fun addReaction(reaction: String) {
    }

    override fun removeReaction(reaction: TimelineElementHolderViewModel.ReactionEvent) {
    }

    override suspend fun isReadBy(): List<String> = listOf("Bob", "Alice")

    init {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            delay(3.seconds)
            timelineElementViewModel.value = object : TextBasedViewModel {
                override val fallbackMessage: String = "I have good news."
                override val referencedMessage: MutableStateFlow<ReferencedMessage?> =
                    MutableStateFlow(null)
                override val message: String = "I have good news."
                override val formattedBody: String = "<b>I</b> have good news."
                override val isByMe: Boolean = false
                override val showChatBubbleEdge: Boolean = false
                override val showBigGap: Boolean = false
                override val showSender: MutableStateFlow<Boolean> = MutableStateFlow(false)
                override val sender: MutableStateFlow<UserInfoElement> =
                    MutableStateFlow(UserInfoElement("Benedict", UserId("benedict:matrix.org")))
                override val formattedTime: String = "11:05"
                override val invitation: MutableStateFlow<String?> = MutableStateFlow(null)
                override val formattedDate: String = "23.11.22"
                override val showDateAbove: Boolean = false
                override val mentionsInMessage: Map<IntRange, StateFlow<MessageMention>> = mapOf()
                override val mentionsInFormattedBody: Map<IntRange, StateFlow<MessageMention>> = mapOf()
                override fun openMention(messageMention: MessageMention) {
                }
            }
        }
    }
}

class TimelineElementHolderViewModelHelper(private val timeZone: TimeZone) {

    fun localDateTimeOf(event: RoomEvent<*>): LocalDateTime {
        val timestamp = event.originTimestamp
        requireNotNull(timestamp) // should not happen as only RoomEvents and StateEvents are possible
        return Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone)
    }

    fun isPreviousBySomeoneElseOrOtherDay(
        previousTimelineEvent: TimelineEvent?,
        event: RoomEvent<*>,
    ): Pair<Boolean, Boolean> = previousTimelineEvent?.let {
        val (_, datesAreDifferent) = compareDates(event, previousTimelineEvent)
        Pair(
            event.sender != previousTimelineEvent.event.sender || previousTimelineEvent.event is StateEvent,
            datesAreDifferent,
        )
    } ?: Pair(true, true) // first message is treated like it is different to the previous one

    fun compareDates(
        event: RoomEvent<*>,
        previousTimelineEvent: TimelineEvent
    ): Pair<LocalDateTime, Boolean> {
        val thisLocalDateTime = localDateTimeOf(event)
        val previousLocalDateTime = localDateTimeOf(previousTimelineEvent.event)
        return Pair(thisLocalDateTime, thisLocalDateTime.isDifferentDay(previousLocalDateTime))
    }

    fun isByMe(viewModelContext: MatrixClientViewModelContext, timelineEvent: TimelineEvent): Boolean =
        timelineEvent.event.sender == viewModelContext.matrixClient.userId

    fun createNullTimelineElementViewModel(
        viewModelContext: MatrixClientViewModelContext,
        invitation: Flow<String?>,
    ) = NullTimelineElementViewModel(
        viewModelContext = viewModelContext,
        formattedDate = "",
        showDateAbove = false,
        invitation = invitation,
    )
}
