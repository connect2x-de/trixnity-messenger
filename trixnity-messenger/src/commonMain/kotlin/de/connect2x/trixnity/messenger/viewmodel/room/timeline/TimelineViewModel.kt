package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnPause
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.DragAndDropHandler
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.util.launchPopWhile
import de.connect2x.trixnity.messenger.util.launchPush
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel.Config
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel.Wrapper
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.PreviewTimelineElementViewModel1
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.PreviewTimelineElementViewModel2
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReportMessageRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReportMessageRouterImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementRules
import de.connect2x.trixnity.messenger.viewmodel.util.DirectRoom
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.isDifferentDay
import de.connect2x.trixnity.messenger.viewmodel.util.takeLastWhileInclusive
import de.connect2x.trixnity.messenger.viewmodel.util.takeWhileInclusive
import de.connect2x.trixnity.messenger.viewmodel.util.throttleFirst
import de.connect2x.trixnity.messenger.viewmodel.util.timezone
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.flattenNotNull
import net.folivo.trixnity.client.flattenValues
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.Timeline
import net.folivo.trixnity.client.room.getAccountData
import net.folivo.trixnity.client.store.RoomOutboxMessage
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents.Direction.BACKWARDS
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.FullyReadEventContent
import net.folivo.trixnity.core.model.events.m.ReceiptType.Read
import org.koin.core.component.get
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger {}

interface TimelineViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        isBackButtonVisible: MutableStateFlow<Boolean>,
        onShowSettings: () -> Unit,
        onBack: () -> Unit,
        onOpenModal: OpenModalCallback,
        onOpenMention: OpenMentionCallback,
    ): TimelineViewModel {
        return TimelineViewModelImpl(
            viewModelContext,
            selectedRoomId,
            isBackButtonVisible,
            onShowSettings,
            onBack,
            onOpenModal,
            onOpenMention
        )
    }

    companion object : TimelineViewModelFactory
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
    val timelineElementHolderViewModels: StateFlow<List<BaseTimelineElementHolderViewModel>>

    /**
     * Has to be set by the UI.
     */
    val windowIsFocused: MutableStateFlow<Boolean>

    /**
     * Has to be set by the UI. String is the key from [timelineElementHolderViewModels].
     */
    val lastVisibleTimelineElement: MutableStateFlow<String?>

    /**
     * Has to be set by the UI. String is the key from [timelineElementHolderViewModels].
     */
    val firstVisibleTimelineElement: MutableStateFlow<String?>

    /**
     * Emits a unique String each time the view should scroll to the given key. String is the key from [timelineElementHolderViewModels].
     */
    val scrollTo: Flow<String>
    val stickyDate: StateFlow<String?>
    val isDirect: StateFlow<Boolean>
    val error: StateFlow<String?>
    val roomHeaderViewModel: RoomHeaderViewModel
    val inputAreaViewModel: InputAreaViewModel
    val sendAttachmentStack: Value<ChildStack<Config, Wrapper>>
    val reportMessageStack: Value<ChildStack<ReportMessageRouter.Config, ReportMessageRouter.Wrapper>>

    /**
     * Only for DnD on desktop: the absolute path of a dragged file.
     */
    val draggedFile: StateFlow<FileDescriptor?>

    fun errorDismiss()
    fun leaveRoom()

    /**
     * Suspends until the last event of the room is present in the timeline.
     */
    fun jumpToEndOfTimeline()

    val loadingBefore: StateFlow<Boolean>
    fun loadBefore()

    sealed class Wrapper {
        data object None : Wrapper()
        class View(val viewModel: SendAttachmentViewModel) : Wrapper()
    }

    sealed class Config {
        data object None : Config()

        data class SendAttachmentView(val file: FileDescriptor) : Config()
    }
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class TimelineViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val isBackButtonVisible: MutableStateFlow<Boolean>,
    private val onShowSettings: () -> Unit,
    private val onBack: () -> Unit,
    private val onOpenModal: OpenModalCallback,
    private val onOpenMention: OpenMentionCallback,
) : MatrixClientViewModelContext by viewModelContext, TimelineViewModel {

    init {
        log.debug { "::: init timelineViewModel: $viewModelContext" }
    }

    data class TimelineElementWrapper(
        val key: String,
        val timelineEvent: Flow<TimelineEvent>,
        val viewModel: TimelineElementHolderViewModel,
    )

    private val config = get<MatrixMessengerConfiguration>()
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
        timelineElements.map { it.map { it.timelineEvent } }
            .stateIn(coroutineScope, SharingStarted.Eagerly, listOf())
    private val readEventsFlow: StateFlow<Set<EventId>>
    private val unreadElementFlow: StateFlow<EventId?>

    private val readEvent = MutableStateFlow<EventId?>(null)
    private val fullyReadEvent = MutableStateFlow<EventId?>(null)

    override val timelineElementHolderViewModels: StateFlow<List<BaseTimelineElementHolderViewModel>>

    override val stickyDate: StateFlow<String?>
    override val scrollTo: MutableSharedFlow<String> = MutableSharedFlow()

    override val windowIsFocused: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val firstVisibleTimelineElement: MutableStateFlow<String?> = MutableStateFlow(null)
    override val lastVisibleTimelineElement: MutableStateFlow<String?> = MutableStateFlow(null)

    override val isDirect: StateFlow<Boolean> =
        matrixClient.room.getById(selectedRoomId).map { it?.isDirect == true }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

    override val loadingBefore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val draggedFile: MutableStateFlow<FileDescriptor?> = MutableStateFlow(null)

    private val outboxElementHolderViewModelCache =
        mutableMapOf<String, OutboxElementHolderViewModel>()

    private val clock = get<Clock>()
    private val directRoom = get<DirectRoom>()
    private val timelineElementRules = get<TimelineElementRules>()
    private val messengerSettings = get<MatrixMessengerSettingsHolder>()

    private val roomUsers =
        matrixClient.user.getAll(selectedRoomId)
            .flattenNotNull()
            .filterNotNull()
            .shareIn(coroutineScope, SharingStarted.WhileSubscribed(), replay = 1)

    private val roomUsersReceipts =
        matrixClient.user.getAllReceipts(selectedRoomId)
            .flattenNotNull()
            .filterNotNull()
            .shareIn(coroutineScope, SharingStarted.WhileSubscribed(), replay = 1)

    override val roomHeaderViewModel: RoomHeaderViewModel =
        get<RoomHeaderViewModelFactory>().create(
            viewModelContext = childContext("roomHeaderViewModel"),
            selectedRoomId = selectedRoomId,
            isBackButtonVisible = isBackButtonVisible,
            onBack = onBack,
            onVerifyUser = ::onVerifyUser,
            onShowRoomSettings = onShowSettings,
        )

    override val inputAreaViewModel: InputAreaViewModel =
        get<InputAreaViewModelFactory>().create(
            viewModelContext = childContext("inputAreaViewModel"),
            selectedRoomId = selectedRoomId,
            onMessageEditFinished = ::onMessageEditFinished,
            onMessageReplyToFinished = ::onMessageReplyToFinished,
            onShowAttachmentSendView = ::onShowAttachmentSendView,
        )

    private val reportMessageRouter: ReportMessageRouter = ReportMessageRouterImpl(
        viewModelContext = viewModelContext,
        roomId = selectedRoomId,
        onShowReportMessageDialog = ::showReportMessageDialog,
        onReportMessageDialogDismiss = ::onReportMessageDialogDismiss
    )

    internal fun onReportMessageDialogDismiss(eventId: EventId) = coroutineScope.launch {
        log.trace { "Closing report popup dialog: $eventId" }
        reportMessageRouter.closeReportMessage()
    }

    internal fun showReportMessageDialog(eventId: EventId) = coroutineScope.launch {
        reportMessageRouter.showReportMessage(eventId)
    }

    override val reportMessageStack = reportMessageRouter.stack


    private val sendAttachmentNavigation = StackNavigation<Config>()
    override val sendAttachmentStack: Value<ChildStack<Config, Wrapper>> = childStack(
        source = sendAttachmentNavigation,
        serializer = null,
        initialConfiguration = Config.None,
        handleBackButton = true,
        childFactory = ::createChild,
        key = "sendAttachmentRouter",
    )

    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ): Wrapper = when (config) {
        is Config.None -> Wrapper.None
        is Config.SendAttachmentView -> Wrapper.View(
            get<SendAttachmentViewModelFactory>().create(
                viewModelContext = childContext(componentContext),
                file = config.file,
                selectedRoomId = selectedRoomId,
                onCloseAttachmentSendView = ::closeAttachmentSendView,
            )
        )
    }

    init {
        coroutineScope.launch {
            matrixClient.user.getReceiptsById(selectedRoomId, matrixClient.userId)
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
        timelineElementHolderViewModels =
            combine(
                timelineElements,
                matrixClient.room.getOutbox()
            ) { timelineEventsViewModels, outbox ->
                log.debug { "compute timeline elements" }
                val timelineElements = timelineEventsViewModels.map { it.viewModel } +
                        computeOutbox(outbox, timelineEventsViewModels.map { it.timelineEvent })
                log.debug { "finished compute timeline elements" }
                timelineElements
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(10.seconds), listOf())

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
                            Instant.fromEpochMilliseconds(timestamp)
                                .toLocalDateTime(TimeZone.of(timezone()))
                        )
                    }
                } else {
                    null
                }
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

        readEventsFlow =
            combine(
                timelineEvents,
                roomUsersReceipts
                    .throttleFirst(5.seconds)
                    .map { roomUserReceipts ->
                        roomUserReceipts.filterNot { (userId, _) -> userId == matrixClient.userId }
                            .mapNotNull { (_, receipts) -> receipts.receipts[Read]?.eventId }
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

        // TODO we only support one file at the moment, but this should change in the future
        val dragAndDropHandler = getOrNull<DragAndDropHandler>()
        if (dragAndDropHandler != null) {
            coroutineScope.launch {
                dragAndDropHandler.onDrop.collect { files ->
                    files.firstOrNull()?.also { onShowAttachmentSendView(it) }
                }
            }
            coroutineScope.launch {
                dragAndDropHandler.onDrag.collect { files ->
                    files.firstOrNull()?.also { draggedFile.value = it }
                }
            }
            coroutineScope.launch {
                dragAndDropHandler.onDragExit.collect {
                    draggedFile.value = null
                }
            }
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
                    } ?: matrixClient.room.getById(selectedRoomId).map { it?.lastEventId }
                    .filterNotNull().first()
                    .also { log.debug { "use last known event as start for timeline" } }

            val foundTimelineEvent =
                withTimeoutOrNull(10.seconds) {
                    matrixClient.room.getTimelineEvent(selectedRoomId, initTimelineFrom)
                        .first() != null // just check, that event is stored locally
                }
            if (foundTimelineEvent == null) {
                log.error { "could not load start point of timeline" }
            }
            timelineStartFrom.emit(initTimelineFrom)
            scheduleScrollTo(initTimelineFrom.full)
        }
    }

    private fun scrollToEndOnNewOutboxElement() {
        coroutineScope.launch {
            matrixClient.room.getOutbox().flattenValues()
                .scan(emptySet<String>()) { transactionIdsOld, outboxNew ->
                    val transactionIdsNew =
                        outboxNew.filter { it.roomId == selectedRoomId }.map { it.transactionId }
                            .toSet()
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
            }.distinctUntilChanged()
                .throttleFirst(500.milliseconds) // we don't want to spam the server
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
        val key = timelineEventFlow.first().event.unsigned?.transactionId ?: eventId.full
        log.trace { "compute timeline element $eventId" }
        val canLoadMoreBefore = timelineState.map { it.canLoadBefore && it.lastLoadedEventIdBefore == eventId }
        val canLoadMoreAfter = timelineState.map { it.canLoadAfter && it.lastLoadedEventIdAfter == eventId }
            .debounce(300.milliseconds) // prevent flicker in UI, because for a short moment, this is true (while the UI loads new elements)
        val viewModel = get<TimelineElementHolderViewModelFactory>().create(
            viewModelContext = childContext("timelineElement-$eventId"),
            key = key,
            timelineEventFlow = timelineEventFlow,
            selectedRoomId = selectedRoomId,
            eventId = eventId,
            canLoadMoreBefore = canLoadMoreBefore,
            canLoadMoreAfter = canLoadMoreAfter,
            isDirect = isDirect,
            isReadFlow = readEventsFlow.map { readEvents -> readEvents.contains(eventId) },
            readBy = readByUsersList(eventId),
            shouldShowUnreadMarkerFlow = unreadElementFlow.map { it == eventId },
            onMessageEdited = ::onMessageEdited,
            onMessageRepliedTo = ::onMessageRepliedTo,
            onMessageReportTo = ::onShowReportMessageModal,
            onOpenModal = onOpenModal,
            onOpenMention = onOpenMention,
        ).also {
            // is used to make sure the viewmodel (and thus the UI representation) for outbox messages is instantly visible to avoid 'jumping' in the timeline
            // is needed in the UI for initial position of read marker
            it.timelineElementViewModel.first { viewModel -> viewModel != null }
        }
        return TimelineElementWrapper(
            key,
            timelineEventFlow,
            viewModel
        )
    }

    private suspend fun computeOutbox(
        outbox: Map<String, Flow<RoomOutboxMessage<*>?>>,
        timelineEventList: List<Flow<TimelineEvent>>,
    ): List<OutboxElementHolderViewModel> = coroutineScope {
        log.debug { "compute outbox" }
        if (outbox.isEmpty()) emptyList()
        else {
            val timelineEventsTransactionIds =
                timelineEventList.mapNotNull { it.first().event.unsigned?.transactionId }.toSet()
            outbox.entries.asFlow()
                .filter { (_, outboxMessage) -> outboxMessage.first()?.roomId == selectedRoomId }
                .filterNot { (transactionId, _) ->
                    timelineEventsTransactionIds.contains(
                        transactionId
                    )
                }
                .map { (transactionId, outboxMessage) ->
                    val existingViewModel = outboxElementHolderViewModelCache[transactionId]
                    if (existingViewModel == null) {
                        val showDateAboveFlow =
                            timelineEvents.flatMapLatest { it.lastOrNull() ?: flowOf(null) }
                                .distinctUntilChanged()
                                .map { lastTimelineEvent ->
                                    val lastDate =
                                        lastTimelineEvent?.event?.originTimestamp?.let { millis ->
                                            Instant.fromEpochMilliseconds(millis)
                                                .toLocalDateTime(TimeZone.of(timezone()))
                                        }
                                    val today = clock.now().toLocalDateTime(TimeZone.of(timezone()))
                                    val lastMessageFromAtLeastYesterday =
                                        lastDate != null && lastDate.isDifferentDay(today)
                                    lastDate == null || lastMessageFromAtLeastYesterday
                                }.distinctUntilChanged()
                        val showChatBubbleEdgeFlow =
                            combine(
                                matrixClient.room.getOutbox()
                                    .map { outbox ->
                                        outbox.filter { it.value.first()?.roomId == selectedRoomId }.keys
                                            .indexOf(transactionId)
                                    }.distinctUntilChanged(),
                                timelineEvents.map {
                                    it.lastOrNull()?.first()?.sender == matrixClient.userId
                                }
                                    .distinctUntilChanged()
                            ) { index, lastEventFromUs ->
                                log.trace { "compute outbox showChatBubbleEdge (index=$index, lastEventFromUs=$lastEventFromUs)" }
                                index == 0 && !lastEventFromUs
                            }.distinctUntilChanged()
                        get<OutboxElementHolderViewModelFactory>().create(
                            viewModelContext = childContext("outboxTimelineElement-${transactionId}"),
                            key = transactionId,
                            outboxMessageFlow = outboxMessage,
                            selectedRoomId = selectedRoomId,
                            transactionId = transactionId,
                            showDateAboveFlow = showDateAboveFlow,
                            showChatBubbleEdgeFlow = showChatBubbleEdgeFlow,
                            onOpenModal = onOpenModal,
                            onOpenMention = onOpenMention,
                        ).also {
                            outboxElementHolderViewModelCache[transactionId] = it
                            // is used to make sure the viewmodel (and thus the UI representation) for outbox messages is instantly visible to avoid 'jumping' in the timeline
                            // is needed in the UI for initial position of read marker
                            it.timelineElementViewModel.first { viewModel -> viewModel != null }
                        }
                    } else existingViewModel
                }.toList().also {
                    log.debug { "finished compute outbox" }
                }
        }
    }

    override fun errorDismiss() {
        error.value = null
    }

    private fun onShowAttachmentSendView(file: FileDescriptor) {
        sendAttachmentNavigation.launchPush(coroutineScope, Config.SendAttachmentView(file))
    }

    private fun onShowReportMessageModal(eventId: EventId) = coroutineScope.launch {
        log.debug { "report to message $eventId" }
        reportMessageRouter.showReportMessage(eventId)
    }

    private fun closeAttachmentSendView() {
        sendAttachmentNavigation.launchPopWhile(coroutineScope) { it !is Config.None }
        jumpToEndOfTimeline()
    }

    private fun onMessageEdited(eventId: EventId) {
        timelineElements.value.filterNot { it.key == eventId.full }
            .forEach { it.viewModel.endEdit() }
        inputAreaViewModel.editMessage(eventId)
    }

    private fun onMessageEditFinished(eventId: EventId) {
        timelineElements.value.firstOrNull { it.key == eventId.full }?.viewModel?.endEdit()
            ?: log.warn { "try to end edit of timeline event that is not present ($eventId)" }
    }

    private fun onMessageRepliedTo(eventId: EventId) {
        timelineElements.value.filterNot { it.key == eventId.full }
            .forEach { it.viewModel.endReplyTo() }
        inputAreaViewModel.replyToMessage(eventId)
    }

    private fun onMessageReplyToFinished(eventId: EventId) {
        timelineElements.value.firstOrNull { it.key == eventId.full }?.viewModel?.endEdit()
            ?: log.warn { "try to end reply to timeline event that is not present (${eventId})" }
    }

    override fun leaveRoom() {
        coroutineScope.launch {
            if (matrixClient.syncState.value == SyncState.ERROR) {
                error.value = i18n.timelineLeaveRoomErrorOffline()
            } else {
                matrixClient.api.room.leaveRoom(selectedRoomId).fold(onSuccess = {
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

    private fun loadMoreBefore() {
        if (config.timelineAutoLoadBefore) {
            coroutineScope.launch {
                timeline.collectLatest { timeline ->
                    combine(
                        timelineElementHolderViewModels,
                        firstVisibleTimelineElement
                    ) { timelineElementViewModels, firstVisibleTimelineElement ->
                        log.trace { "loadMoreBefore (check) : ${timelineElementViewModels.map { it.key }}, firstVisible: $firstVisibleTimelineElement" }
                        val indexOfFirstVisibleTimelineElement =
                            timelineElementViewModels.indexOfFirst { it.key == firstVisibleTimelineElement }
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
                    timelineElementHolderViewModels,
                    lastVisibleTimelineElement
                ) { changedTimelineElementViewModels, changedLastVisibleTimelineElement ->
                    log.debug { "loadMoreAfter (check) : ${changedTimelineElementViewModels.map { it.key }}, lastVisible: $changedLastVisibleTimelineElement" }
                    val indexOfLastVisibleTimelineElement =
                        changedTimelineElementViewModels.indexOfFirst { it.key == changedLastVisibleTimelineElement }
                    if (indexOfLastVisibleTimelineElement >= 0 &&
                        indexOfLastVisibleTimelineElement > (changedTimelineElementViewModels.size - 10)
                    ) {
                        val lastEventIdBeforeChange =
                            matrixClient.room.getById(selectedRoomId).map { it?.lastEventId }
                                .first()

                        // IMPORTANT: when we are at the end of the timeline, this suspends and waits for new messages
                        log.debug { "load more timeline events after" }
                        val timelineStateChange = timeline.loadAfter()
                        log.debug { "finished load more timeline events after" }

                        if (timelineStateChange.newElements.isNotEmpty()
                            && windowIsFocused.value
                            && timelineStateChange.elementsBeforeChange.endOfTimelineVisible(
                                lastEventIdBeforeChange
                            )
                        ) {
                            val newLastEvent = timelineStateChange.newElements.last().key
                            val lastVisibleTimelineEvent = lastVisibleTimelineElement.value?.let {
                                timelineElementHolderViewModels.findLastWithEventId(it)
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
            matrixClient.room.getTimelineEvent(selectedRoomId, it)
                .first()?.event?.unsigned?.transactionId
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
                matrixClient.room.getById(selectedRoomId).map { it?.lastEventId }.filterNotNull()
                    .first()
            log.debug { "jump to end of timeline to $lastEventId" }
            timelineStartFrom.emit(lastEventId)
            val lastEventKey =
                matrixClient.room.getTimelineEvent(selectedRoomId, lastEventId).filterNotNull()
                    .first()
                    .run { event.unsigned?.transactionId ?: eventId.full }
            scheduleScrollTo(lastEventKey)
        }
    }

    private suspend fun markAsRead(key: String) {
        val alreadyReadUntil = readEvent.value
        val requestedNextReadUntil =
            timelineElementHolderViewModels.findLastWithEventId(key) ?: return
        val eventId =
            if (alreadyReadUntil != null) {
                val timelineEvents = timelineEvents.first()
                val indexOfAlreadyReadUntil =
                    timelineEvents.indexOfFirst { it.first().eventId == alreadyReadUntil }
                val indexOfRequestedNextReadUntil =
                    timelineEvents.indexOfFirst { it.first().eventId == requestedNextReadUntil }
                log.trace { "check if requested read $requestedNextReadUntil (key=$key) really need to be marked as read (indexOfAlreadyReadUntil=$indexOfAlreadyReadUntil, indexOfRequestedNextReadUntil=$indexOfRequestedNextReadUntil)" }
                if (indexOfAlreadyReadUntil < 0 || indexOfRequestedNextReadUntil < 0) requestedNextReadUntil
                else if (indexOfAlreadyReadUntil < indexOfRequestedNextReadUntil) requestedNextReadUntil
                else return
            } else requestedNextReadUntil

        readEvent.value = eventId
        val readMarkerIsPublic = messengerSettings[userId].first()?.base?.readMarkerIsPublic == true
        matrixClient.api.room.setReadMarkers(
            roomId = selectedRoomId,
            read = if (readMarkerIsPublic) eventId else null,
            privateRead = if (readMarkerIsPublic) null else eventId,
        ).onFailure { log.error(it) { "cannot set read marker for event $eventId" } }
            .onSuccess { log.debug { "successfully set read marker for message: $eventId" } }
    }

    private fun markAsFullyRead() {
        // we have to execute this in the outerScope, since otherwise the view model would be cleaned up and with
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
                        matrixClient.api.room.setReadMarkers(selectedRoomId, fullyRead = readUntil)
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
        return roomUsersReceipts.map { roomUsersReceipts ->
            val messagesReadBy = mutableMapOf<EventId, List<String>>()
            roomUsersReceipts
                .filterNot { (userId, _) -> userId == matrixClient.userId }
                .forEach { (userId, receipts) ->
                    receipts.receipts[Read]?.eventId?.also { lastReadMessage ->
                        roomUsers.first()[userId]?.name?.also { name ->
                            messagesReadBy[lastReadMessage] =
                                messagesReadBy.getOrElse(lastReadMessage) { emptyList() }.plus(name)
                        }
                    }
                }

            val collectReadByUsers =
                collectReadByUsers(messagesReadBy, roomUsers.first().size, eventId)
            log.debug { "collected read by users for $eventId: $collectReadByUsers" }
            collectReadByUsers
        }
    }

    private suspend fun collectReadByUsers(
        messagesReadBy: Map<EventId, List<String>>,
        roomUsersSize: Int,
        eventId: EventId,
    ): List<String> {
        return matrixClient.room.getById(selectedRoomId)
            .firstOrNull()?.lastEventId?.let { lastTimelineEvent ->
                matrixClient.room.getTimelineEvents(selectedRoomId, lastTimelineEvent, BACKWARDS)
                    .takeWhileInclusive { it.first().eventId != eventId } // inclusive the current event
                    .take(100) // no more than 100 events
                    .scan(listOf<String>()) { readBy, currentEvent ->
                        readBy + (currentEvent.first().eventId.let { eventId -> messagesReadBy[eventId] }
                            ?: emptyList())
                    }.takeWhileInclusive { readBy ->
                        readBy.size <= 10 && readBy.size < roomUsersSize
                    }.lastOrNull()?.take(11)?.sorted() ?: emptyList()
            } ?: emptyList()
    }

    private fun onVerifyUser() {
        coroutineScope.launch {
            log.debug { "try to create new user verification" }
            val isDirectRoom = matrixClient.room.getById(selectedRoomId).first()?.isDirect ?: false
            log.debug { "is direct room: $isDirectRoom" }
            directRoom.getUsers(matrixClient, selectedRoomId).first().firstOrNull()
                ?.let { otherUserId ->
                    log.debug { "create new user verification with user $otherUserId" }
                    matrixClient.verification.createUserVerificationRequest(otherUserId)
                }
        }
    }

    private fun scheduleScrollTo(key: String) {
        coroutineScope.launch {
            val result = withTimeoutOrNull(1.seconds) {
                timelineElementHolderViewModels.first { vms -> vms.any { it.key == key } }
                log.debug { "scheduled scroll to $key" }
                scrollTo.emit(key)
            }
            if (result == null) log.warn { "could not scroll to $key, because view model does not exist" }
        }
    }

    private suspend fun StateFlow<List<BaseTimelineElementHolderViewModel>>.findLastWithEventId(key: String) =
        withTimeoutOrNull(1.seconds) {
            first { vms -> vms.any { it.key == key } }
                .takeWhileInclusive { it.key != key }
                .reversed()
                .firstNotNullOfOrNull { if (it is TimelineElementHolderViewModel) it.eventId else null }
        }
}

class PreviewTimelineViewModel : TimelineViewModel {
    override val timelineElementHolderViewModels: MutableStateFlow<List<BaseTimelineElementHolderViewModel>> =
        MutableStateFlow(
            listOf(
                PreviewTimelineElementViewModel1(),
                PreviewTimelineElementViewModel2(),
            )
        )
    override val windowIsFocused: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val lastVisibleTimelineElement: MutableStateFlow<String?> = MutableStateFlow(null)
    override val firstVisibleTimelineElement: MutableStateFlow<String?> = MutableStateFlow(null)
    override val stickyDate: StateFlow<String?> = MutableStateFlow(null)
    override val scrollTo: Flow<String> = MutableSharedFlow()
    override val isDirect: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val roomHeaderViewModel: RoomHeaderViewModel = PreviewRoomHeaderViewModel()
    override val inputAreaViewModel: InputAreaViewModel = PreviewInputViewModel()
    override val sendAttachmentStack: Value<ChildStack<Config, Wrapper>> = MutableValue(
        ChildStack(
            configuration = Config.None,
            instance = Wrapper.None,
        )
    )

    override val reportMessageStack: Value<ChildStack<ReportMessageRouter.Config, ReportMessageRouter.Wrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = ReportMessageRouter.Config.None,
                    instance = ReportMessageRouter.Wrapper.None
                )
            )
        )
    override val loadingBefore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val draggedFile: MutableStateFlow<FileDescriptor?> = MutableStateFlow(null)

    init {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            delay(3.seconds)
            timelineElementHolderViewModels.value = listOf(
                PreviewTimelineElementViewModel1(),
                PreviewTimelineElementViewModel2(),
                PreviewTimelineElementViewModel1(),
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
}
