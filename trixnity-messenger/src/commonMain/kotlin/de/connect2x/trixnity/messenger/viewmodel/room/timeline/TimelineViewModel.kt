package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.start
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.benasher44.uuid.uuid4
import de.connect2x.trixnity.messenger.util.launchPopWhile
import de.connect2x.trixnity.messenger.util.launchPush
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel.SendAttachmentConfig
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel.SendAttachmentWrapper
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.*
import de.connect2x.trixnity.messenger.viewmodel.util.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.flatten
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.Timeline
import net.folivo.trixnity.client.room.getAccountData
import net.folivo.trixnity.client.store.RoomOutboxMessage
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents.Direction.BACKWARDS
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.Event.MessageEvent
import net.folivo.trixnity.core.model.events.m.FullyReadEventContent
import net.folivo.trixnity.core.model.events.m.ReceiptType.Read
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import org.koin.core.component.get
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger {}

interface TimelineViewModelFactory {
    fun newTimelineViewModel(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        isBackButtonVisible: MutableStateFlow<Boolean>,
        onShowSettings: () -> Unit,
        onBack: () -> Unit,
        onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String) -> Unit,
    ): TimelineViewModel {
        return TimelineViewModelImpl(
            viewModelContext,
            selectedRoomId,
            isBackButtonVisible,
            onShowSettings,
            onBack,
            onOpenModal,
        )
    }
}

/**
 * Handles the timeline of a room and provides a list of view models that represent all elements of the timeline.
 *
 * It handles the marking of elements that are read. In order to work, the UI has to set the [lastVisibleTimelineElement].
 *
 * There are 2 read states: _read_ and _fully_read_. It determines the state of read elements by the following rules:
 *  * _read_ means the user has seen the event
 *    * it is no longer considered for the unread messages number of the room and it will be interpreted by other
 *      clients as "the user has read the event"
 *    * after the room is opened the [lastVisibleTimelineEvent] will be set as _read_
 *    * when the [lastVisibleTimelineEvent] changes, the now visible events are marked as _read_
 *  * _fully_read_ is only used to determine the position of the unread marker; as the unread marker should stay
 *      in place, it is changed less frequently
 *    * it is set implicitly by Trixnity, when the user sends a new message
 *    * it is set to the current value of the _read_ marker when the app loses focus (lifecycle state: pause)
 *    * it is set to the current value of the _read_ marker when the room is changed or the app is closed
 *      (lifecycle state: destroyed)
 */
interface TimelineViewModel {
    val timelineElementViewModels: StateFlow<List<Pair<String, ITimelineElementViewModel>>>

    /**
     * Has to be set by the UI.
     */
    val windowIsFocused: MutableStateFlow<Boolean>

    /**
     * Has to be set by the UI. String is the key from [timelineElementViewModels].
     */
    val lastVisibleTimelineElement: MutableStateFlow<String?>

    /**
     * Has to be set by the UI. String is the key from [timelineElementViewModels].
     */
    val firstVisibleTimelineElement: MutableStateFlow<String?>

    /**
     * Emits a unique String each time the view should scroll to the given key. String is the key from [timelineElementViewModels].
     */
    val scrollTo: Flow<String>
    val stickyDate: StateFlow<String?>
    val selectedMessage: MutableStateFlow<String?>
    val selectedMessageActions: MutableStateFlow<List<Pair<ContextMenuAction, () -> Unit>>>
    val isDirect: StateFlow<Boolean>
    val error: StateFlow<String?>
    val roomHeaderViewModel: RoomHeaderViewModel
    val inputAreaViewModel: InputAreaViewModel
    val sendAttachmentStack: Value<ChildStack<SendAttachmentConfig, SendAttachmentWrapper>>

    /**
     * Only for DnD on desktop: the absolute path of a dragged file.
     */
    val draggedFile: StateFlow<String?>

    fun errorDismiss()
    fun leaveRoom()

    /**
     * Suspends until the last event of the room is present in the timeline.
     */
    fun jumpToEndOfTimeline()

    val loadingBefore: StateFlow<Boolean>
    fun loadBefore()

    fun selectFile(file: String)
    fun dragFile(file: String)
    fun dragFileExit()

    sealed class SendAttachmentWrapper {
        object None : SendAttachmentWrapper()
        class View(val sendAttachmentViewModel: SendAttachmentViewModel) : SendAttachmentWrapper()
    }

    sealed class SendAttachmentConfig : Parcelable {
        @Parcelize
        object None : SendAttachmentConfig()

        @Parcelize
        data class SendAttachmentView(val file: String) : SendAttachmentConfig()
    }

}

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val isBackButtonVisible: MutableStateFlow<Boolean>,
    private val onShowSettings: () -> Unit,
    private val onBack: () -> Unit,
    private val onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String) -> Unit,
) : MatrixClientViewModelContext by viewModelContext, TimelineViewModel {

    init {
        log.debug { "::: init timelineViewModel: $viewModelContext" }
    }

    data class TimelineElementWrapper(
        val key: String,
        val timelineEvent: Flow<TimelineEvent>,
        val viewModel: TimelineElementViewModel,
    )

    private val timelineViewModelConfig = get<TimelineViewModelConfig>()
    private val outerScope = get<CoroutineScope>()

    private val timelineStartFrom = MutableSharedFlow<EventId>(replay = 1)
    private val timeline: SharedFlow<Timeline<TimelineElementWrapper>> =
        timelineStartFrom.mapLatest { startFrom ->
            log.debug { "try init timeline from $startFrom" }
            val newTimeline: Timeline<TimelineElementWrapper> =
                matrixClient.room.getTimeline(selectedRoomId) {
                    computeTimelineElement(it)
                }
            newTimeline.init(startFrom)
            log.debug { "finished init timeline from $startFrom" }
            newTimeline
        }.shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 1)
    private val timelineState =
        timeline.flatMapLatest { it.state }.distinctUntilChanged()
            .shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 1)
    private val timelineElements =
        timelineState.map { it.elements }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), listOf())
    private val timelineEvents =
        timelineElements.map { it.map { it.timelineEvent } }.stateIn(coroutineScope, SharingStarted.Eagerly, listOf())
    private val readEventsFlow: StateFlow<Set<EventId>>
    private val unreadElementFlow: StateFlow<EventId?>

    private val readEvent = MutableStateFlow<EventId?>(null)
    private val fullyReadEvent = MutableStateFlow<EventId?>(null)

    override val timelineElementViewModels: StateFlow<List<Pair<String, ITimelineElementViewModel>>>

    private val timelineEventsViewModelsCache = mutableMapOf<EventId, TimelineElementViewModel>()
    private val outboxLifecycles = mutableListOf<LifecycleRegistry>()

    override val stickyDate: StateFlow<String?>
    override val scrollTo: MutableSharedFlow<String> = MutableSharedFlow()

    override val windowIsFocused: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val firstVisibleTimelineElement: MutableStateFlow<String?> = MutableStateFlow(null)
    override val lastVisibleTimelineElement: MutableStateFlow<String?> = MutableStateFlow(null)

    override val selectedMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    override val selectedMessageActions: MutableStateFlow<List<Pair<ContextMenuAction, () -> Unit>>> =
        MutableStateFlow(listOf())

    override val isDirect: StateFlow<Boolean> = matrixClient.room.getById(selectedRoomId).map { it?.isDirect == true }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

    override val loadingBefore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val draggedFile: MutableStateFlow<String?> = MutableStateFlow(null)

    private val clock = get<Clock>()
    private val directRoom = get<DirectRoom>()
    private val timelineElementRules = get<TimelineElementRules>()

    private val roomUsers =
        matrixClient.user.getAll(selectedRoomId)
            .filterNotNull()
            .flatten()
            .map { it?.values?.filterNotNull().orEmpty() }
            .shareIn(coroutineScope, SharingStarted.WhileSubscribed(), replay = 1)

    override val roomHeaderViewModel: RoomHeaderViewModel =
        get<RoomHeaderViewModelFactory>().newRoomHeaderViewModel(
            viewModelContext = childContext("roomHeaderViewModel"),
            selectedRoomId = selectedRoomId,
            isBackButtonVisible = isBackButtonVisible,
            onBack = onBack,
            onVerifyUser = ::onVerifyUser,
            onShowRoomSettings = onShowSettings,
        )

    override val inputAreaViewModel: InputAreaViewModel =
        get<InputAreaViewModelFactory>().newInputAreaViewModel(
            viewModelContext = childContext("inputAreaViewModel"),
            selectedRoomId = selectedRoomId,
            onMessageEditFinished = ::onMessageEditFinished,
            onMessageReplyToFinished = ::onMessageReplyToFinished,
            onShowAttachmentSendView = ::onShowAttachmentSendView,
        )

    private val sendAttachmentNavigation = StackNavigation<SendAttachmentConfig>()
    override val sendAttachmentStack: Value<ChildStack<SendAttachmentConfig, SendAttachmentWrapper>> = childStack(
        source = sendAttachmentNavigation,
        initialConfiguration = SendAttachmentConfig.None,
        handleBackButton = true,
        childFactory = ::createChild,
        key = "sendAttachmentRouter",
    )

    private fun createChild(
        config: SendAttachmentConfig, componentContext: ComponentContext
    ): SendAttachmentWrapper = when (config) {
        is SendAttachmentConfig.None -> SendAttachmentWrapper.None
        is SendAttachmentConfig.SendAttachmentView -> SendAttachmentWrapper.View(
            get<SendAttachmentViewModelFactory>().newSendAttachmentViewModel(
                viewModelContext = childContext(componentContext),
                file = config.file,
                selectedRoomId = selectedRoomId,
                onCloseAttachmentSendView = ::closeAttachmentSendView,
            )
        )
    }


    init {
        coroutineScope.launch {
            matrixClient.user.getById(selectedRoomId, matrixClient.userId)
                .filterNotNull()
                .map { it.receipts[Read]?.eventId }
                .collect {
                    readEvent.value = it
                }
        }
        coroutineScope.launch {
            matrixClient.room.getAccountData<FullyReadEventContent>(selectedRoomId).filterNotNull()
                .map { it.eventId }
                .collect {
                    fullyReadEvent.value = it
                }
        }
        timelineElementViewModels =
            combine(
                timelineElements,
                matrixClient.room.getOutbox()
            ) { timelineEventsViewModels, outbox ->
                log.debug { "compute timeline elements" }
                val timelineElements = timelineEventsViewModels.map { it.key to it.viewModel } +
                        computeOutbox(outbox, timelineEventsViewModels.map { it.timelineEvent })
                log.debug { "finished compute timeline elements" }
                timelineElements
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(5.seconds), listOf())

        unreadElementFlow =
            combine(
                timelineEvents,
                fullyReadEvent,
            ) { timelineEvents, fullyReadEvent ->
                log.debug { "start compute unread marker (fullyReadEvent=$fullyReadEvent)" }
                // be AWARE: the timelineEvents pair first element might NOT be the real EventId (our messages have the transaction ID)

                val unreadElement =
                    timelineEvents.takeLastWhile { it.first().eventId != fullyReadEvent }.firstOrNull { eventFlow ->
                        val timelineEvent = eventFlow.first()
                        val isByMe = timelineEvent.event.sender == matrixClient.userId
                        val origEventContent = timelineEvent.event.content
                        timelineElementRules.canHaveUnreadMarker.any { it.isInstance(origEventContent) }
                                && timelineElementRules.areVisible.any { it.isInstance(origEventContent) }
                                && isByMe.not()
                    }?.first()?.eventId
                log.debug { "new unread marker at $unreadElement" }
                unreadElement
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

        stickyDate =
            combine(
                firstVisibleTimelineElement,
                timelineEvents,
            ) { firstVisibleTimelineEvent, timelineEvents ->
                if (firstVisibleTimelineEvent != null) {
                    val visibleTimelineEvent =
                        timelineEvents.takeLastWhileInclusive {
                            it.first().eventId.full != firstVisibleTimelineEvent
                                    && it.first().event.unsigned?.transactionId != firstVisibleTimelineEvent
                        }.firstOrNull { timelineEvent ->
                            timelineElementRules.areVisible.any { it.isInstance(timelineEvent.first().event.content) }
                        }
                    visibleTimelineEvent?.first()?.event?.originTimestamp?.let { timestamp ->
                        formatDate(
                            Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.of(timezone()))
                        )
                    }
                } else {
                    null
                }
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

        readEventsFlow =
            combine(
                timelineEvents,
                roomUsers
                    .throttleFirst(5.seconds)
                    .map { roomUsers ->
                        roomUsers.filterNot { roomUser -> roomUser.userId == matrixClient.userId }
                            .mapNotNull { roomUser -> roomUser.receipts[Read]?.eventId }
                    }
            ) { timelineEvents, eventsWithReadReceipt ->
                timelineEvents.reversed()
                    .scan(null as TimelineEvent? to false) { (previousTimelineEvent, isRead), timelineEventFlow ->
                        if (timelineEventFlow.first().event.sender == matrixClient.userId) {
                            timelineEventFlow.first() to (
                                    isRead
                                            || previousTimelineEvent?.event?.sender != null && previousTimelineEvent.event.sender != matrixClient.userId
                                            || eventsWithReadReceipt.contains(timelineEventFlow.first().eventId)
                                    )
                        } else {
                            null to true // messages after our own are interpreted as 'user has seen our message'
                        }
                    }.filter { it.first != null && it.second }.map { it.first!!.eventId }.toSet()
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), setOf())

        initTimeline()
        loadMoreBefore()
        loadMoreAfter()
        scrollToEndOnNewOutboxElement()

        markLastVisibleEventAsReadWhenItChanges()
        markFullyReadWhenRoomOrAppIsClosed()
        markFullyReadWhenFocusIsLost()

        coroutineScope.launch {
            matrixClient.user.loadMembers(selectedRoomId, wait = false)
        }
    }

    private fun initTimeline() {
        coroutineScope.launch {
            val initTimelineFrom =
                matrixClient.room.getAccountData<FullyReadEventContent>(selectedRoomId)
                    .first()?.eventId?.let { lastReadEventId ->
                        withTimeoutOrNull(10.seconds) {
                            lastReadEventId.takeIf {
                                matrixClient.room.getTimelineEvent(selectedRoomId, lastReadEventId)
                                    .first() != null // just check, that event is stored locally
                            }.also {
                                if (it == null) log.warn { "cannot use fully read event as start for timeline, because event not stored locally" }
                                else log.debug { "use fully read event as start for timeline" }
                            }
                        }
                    } ?: matrixClient.room.getById(selectedRoomId).map { it?.lastEventId }.filterNotNull().first()
                    .also { log.debug { "use last known event as start for timeline" } }
            timelineStartFrom.emit(initTimelineFrom)
            scheduleScrollTo(initTimelineFrom.full)
        }
    }

    private fun scrollToEndOnNewOutboxElement() {
        coroutineScope.launch {
            matrixClient.room.getOutbox().scan(emptySet<String>()) { transactionIdsOld, outboxNew ->
                val transactionIdsNew =
                    outboxNew.filter { it.roomId == selectedRoomId }.map { it.transactionId }.toSet()
                val diff = (transactionIdsNew - transactionIdsOld).toSet()
                if (diff.isNotEmpty()) {
                    log.debug { "submitted a new message to the outbox -> should scroll to it" }
                    scheduleScrollTo(diff.last())
                }
                transactionIdsNew
            }.collect()
        }
    }

    private fun markLastVisibleEventAsReadWhenItChanges() {
        coroutineScope.launch {
            combine(
                lastVisibleTimelineElement.filterNotNull(),
                windowIsFocused,
            ) { lastVisibleTimelineEvent, windowIsFocused ->
                Pair(lastVisibleTimelineEvent, windowIsFocused)
            }.distinctUntilChanged().throttleFirst(500.milliseconds) // we don't want to spam the server
                .collect { (lastVisibleTimelineElement, windowIsFocused) ->
                    if (windowIsFocused) {
                        log.debug { "mark the last visible element as read: $lastVisibleTimelineElement" }
                        markAsRead(lastVisibleTimelineElement)
                    }
                }
        }
    }

    private fun markFullyReadWhenRoomOrAppIsClosed() {
        lifecycle.doOnPause {
            log.debug { "timeline is paused: mark last seen message as fully read" }
            markAsFullyRead()
        }
        lifecycle.doOnDestroy {
            log.debug { "timeline is paused: mark last seen message as fully read" }
            markAsFullyRead()
        }
    }

    private fun markFullyReadWhenFocusIsLost() {
        coroutineScope.launch {
            windowIsFocused.collectLatest {
                if (it.not()) markAsFullyRead()
            }
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun computeTimelineElement(
        timelineEventFlow: Flow<TimelineEvent>,
    ): TimelineElementWrapper {
        val eventId = timelineEventFlow.first().eventId
        log.trace { "compute timeline element $eventId" }
        val existingViewModel = timelineEventsViewModelsCache[eventId]
        val viewModel = if (existingViewModel != null) existingViewModel
        else {
            val canLoadMoreBefore = timelineState.map {
                it.canLoadBefore && it.lastLoadedEventIdBefore == eventId
            }
            val canLoadMoreAfter = timelineState.map {
                it.canLoadAfter && it.lastLoadedEventIdAfter == eventId
            }
                // prevent flicker in UI, because for a short moment, this is true (while the UI loads new elements)
                .debounce(300.milliseconds)
            get<TimelineElementViewModelFactory>().newTimelineElementViewModel(
                viewModelContext = childContext("timelineElement-$eventId"),
                selectedRoomId = selectedRoomId,
                timelineEventFlow = timelineEventFlow,
                eventId = eventId,
                canLoadMoreBefore = canLoadMoreBefore,
                canLoadMoreAfter = canLoadMoreAfter,
                isDirect = isDirect,
                isReadFlow = readEventsFlow.map { readEvents -> readEvents.contains(eventId) },
                readBy = readByUsersList(eventId),
                shouldShowUnreadMarkerFlow = unreadElementFlow.map { it == eventId },
                onMessageEdited = ::onMessageEdited,
                onMessageRepliedTo = ::onMessageRepliedTo,
                onOpenModal = onOpenModal,
            ).also {
                timelineEventsViewModelsCache[eventId] = it
                // is used to make sure the viewmodel (and thus the UI representation) for outbox messages is instantly visible to avoid 'jumping' in the timeline
                // if performance is an issue, maybe investigate if this can be replaced with a smarter solution
                it.viewModel.first { viewModel -> viewModel != null }
            }
        }
        return TimelineElementWrapper(
            (timelineEventFlow.first().event.unsigned?.transactionId ?: eventId.full),
            timelineEventFlow,
            viewModel
        )
    }

    private suspend fun computeOutbox(
        outbox: List<RoomOutboxMessage<*>>, timelineEvents: List<Flow<TimelineEvent>>
    ): List<Pair<String, ITimelineElementViewModel>> = coroutineScope {
        log.debug { "compute outbox" }
        val lastTimelineEvent = timelineEvents.lastOrNull()?.first()
        if (outbox.isEmpty()) emptyList()
        else {
            val timelineEventsTransactionIds =
                timelineEvents.mapNotNull { it.first().event.unsigned?.transactionId }.toSet()
            outbox.asFlow()
                .filter { outboxMessage -> outboxMessage.roomId == selectedRoomId }
                .filterNot { timelineEventsTransactionIds.contains(it.transactionId) }
                .withIndex().map { (index, outboxMessage) ->
                    val lastDate = lastTimelineEvent?.event?.originTimestamp?.let { millis ->
                        Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.of(timezone()))
                    }
                    val today = clock.now().toLocalDateTime(TimeZone.of(timezone()))
                    val lastMessageFromAtLeastYesterday = lastDate != null && lastDate.isDifferentDay(today)
                    val showDateAbove = lastDate == null || lastMessageFromAtLeastYesterday
                    log.trace {
                        "outbox: transactionId=${outboxMessage.transactionId}, showDateAbove=$showDateAbove, lastDate=$lastDate, today=${
                            clock.now().toLocalDateTime(TimeZone.of(timezone()))
                        }"
                    }

                    val showChatBubbleEdge =
                        index == 0 && lastTimelineEvent?.event?.sender != matrixClient.userId

                    val lifecycleRegistry = LifecycleRegistry()
                    outboxLifecycles.add(lifecycleRegistry)

                    // TODO we should cache this and not create new view models on each change of the complete outbox
                    val viewModel =
                        get<OutboxElementViewModelFactory>().newOutboxElementViewModel(
                            viewModelContext = childContext(
                                // as it sometimes can happen that the outbox element is not yet destroyed and still in the
                                // outbox list, we have to make the id unique here to avoid an exception in decompose
                                "outboxTimelineElement-${outboxMessage.transactionId}-${uuid4()}",
                                lifecycle = lifecycleRegistry
                            ),
                            selectedRoomId = selectedRoomId,
                            outboxMessage = outboxMessage,
                            showDateAbove = showDateAbove,
                            showChatBubbleEdge = showChatBubbleEdge,
                            onOpenModal = onOpenModal,
                        )
                    lifecycleRegistry.start()
                    outboxMessage.transactionId to viewModel
                }.toList().also {
                    log.debug { "finished compute outbox" }
                }
        }
    }

    override fun errorDismiss() {
        error.value = null
    }

    private fun onShowAttachmentSendView(file: String) {
        sendAttachmentNavigation.launchPush(coroutineScope, SendAttachmentConfig.SendAttachmentView(file))
    }

    private fun closeAttachmentSendView() {
        sendAttachmentNavigation.launchPopWhile(coroutineScope) { it !is SendAttachmentConfig.None }
    }

    private fun onMessageEdited(eventId: EventId) {
        timelineEventsViewModelsCache.filterNot { it.key == eventId }.forEach { it.value.endEdit() }
        inputAreaViewModel.editMessage(eventId)
    }

    private fun onMessageEditFinished(eventId: EventId) {
        timelineEventsViewModelsCache[eventId]?.endEdit()
            ?: log.warn { "try to end edit of timeline event that is not present ($eventId)" }
    }

    private fun onMessageRepliedTo(event: MessageEvent<*>) {
        timelineEventsViewModelsCache.filterNot { it.key == event.id }.forEach { it.value.endReplyTo() }
        inputAreaViewModel.replyToMessage(event)
    }

    private fun onMessageReplyToFinished(event: MessageEvent<*>) {
        timelineEventsViewModelsCache[event.id]?.endReplyTo()
            ?: log.warn { "try to end reply to timeline event that is not present (${event.id})" }
    }

    override fun leaveRoom() {
        coroutineScope.launch {
            if (matrixClient.syncState.value == SyncState.ERROR) {
                error.value = i18n.timelineLeaveRoomErrorOffline()
            } else {
                matrixClient.api.rooms.leaveRoom(selectedRoomId).fold(onSuccess = {
                    onBack()
                }, onFailure = {
                    if (it is CancellationException) {
                        return@launch
                    }
                    log.error(it) { "cannot leave room $selectedRoomId" }
                    val groupOrChat =
                        if (isDirect.value) i18n.eventChangeChatGenitive()
                        else i18n.eventChangeGroupGenitive()
                    error.value = i18n.timelineLeaveRoomError(groupOrChat)
                })
            }
        }
    }

    override fun loadBefore() {
        coroutineScope.launch {
            loadingBefore.value = true
            timeline.first().loadBefore()
            loadingBefore.value = false
        }
    }

    override fun selectFile(file: String) {
        onShowAttachmentSendView(file)
    }

    override fun dragFile(file: String) {
        draggedFile.value = file
    }

    override fun dragFileExit() {
        draggedFile.value = null
    }

    private fun loadMoreBefore() {
        if (timelineViewModelConfig.autoLoadBefore) {
            coroutineScope.launch {
                timeline.collectLatest { timeline ->
                    combine(
                        timelineElementViewModels,
                        firstVisibleTimelineElement
                    ) { timelineElementViewModels, firstVisibleTimelineElement ->
                        log.trace { "loadMoreBefore (check) : ${timelineElementViewModels.map { it.first }}, firstVisible: $firstVisibleTimelineElement" }
                        val indexOfFirstVisibleTimelineElement =
                            timelineElementViewModels.indexOfFirst { it.first == firstVisibleTimelineElement }
                        if (indexOfFirstVisibleTimelineElement in 0..9) {
                            log.debug { "load more timeline events before" }
                            timeline.loadBefore()
                        }
                    }.collect()
                }
            }
        }
    }

    private fun loadMoreAfter() {
        coroutineScope.launch {
            timeline.collectLatest { timeline ->
                combine(
                    timelineElementViewModels,
                    lastVisibleTimelineElement
                ) { changedTimelineElementViewModels, changedLastVisibleTimelineElement ->
                    log.debug { "loadMoreAfter (check) : ${changedTimelineElementViewModels.map { it.first }}, lastVisible: $changedLastVisibleTimelineElement" }
                    val indexOfLastVisibleTimelineElement =
                        changedTimelineElementViewModels.indexOfFirst { it.first == changedLastVisibleTimelineElement }
                    if (indexOfLastVisibleTimelineElement >= 0 &&
                        indexOfLastVisibleTimelineElement > (changedTimelineElementViewModels.size - 10)
                    ) {
                        val lastEventIdBeforeChange =
                            matrixClient.room.getById(selectedRoomId).map { it?.lastEventId }.first()

                        // IMPORTANT: when we are at the end of the timeline, this suspends and waits for new messages
                        log.debug { "load more timeline events after" }
                        val timelineStateChange = timeline.loadAfter()
                        log.debug { "finished load more timeline events after" }

                        if (timelineStateChange.newElements.isNotEmpty()
                            && windowIsFocused.value
                            && timelineStateChange.elementsBeforeChange.endOfTimelineVisible(lastEventIdBeforeChange)
                        ) {
                            val newLastEvent = timelineStateChange.newElements.last().key
                            val lastVisibleTimelineEvent = lastVisibleTimelineElement.value?.let {
                                timelineElementViewModels.findLastWithEventId(it)
                            }
                            val currentFullyReadEvent = fullyReadEvent.value
                            log.trace { "lastVisibleTimelineEvent=$lastVisibleTimelineEvent currentFullyReadEvent=$currentFullyReadEvent newLastEvent=$newLastEvent" }
                            log.debug { "new timeline events has been added at the end of timeline -> scroll to end" }
                            scheduleScrollTo(newLastEvent)
                            if (lastVisibleTimelineEvent == currentFullyReadEvent) {
                                log.debug { "new timeline events has been added at the end of timeline -> mark as fully read" }
                                markAsRead(newLastEvent)
                                markAsFullyRead()
                            }
                        }
                    }
                }.collect()
            }
        }
    }

    private suspend fun List<TimelineElementWrapper>.endOfTimelineVisible(lastEventId: EventId?): Boolean {
        val timelineElementKeys = map { it.key }
        val lastVisibleTimelineElementKey = lastVisibleTimelineElement.value
        val lastEventKey = lastEventId?.let {
            matrixClient.room.getTimelineEvent(selectedRoomId, it).first()?.event?.unsigned?.transactionId
                ?: it.full
        }

        val lastVisibleTimelineElementIndex =
            timelineElementKeys.indexOfLast { it == lastVisibleTimelineElementKey }
        val lastEventIndex =
            timelineElementKeys.indexOfLast { it == lastEventKey }
        return (
                if (lastVisibleTimelineElementIndex >= 0 && lastEventIndex >= 0)
                    lastVisibleTimelineElementIndex >= lastEventIndex
                else false
                ).also {
                log.trace { "calculated endOfTimelineVisible=$it (lastVisibleTimelineElementIndex=$lastVisibleTimelineElementIndex, lastVisibleTimelineElementKey=$lastVisibleTimelineElementKey, lastEventIndex=$lastEventIndex, lastEventKey=$lastEventKey)" }
            }
    }

    override fun jumpToEndOfTimeline() {
        coroutineScope.launch {
            val lastEventId =
                matrixClient.room.getById(selectedRoomId).map { it?.lastEventId }.filterNotNull().first()
            log.debug { "jump to end of timeline to $lastEventId" }
            timelineStartFrom.emit(lastEventId)
            val lastEventKey =
                matrixClient.room.getTimelineEvent(selectedRoomId, lastEventId).filterNotNull().first()
                    .run { event.unsigned?.transactionId ?: eventId.full }
            scheduleScrollTo(lastEventKey)
        }
    }

    private suspend fun markAsRead(key: String) {
        val alreadyReadUntil = readEvent.value
        val requestedNextReadUntil = timelineElementViewModels.findLastWithEventId(key) ?: return
        val eventId =
            if (alreadyReadUntil != null) {
                val timelineEvents = timelineEvents.first()
                val indexOfAlreadyReadUntil = timelineEvents.indexOfFirst { it.first().eventId == alreadyReadUntil }
                val indexOfRequestedNextReadUntil =
                    timelineEvents.indexOfFirst { it.first().eventId == requestedNextReadUntil }
                log.trace { "check if requested read $requestedNextReadUntil (key=$key) really need to be marked as read (indexOfAlreadyReadUntil=$indexOfAlreadyReadUntil, indexOfRequestedNextReadUntil=$indexOfRequestedNextReadUntil)" }
                if (indexOfAlreadyReadUntil < 0 || indexOfRequestedNextReadUntil < 0) requestedNextReadUntil
                else if (indexOfAlreadyReadUntil < indexOfRequestedNextReadUntil) requestedNextReadUntil
                else return
            } else requestedNextReadUntil

        readEvent.value = eventId
        matrixClient.api.rooms.setReadMarkers(
            roomId = selectedRoomId,
            read = eventId
        ).onFailure { log.error(it) { "cannot set read marker for event $eventId" } }
            .onSuccess { log.debug { "successfully set read marker for message: $eventId" } }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun markAsFullyRead() {
        // we have to execute this in the GlobalScope, since otherwise the view model would be cleaned up and with
        // it the scope where this code is executed
        // TODO alternative: we could put this in some sort of global worker (in Trixnity?) with database for offline scenarios (this worker could also handle redactions and more)
        try {
            outerScope.launch {
                withTimeout(5.seconds) {
                    val readUntil = readEvent.value
                    val currentFullyReadMarker = fullyReadEvent.value
                    if (readUntil != null && readUntil != currentFullyReadMarker) {
                        log.debug { "mark last seen message as fully read (readUntil=$readUntil currentFullyReadMarker=$currentFullyReadMarker)" }
                        fullyReadEvent.value = readUntil
                        matrixClient.api.rooms.setReadMarkers(selectedRoomId, fullyRead = readUntil)
                            .onFailure { log.error(it) { "cannot set message as fully read: $readUntil" } }
                            .onSuccess { log.debug { "set message as fully read: $readUntil" } }
                    } else {
                        if (readUntil == null) log.warn { "cannot mark message as read, since readUntil == null" }
                    }
                }
            }
        } catch (exc: CancellationException) {
            log.debug { "mark as fully read has been cancelled before completing" }
        }
    }

    private suspend fun readByUsersList(eventId: EventId): Flow<List<String>> {
        return roomUsers.map { roomUsers ->
            val messagesReadBy = mutableMapOf<EventId, List<String>>()
            roomUsers
                .filterNot { roomUser -> roomUser.userId == matrixClient.userId }
                .forEach { roomUser ->
                    roomUser.receipts[Read]?.eventId?.let { lastReadMessage ->
                        messagesReadBy[lastReadMessage] =
                            messagesReadBy.getOrElse(lastReadMessage) { emptyList() }.plus(roomUser.name)
                    }
                }

            val collectReadByUsers = collectReadByUsers(messagesReadBy, roomUsers, eventId)
            log.debug { "collected read by users for $eventId: $collectReadByUsers" }
            collectReadByUsers
        }
    }

    private suspend fun collectReadByUsers(
        messagesReadBy: Map<EventId, List<String>>,
        roomUsers: List<RoomUser>,
        eventId: EventId,
    ): List<String> {
        return matrixClient.room.getById(selectedRoomId).firstOrNull()?.lastEventId?.let { lastTimelineEvent ->
            matrixClient.room.getTimelineEvents(selectedRoomId, lastTimelineEvent, BACKWARDS)
                .takeWhileInclusive { it.first().eventId != eventId } // inclusive the current event
                .take(100) // no more than 100 events
                .scan(listOf<String>()) { readBy, currentEvent ->
                    readBy + (currentEvent.first().eventId.let { eventId -> messagesReadBy[eventId] }
                        ?: emptyList())
                }.takeWhileInclusive { readBy ->
                    readBy.size <= 10 && readBy.size < roomUsers.size
                }.lastOrNull()?.take(11)?.sorted() ?: emptyList()
        } ?: emptyList()
    }

    private fun onVerifyUser() {
        coroutineScope.launch {
            log.debug { "try to create new user verification" }
            val isDirectRoom = matrixClient.room.getById(selectedRoomId).first()?.isDirect ?: false
            log.debug { "is direct room: $isDirectRoom" }
            directRoom.getUser(matrixClient, selectedRoomId).first()?.let { otherUserId ->
                log.debug { "create new user verification with user $otherUserId" }
                matrixClient.verification.createUserVerificationRequest(otherUserId)
            }
        }
    }

    private fun scheduleScrollTo(key: String) {
        coroutineScope.launch {
            val result = withTimeoutOrNull(1.seconds) {
                timelineElementViewModels.first { vms -> vms.any { it.first == key } }
                log.debug { "scheduled scroll to $key" }
                scrollTo.emit(key)
            }
            if (result == null) log.warn { "could not scroll to $key, because view model does not exist" }
        }
    }

    private suspend fun StateFlow<List<Pair<String, ITimelineElementViewModel>>>.findLastWithEventId(key: String) =
        withTimeoutOrNull(1.seconds) {
            first { vms -> vms.any { it.first == key } }
                .takeWhileInclusive { it.first != key }
                .reversed()
                .firstNotNullOfOrNull { it.second.eventId }
        }
}

class PreviewTimelineViewModel : TimelineViewModel {
    override val timelineElementViewModels: MutableStateFlow<List<Pair<String, ITimelineElementViewModel>>> =
        MutableStateFlow(
            listOf(
                "\$1:localhost" to PreviewTimelineElementViewModel1(),
                "\$2:localhost" to PreviewTimelineElementViewModel2(),
            )
        )
    override val windowIsFocused: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val lastVisibleTimelineElement: MutableStateFlow<String?> = MutableStateFlow(null)
    override val firstVisibleTimelineElement: MutableStateFlow<String?> = MutableStateFlow(null)
    override val stickyDate: StateFlow<String?> = MutableStateFlow(null)
    override val scrollTo: Flow<String> = MutableSharedFlow()
    override val selectedMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    override val selectedMessageActions: MutableStateFlow<List<Pair<ContextMenuAction, () -> Unit>>> =
        MutableStateFlow(emptyList())
    override val isDirect: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val roomHeaderViewModel: RoomHeaderViewModel = PreviewRoomHeaderViewModel()
    override val inputAreaViewModel: InputAreaViewModel = PreviewInputViewModel()
    override val sendAttachmentStack: Value<ChildStack<SendAttachmentConfig, SendAttachmentWrapper>> = MutableValue(
        ChildStack(
            configuration = SendAttachmentConfig.None,
            instance = SendAttachmentWrapper.None,
        )
    )
    override val loadingBefore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val draggedFile: MutableStateFlow<String?> = MutableStateFlow(null)

    init {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            delay(3.seconds)
            timelineElementViewModels.value = listOf(
                "\$1:localhost" to PreviewTimelineElementViewModel1(),
                "\$2:localhost" to PreviewTimelineElementViewModel2(),
                "\$3:localhost" to PreviewTimelineElementViewModel1(),
            )
        }
    }

    override fun errorDismiss() {
    }

    override fun leaveRoom() {
    }

    override fun jumpToEndOfTimeline() {
    }

    override fun loadBefore() {
    }

    override fun selectFile(file: String) {
    }

    override fun dragFile(file: String) {
    }

    override fun dragFileExit() {
    }
}