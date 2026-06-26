package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import de.connect2x.lognity.api.logger.error
import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.GetTimelineEventsConfig
import de.connect2x.trixnity.client.room.Timeline
import de.connect2x.trixnity.client.room.getAccountData
import de.connect2x.trixnity.client.store.RoomOutboxMessage
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.store.eventId
import de.connect2x.trixnity.client.store.isFirst
import de.connect2x.trixnity.client.store.isLast
import de.connect2x.trixnity.client.store.originTimestamp
import de.connect2x.trixnity.client.store.roomId
import de.connect2x.trixnity.client.store.sender
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.client.verification
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.clientserverapi.model.room.GetEvents
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.FullyReadEventContent
import de.connect2x.trixnity.core.model.events.m.MarkedUnreadEventContent
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.DragAndDropHandler
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.LeaveRoom
import de.connect2x.trixnity.messenger.util.getOrNull
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
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactorySelector
import de.connect2x.trixnity.messenger.viewmodel.util.asReversedFlow
import de.connect2x.trixnity.messenger.viewmodel.util.asReversedIndexedFlow
import de.connect2x.trixnity.messenger.viewmodel.util.byEventId
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import de.connect2x.trixnity.messenger.viewmodel.util.throttleFirst
import de.connect2x.trixnity.utils.concurrentMutableMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.get

interface TimelineViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        roomId: RoomId,
        onBack: () -> Unit,
        onOpenRoomSettings: () -> Unit,
        onOpenUserProfile: (UserId) -> Unit,
        onOpenMention: OpenMentionCallback,
        onOpenMetadata: (eventId: EventId) -> Unit,
    ): TimelineViewModel =
        TimelineViewModelImpl(
            viewModelContext = viewModelContext,
            roomId = roomId,
            onBack = onBack,
            onOpenSettings = onOpenRoomSettings,
            onOpenUserProfile = onOpenUserProfile,
            onOpenMention = onOpenMention,
            onOpenMetadata = onOpenMetadata,
        )

    companion object : TimelineViewModelFactory
}

/** Handles the timeline of a room and provides a list of view models that represent all elements of the timeline. */
interface TimelineViewModel {
    val elements: StateFlow<List<BaseTimelineElementHolderViewModel>>
    val canLoadBefore: StateFlow<Boolean?>
    val canLoadAfter: StateFlow<Boolean?>

    /** Emits a unique String each time the view should scroll to the given key. String is the key from [elements]. */
    val scrollTo: StateFlow<String?>
    val isDirect: StateFlow<Boolean>
    val error: StateFlow<String?>

    val roomHeaderViewModel: RoomHeaderViewModel
    val inputAreaViewModel: InputAreaViewModel

    val sendAttachmentStack: Value<ChildStack<Config, Wrapper>>
    val reportMessageStack: Value<ChildStack<ReportMessageRouter.Config, ReportMessageRouter.Wrapper>>

    /** Only for DnD on desktop: the absolute path of a dragged file. */
    val draggedFile: StateFlow<FileDescriptor?>

    val unreadCount: StateFlow<String?>

    fun setViewState(viewState: ViewState)

    fun errorDismiss()

    fun leaveRoom()

    fun jumpToEndOfTimeline()

    /** Should be used, when [MatrixMessengerConfiguration.timelineViewStateSupported] is disabled. */
    suspend fun loadBefore()

    /** Should be used, when [MatrixMessengerConfiguration.timelineViewStateSupported] is disabled. */
    suspend fun loadAfter()

    /** Should be used, when [MatrixMessengerConfiguration.timelineViewStateSupported] is disabled. */
    suspend fun dropBefore(key: String)

    /** Should be used, when [MatrixMessengerConfiguration.timelineViewStateSupported] is disabled. */
    suspend fun dropAfter(key: String)

    /** Should be used, when [MatrixMessengerConfiguration.timelineViewStateSupported] is disabled. */
    suspend fun markAsRead(key: String)

    /** Should be used, when [MatrixMessengerConfiguration.timelineViewStateSupported] is disabled. */
    suspend fun finishedScrollTo(key: String)

    data class ViewState(
        val firstVisibleElement: String,
        val lastVisibleElement: String,
        val firstLoadedElement: String,
        val lastLoadedElement: String,
        val timelineIsFocused: Boolean,
        val finishedScrollTo: String?,
    )

    sealed class Wrapper {
        data object None : Wrapper()

        class View(val viewModel: SendAttachmentViewModel) : Wrapper()
    }

    sealed class Config {
        data object None : Config()

        data class SendAttachmentView(val file: FileDescriptor) : Config()
    }
}

// TODO many calculations do not support future room upgrades. Either every usage of roomId considers room upgrades or
//  instead, the room list should re-initialize the timeline with the new roomId!
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class TimelineViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    private val onOpenUserProfile: (UserId) -> Unit,
    private val onOpenMention: OpenMentionCallback,
    private val onOpenMetadata: (eventId: EventId) -> Unit,
) : MatrixClientViewModelContext by viewModelContext, TimelineViewModel {
    private val leaveRoom: LeaveRoom = get()

    init {
        log.debug { "::: init timelineViewModel: $viewModelContext" }
    }

    internal data class TimelineElementWrapper(
        val key: String,
        val roomId: RoomId,
        val eventId: EventId,
        val timelineEvent: Flow<TimelineEvent>,
        val viewModel: TimelineElementHolderViewModel,
        val lifecycle: LifecycleRegistry,
    )

    private data class OutboxElementWrapper(
        val key: String,
        val viewModel: OutboxElementHolderViewModel,
        val lifecycle: LifecycleRegistry,
    )

    private val config = get<MatrixMessengerConfiguration>()
    private val outerScope = get<CoroutineScope>()
    private val timeZone = get<TimeZone>()

    private val timeline: Timeline<TimelineElementWrapper> =
        matrixClient.room.getTimeline(
            onStateChange = { stateChange ->
                if (stateChange.removedElements.isNotEmpty()) {
                    log.debug { "forget old timeline elements" }
                    stateChange.removedElements.forEach { it.lifecycle.destroy() }
                }
            }
        ) {
            computeTimelineElement(it)
        }

    private val timelineState = timeline.state.distinctUntilChanged().shareIn(coroutineScope, WhileSubscribed(), 1)
    private val timelineElements = timelineState.map { it.elements }.stateIn(coroutineScope, Eagerly, listOf())

    private val readEvent = MutableStateFlow<Pair<RoomId, EventId>?>(null)
    private val readEventReplay =
        readEvent.buffer(0, BufferOverflow.DROP_OLDEST).shareIn(coroutineScope, Eagerly, replay = 50)

    private val readEventMarker = MutableStateFlow<Pair<RoomId, EventId>?>(null)

    private val timelineElementViewModelFactorySelector = get<TimelineElementViewModelFactorySelector>()

    override val unreadCount: StateFlow<String?> =
        readEvent
            .filterNotNull()
            .flatMapLatest { readEvent ->
                matrixClient.room
                    .getTimelineEvents(readEvent.first, readEvent.second, GetEvents.Direction.FORWARDS) {
                        decryptionTimeout = Duration.ZERO
                    }
                    .drop(1)
                    .filter(timelineElementViewModelFactorySelector::supports)
                    .take(100)
                    .scan(0) { count, _ -> count + 1 }
                    .debounce(100.milliseconds)
                    .map {
                        when {
                            it in 1..99 -> it.toString()
                            it > 99 -> "99+"
                            else -> null
                        }
                    }
            }
            .stateIn(coroutineScope, WhileSubscribed(), null)

    private val _scrollTo = MutableStateFlow<String?>(null)
    override val scrollTo = _scrollTo.asStateFlow()
    private val finishedScrollTo = MutableSharedFlow<String>(extraBufferCapacity = 0, replay = 0)
    private val scrollToMutex = Mutex()

    private val outbox =
        matrixClient.room.getOutbox(roomId = roomId).shareIn(coroutineScope, WhileSubscribed(), replay = 1)
    override val elements: StateFlow<List<BaseTimelineElementHolderViewModel>> =
        combine(
                timelineElements.filter { it.isNotEmpty() }, // prevent initial timeline with outbox only
                outbox,
            ) { elements, outbox ->
                log.debug { "compute timeline elements" }
                val timelineElements =
                    elements.map { it.viewModel } + computeOutbox(outbox, elements.map { it.timelineEvent })
                log.debug { "finished compute timeline elements" }
                timelineElements
            }
            .stateIn(coroutineScope, Eagerly, listOf())

    private val lastEventIdHistory =
        matrixClient.room
            .getById(roomId)
            .mapNotNull { it?.lastEventId }
            .scan(listOf<EventId>()) { acc, id -> (acc + id).takeLast(24) }
            .shareIn(coroutineScope, Eagerly, replay = 1)

    private val _viewState: MutableSharedFlow<TimelineViewModel.ViewState?> =
        MutableSharedFlow(extraBufferCapacity = Channel.UNLIMITED)

    data class SynchronizedViewState(
        val viewState: TimelineViewModel.ViewState,
        val elements: List<BaseTimelineElementHolderViewModel>,
    )

    private val synchronizedViewState =
        combineTransform(_viewState, elements) { viewState, elements ->
                when {
                    viewState == null -> {}
                    viewState.firstLoadedElement != elements.firstOrNull()?.key ||
                        viewState.lastLoadedElement != elements.lastOrNull()?.key -> {
                        log.trace {
                            "synchronizing loaded viewState (first=${viewState.firstLoadedElement}, last=${viewState.lastLoadedElement}) to match elements (first=${elements.firstOrNull()?.key}, last=${elements.lastOrNull()?.key})"
                        }
                    }

                    else -> {
                        log.trace {
                            "synchronized viewState (first=${viewState.firstLoadedElement}, last=${viewState.lastLoadedElement})"
                        }
                        emit(SynchronizedViewState(viewState, elements))
                    }
                }
            }
            .distinctUntilChanged()
            .shareIn(coroutineScope, Eagerly, replay = 1)

    override fun setViewState(viewState: TimelineViewModel.ViewState) {
        this._viewState.tryEmit(viewState)
    }

    override val canLoadBefore: StateFlow<Boolean?> =
        timelineState.map { it.canLoadBefore }.stateIn(coroutineScope, WhileSubscribed(), null)
    override val canLoadAfter: StateFlow<Boolean?> =
        timelineState.map { it.canLoadAfter }.stateIn(coroutineScope, WhileSubscribed(), null)

    override val isDirect: StateFlow<Boolean> =
        matrixClient.room.getById(roomId).map { it?.isDirect == true }.stateIn(coroutineScope, WhileSubscribed(), false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

    override val draggedFile: MutableStateFlow<FileDescriptor?> = MutableStateFlow(null)

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()

    override val roomHeaderViewModel: RoomHeaderViewModel =
        get<RoomHeaderViewModelFactory>()
            .create(
                viewModelContext = childContext("roomHeaderViewModel"),
                selectedRoomId = roomId,
                onBack = onBack,
                onVerifyUser = ::onVerifyUser,
                onOpenRoomSettings = onOpenSettings,
                onOpenUserProfile = onOpenUserProfile,
            )

    override val inputAreaViewModel: InputAreaViewModel =
        get<InputAreaViewModelFactory>()
            .create(
                viewModelContext = childContext("inputAreaViewModel"),
                selectedRoomId = roomId,
                onMessageReplaceFinished = ::onMessageReplaceFinished,
                onMessageReplyFinished = ::onMessageReplyFinished,
                onShowAttachmentSendView = ::onShowAttachmentSendView,
                onOpenMention = onOpenMention,
            )

    private val reportMessageRouter: ReportMessageRouter =
        ReportMessageRouterImpl(
            viewModelContext = viewModelContext,
            onShowReportMessageDialog = ::showReportMessageDialog,
            onReportMessageDialogDismiss = ::onReportMessageDialogDismiss,
        )

    internal fun onReportMessageDialogDismiss() = coroutineScope.launch {
        log.trace { "Closing report popup dialog" }
        reportMessageRouter.closeReportMessage()
    }

    internal fun showReportMessageDialog(roomId: RoomId, eventId: EventId) = coroutineScope.launch {
        reportMessageRouter.showReportMessage(roomId, eventId)
    }

    override val reportMessageStack = reportMessageRouter.stack

    private val sendAttachmentRouter =
        SendAttachmentRouterImpl(
            viewModelContext = viewModelContext.childContext("SendAttachmentRouter"),
            roomId = roomId,
        )
    override val sendAttachmentStack: Value<ChildStack<Config, Wrapper>> = sendAttachmentRouter.stack

    init {
        outerScope.removeMarkAsUnread()
        updateReadEventAndMarker()

        initTimeline()
        continuouslyLoadTimeline()

        markLastVisibleEventAsReadWhenItChanges()

        coroutineScope.launch {
            elements.first { it.isNotEmpty() } // wait for room to be fetched first
            matrixClient.user.loadMembers(roomId, wait = false)
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
                dragAndDropHandler.onDrag.collect { files -> files.firstOrNull()?.also { draggedFile.value = it } }
            }
            coroutineScope.launch { dragAndDropHandler.onDragExit.collect { draggedFile.value = null } }
        }
    }

    private fun updateReadEventAndMarker() {
        coroutineScope.launch {
            matrixClient.room
                .getAccountData<FullyReadEventContent>(roomId)
                .filterNotNull()
                .map { roomId to it.eventId }
                .filter { readEventReplay.replayCache.contains(it).not() }
                .collect { readEvent.value = it }
        }
        coroutineScope.launch {
            val currentReadEvent = readEvent.filterNotNull().first()
            if (matrixClient.room.getById(currentReadEvent.first).first()?.lastEventId != currentReadEvent.second) {
                readEventMarker.value = currentReadEvent
            } else {
                log.trace { "skipped setting readEventMarker, because timeline is fully read" }
            }
            synchronizedViewState
                .conflate()
                .map { it.viewState.timelineIsFocused }
                .collectLatest { timelineIsFocused ->
                    if (timelineIsFocused.not()) {
                        log.trace { "start setting readEventMarker" }
                        readEvent.collect { readEventMarker.value = it }
                    }
                }
        }
    }

    private fun initTimeline() {
        coroutineScope.launch {
            val initTimelineFrom =
                matrixClient.room.getAccountData<FullyReadEventContent>(roomId).first()?.eventId?.let { lastReadEventId
                    ->
                    withTimeoutOrNull(10.seconds) {
                        lastReadEventId
                            .takeIf {
                                matrixClient.room.getTimelineEvent(roomId, lastReadEventId).first() !=
                                    null // just check, that event is stored locally
                            }
                            .also {
                                if (it == null)
                                    log.warn {
                                        "cannot use fully read event as start for timeline, because event not stored locally"
                                    }
                                else log.debug { "use fully read event as start for timeline" }
                            }
                    }
                }
                    ?: matrixClient.room
                        .getById(roomId)
                        .map { it?.lastEventId }
                        .filterNotNull()
                        .first()
                        .also { log.debug { "use last known event as start for timeline" } }

            val foundTimelineEvent =
                withTimeoutOrNull(10.seconds) {
                    matrixClient.room.getTimelineEvent(roomId, initTimelineFrom).first() !=
                        null // just check, that event is stored locally
                }
            if (foundTimelineEvent == null) {
                log.error { "could not load start point of timeline" }
            }
            val scrollToKey =
                matrixClient.room.getTimelineEvent(roomId, initTimelineFrom).filterNotNull().first().asKey()
            coroutineScope {
                val waitForScrollResult = async {
                    withTimeoutOrNull(5.seconds) {
                        if (config.timelineViewStateSupported)
                            synchronizedViewState.first { it.viewState.finishedScrollTo == scrollToKey }
                        else finishedScrollTo.first { it == scrollToKey }
                    }
                }
                _scrollTo.value = scrollToKey
                initTimeline(initTimelineFrom)

                if (waitForScrollResult.await() == null) {
                    log.warn {
                        "could not scroll to $scrollToKey for timeline init, because UI did not set finishedScrollTo"
                    }
                }
                _scrollTo.value = null
            }
        }
    }

    private val initTimelineMutex = Mutex()

    private suspend fun initTimeline(startFrom: EventId) = initTimelineMutex.withLock {
        log.debug { "try init timeline from $startFrom" }
        timeline.init(
            roomId = roomId,
            startFrom = startFrom,
            configBefore = {
                fetchSize = config.timelineInitialSize.toLong() - 1
                maxSize = fetchSize
            },
        )
        val result =
            withTimeoutOrNull(30.seconds) {
                val elements =
                    if (config.timelineViewStateSupported) synchronizedViewState.map { it.elements } else elements
                elements.first {
                    it.any { it is TimelineElementHolderViewModel && it.eventId == startFrom && it.roomId == roomId }
                }
            }
        if (result == null) log.debug { "failed init timeline from $startFrom" }
        else log.debug { "finished init timeline from $startFrom" }
    }

    private fun markLastVisibleEventAsReadWhenItChanges() {
        coroutineScope.launch {
            synchronizedViewState
                .conflate()
                .map { it.viewState.lastVisibleElement to it.viewState.timelineIsFocused }
                .distinctUntilChanged()
                .throttleFirst(500.milliseconds) // we don't want to spam the server
                .collect { (lastVisibleTimelineElement, timelineIsFocused) ->
                    if (timelineIsFocused) {
                        log.debug { "mark the last visible element as read: $lastVisibleTimelineElement" }
                        markAsReadInternal(lastVisibleTimelineElement)
                    }
                }
        }
    }

    private val loadingIndicatorBefore =
        combine(
                timelineState,
                flow {
                    if (config.timelineViewStateSupported)
                        emitAll(
                            synchronizedViewState
                                .conflate()
                                .map { it.viewState.firstLoadedElement }
                                .distinctUntilChanged()
                        )
                    else emit(null)
                },
            ) { timelineState, firstLoadedElement ->
                val firstElement = timelineState.elements.firstOrNull()?.key
                val canLoadBefore = timelineState.canLoadBefore
                log.trace {
                    "canLoadBefore: firstElement=$firstElement, firstLoadedElement=$firstLoadedElement, canLoadBefore=$canLoadBefore"
                }
                when {
                    firstLoadedElement == null -> firstElement
                    firstElement != firstLoadedElement -> firstLoadedElement
                    canLoadBefore -> firstElement
                    else -> null
                }
            }
            .shareIn(coroutineScope, WhileSubscribed(), replay = 1)

    private val loadingIndicatorAfter =
        combine(
                timelineState,
                flow {
                    if (config.timelineViewStateSupported)
                        emitAll(
                            synchronizedViewState
                                .conflate()
                                .map { it.viewState.lastLoadedElement }
                                .distinctUntilChanged()
                        )
                    else emit(null)
                },
            ) { timelineState, lastLoadedElement ->
                val lastElement = timelineState.elements.lastOrNull()?.key
                val canLoadAfter = timelineState.canLoadAfter
                log.trace {
                    "canLoadBefore: firstElement=$lastElement, lastLoadedElement=$lastLoadedElement, canLoadAfter=$canLoadAfter"
                }
                when {
                    lastLoadedElement == null -> lastElement
                    lastElement != lastLoadedElement -> lastLoadedElement
                    canLoadAfter -> lastElement
                    else -> null
                }
            }
            .shareIn(coroutineScope, WhileSubscribed(), replay = 1)

    @OptIn(FlowPreview::class)
    private suspend fun computeTimelineElement(timelineEventFlow: Flow<TimelineEvent>): TimelineElementWrapper {
        val timelineEvent = timelineEventFlow.first()
        val roomId = timelineEvent.roomId
        val eventId = timelineEvent.eventId
        val sender = timelineEvent.sender
        val key = timelineEvent.asKey()

        log.trace { "compute timeline element $eventId" }
        val lifecycleRegistry = LifecycleRegistry()
        lifecycleRegistry.start()

        val showUnreadMarker =
            readEventMarker
                // prevent fast re-computations
                .throttleFirst(300.milliseconds)
                .map { it?.first == roomId && it.second == eventId }
                .distinctUntilChanged()

        val showLoadingIndicatorBefore =
            loadingIndicatorBefore
                // prevent fast re-computations
                .debounce(300.milliseconds)
                .map { it == key }
                .distinctUntilChanged()

        val showLoadingIndicatorAfter =
            loadingIndicatorAfter
                // prevent fast re-computations
                .debounce(300.milliseconds)
                .map { it == key }
                .distinctUntilChanged()

        val formattedDate =
            formatDate(Instant.fromEpochMilliseconds(timelineEvent.originTimestamp).toLocalDateTime(timeZone))
        val formattedTime =
            formatTime(Instant.fromEpochMilliseconds(timelineEvent.originTimestamp).toLocalDateTime(timeZone))

        val viewModel =
            get<TimelineElementHolderViewModelFactory>()
                .create(
                    viewModelContext = childContextWithOwnLifecycle(key, lifecycleRegistry),
                    key = key,
                    timelineEventFlow = timelineEventFlow,
                    roomId = roomId,
                    eventId = eventId,
                    sender = sender,
                    formattedDate = formattedDate,
                    formattedTime = formattedTime,
                    showUnreadMarker = showUnreadMarker,
                    showLoadingIndicatorBefore = showLoadingIndicatorBefore,
                    showLoadingIndicatorAfter = showLoadingIndicatorAfter,
                    ignoreReplacedEvents = true,
                    getReceipts = ::getReceipts,
                    onMessageReplace = ::onMessageReplace,
                    onMessageReply = ::onMessageReply,
                    onMessageReport = ::onShowReportMessageModal,
                    onOpenMention = onOpenMention,
                    onOpenMetadata = onOpenMetadata,
                    jumpTo = ::jumpTo,
                )
        return TimelineElementWrapper(
            key = key,
            roomId = roomId,
            eventId = eventId,
            timelineEvent = timelineEventFlow,
            viewModel = viewModel,
            lifecycle = lifecycleRegistry,
        )
    }

    private val outboxElementCache = mutableMapOf<String, OutboxElementWrapper>()

    private suspend fun computeOutbox(
        outbox: List<Flow<RoomOutboxMessage<*>?>>,
        timelineEventList: List<Flow<TimelineEvent>>,
    ): List<OutboxElementHolderViewModel> = coroutineScope {
        log.debug { "compute outbox" }

        if (outbox.isEmpty()) {
            log.debug { "empty outbox" }
            return@coroutineScope emptyList()
        }

        val outboxMap = outbox.mapNotNull { it.first()?.transactionId?.let { tid -> tid to it } }.toMap()

        val timelineEventsTransactionIds =
            timelineEventList.mapNotNull { it.first().event.unsigned?.transactionId }.toSet()

        val relevantOutboxEntries = outboxMap.filterNot { (transactionId, _) ->
            timelineEventsTransactionIds.contains(transactionId)
        }

        outboxElementCache
            .mapNotNull { (key, wrapper) ->
                if (relevantOutboxEntries[key] == null) {
                    wrapper.lifecycle.destroy()
                    key
                } else null
            }
            .forEach { key -> outboxElementCache.remove(key) }

        relevantOutboxEntries
            .mapNotNull { (transactionId, outboxMessage) ->
                outboxElementCache[transactionId]?.viewModel
                    ?: run {
                        val outboxMessageSnapshot = outboxMessage.first()
                        if (outboxMessageSnapshot == null) return@run null
                        val formattedDate = formatDate(outboxMessageSnapshot.createdAt.toLocalDateTime(timeZone))
                        val formattedTime = formatTime(outboxMessageSnapshot.createdAt.toLocalDateTime(timeZone))

                        val lifecycleRegistry = LifecycleRegistry()
                        lifecycleRegistry.start()
                        this@TimelineViewModelImpl.get<OutboxElementHolderViewModelFactory>()
                            .create(
                                viewModelContext = childContextWithOwnLifecycle(transactionId, lifecycleRegistry),
                                key = transactionId.asKey(roomId),
                                outboxMessageFlow = outboxMessage,
                                roomId = roomId,
                                transactionId = transactionId,
                                formattedDate = formattedDate,
                                formattedTime = formattedTime,
                                onOpenMention = onOpenMention,
                                jumpTo = ::jumpTo,
                            )
                            .also {
                                outboxElementCache[transactionId] =
                                    OutboxElementWrapper(transactionId, it, lifecycleRegistry)
                            }
                    }
            }
            .also { log.debug { "finished compute outbox" } }
    }

    override fun errorDismiss() {
        error.value = null
    }

    private fun onShowAttachmentSendView(file: FileDescriptor) {
        coroutineScope.launch { sendAttachmentRouter.showAttachmentSendView(file) }
    }

    private fun onShowReportMessageModal(roomId: RoomId, eventId: EventId) = coroutineScope.launch {
        log.debug { "report to message $eventId" }
        reportMessageRouter.showReportMessage(roomId, eventId)
    }

    private fun onMessageReplace(roomId: RoomId, eventId: EventId) {
        timelineElements.value
            .filterNot { it.eventId == eventId && it.roomId == roomId }
            .forEach { it.viewModel.endReplace() }
        inputAreaViewModel.replaceMessage(roomId, eventId)
    }

    private fun onMessageReplaceFinished(roomId: RoomId, eventId: EventId) {
        timelineElements.value.firstOrNull { it.eventId == eventId && it.roomId == roomId }?.viewModel?.endReplace()
            ?: log.warn { "try to end edit of timeline event that is not present ($eventId)" }
    }

    private fun onMessageReply(roomId: RoomId, eventId: EventId) {
        timelineElements.value
            .filterNot { it.eventId == eventId && it.roomId == roomId }
            .forEach { it.viewModel.endReply() }
        inputAreaViewModel.replyMessage(roomId, eventId)
    }

    private fun onMessageReplyFinished(roomId: RoomId, eventId: EventId) {
        timelineElements.value.firstOrNull { it.eventId == eventId && it.roomId == roomId }?.viewModel?.endReply()
            ?: log.warn { "try to end reply to timeline event that is not present (${eventId})" }
    }

    override fun leaveRoom() {
        coroutineScope.launch {
            if (matrixClient.syncState.value == SyncState.ERROR) {
                error.value = i18n.timelineLeaveRoomErrorOffline()
                return@launch
            }

            leaveRoom(matrixClient, roomId, forget = false)
                .onSuccess { onBack() }
                .onFailure {
                    if (it is CancellationException) {
                        return@launch
                    }

                    log.error(it) { "cannot leave room $roomId" }
                    val groupOrChat =
                        if (isDirect.value) i18n.eventChangeChatGenitive() else i18n.eventChangeGroupGenitive()
                    error.value = i18n.timelineLeaveRoomError(groupOrChat)
                }
        }
    }

    private val loadConfig: GetTimelineEventsConfig.() -> Unit = {
        fetchSize = config.timelineFetchSize.toLong()
        maxSize = config.timelineBuffer.toLong()
    }

    private fun continuouslyLoadTimeline() {
        coroutineScope.launch {
            flow {
                    var previous: SynchronizedViewState? = null
                    combineTransform(synchronizedViewState.conflate(), scrollTo) { synchronizedViewState, scrollTo ->
                            if (scrollTo == null) emit(synchronizedViewState)
                        }
                        .distinctUntilChanged()
                        .collect { current ->
                            emit(current)
                            val finalPrevious = previous
                            val previousLastElement = finalPrevious?.elements?.lastOrNull()
                            val currentLastElement = current.elements.lastOrNull()
                            log.trace {
                                """
                                    continuouslyLoadTimeline (scroll):
                                        synchronizedViewStateHistoryValue.previous.viewState  ${finalPrevious?.viewState}
                                        synchronizedViewStateHistoryValue.current.viewState   ${current.viewState}
                                        synchronizedViewStateHistoryValue.previous.elements   ${finalPrevious?.elements?.map { it.key } }
                                        synchronizedViewStateHistoryValue.current.elements    ${current.elements.map { it.key }}
                                """
                                    .trimIndent()
                            }

                            // When a scroll condition is found, the state needs to be skipped in the calculation.
                            // Otherwise, the end-position cannot be detected.
                            when {
                                !current.viewState.timelineIsFocused -> {
                                    log.trace { "don't scroll: timeline not focused" }
                                    previous = current
                                }
                                finalPrevious == null ||
                                    previousLastElement == null ||
                                    currentLastElement == null ||
                                    previousLastElement.key == currentLastElement.key -> {
                                    log.trace { "don't scroll: no new elements at end" }
                                    previous = current
                                }
                                finalPrevious.viewState.lastVisibleElement != previousLastElement.key -> {
                                    log.trace { "don't scroll: previous lastVisibleElement was not the last element" }
                                    previous = current
                                }
                                finalPrevious.viewState.lastVisibleElement != previousLastElement.key -> {
                                    log.trace { "don't scroll: previous lastVisibleElement was not the last element" }
                                    previous = current
                                }
                                previousLastElement is OutboxElementHolderViewModel -> {
                                    log.trace { "scroll: previously an outbox was the last visible element" }
                                    scrollTo(currentLastElement.key)
                                    previous = null
                                }
                                previousLastElement is TimelineElementHolderViewModel &&
                                    lastEventIdHistory.first().contains(previousLastElement.eventId) -> {
                                    log.trace { "scroll: previously the last event id was the last visible element" }
                                    scrollTo(currentLastElement.key)
                                    previous = null
                                }
                                else -> {
                                    previous = current
                                }
                            }
                        }
                }
                .conflate()
                .throttleFirst(250.milliseconds)
                .collect { synchronizedViewStateValue ->
                    val (viewState, elements) = synchronizedViewStateValue

                    val timelineElements = elements.filterIsInstance<TimelineElementHolderViewModel>()
                    val timelineSize = timelineElements.size
                    val visibleTimelineSize = timelineElements.count {
                        it.element.filterNotNull().first() !is TimelineElementViewModel.Empty
                    }
                    if (timelineSize == 0) return@collect
                    val timelineMaxSize = config.timelineMaxSize + 2 * config.timelineBuffer

                    val (firstVisibleElement, lastVisibleElement) =
                        viewState.firstVisibleElement to viewState.lastVisibleElement
                    val indexOfFirstVisibleTimelineElement = // ignores outbox
                        elements
                            .asFlow()
                            .withIndex()
                            .dropWhile { it.value.key != firstVisibleElement }
                            .firstOrNull { it.value is TimelineElementHolderViewModel }
                            ?.index ?: -1
                    val indexOfLastVisibleTimelineElement = // ignores outbox
                        elements
                            .asReversedIndexedFlow()
                            .dropWhile { it.value.key != lastVisibleElement }
                            .firstOrNull { it.value is TimelineElementHolderViewModel }
                            ?.index ?: -1

                    val isInBufferBefore = indexOfFirstVisibleTimelineElement in 0..<config.timelineBuffer
                    val isInBufferAfter =
                        indexOfLastVisibleTimelineElement >= 0 &&
                            indexOfLastVisibleTimelineElement in (timelineSize - config.timelineBuffer)..<timelineSize

                    log.trace {
                        """
                            continuouslyLoadTimeline (load):
                                indexOfFirstVisibleTimelineElement=$indexOfFirstVisibleTimelineElement (isInBufferBefore=$isInBufferBefore)
                                indexOfLastVisibleTimelineElement=$indexOfLastVisibleTimelineElement (isInBufferAfter=$isInBufferAfter)
                                timelineSize=$timelineSize visibleTimelineSize=$visibleTimelineSize (timelineElementViewModels=${elements.map { it.key }})
                        """
                            .trimIndent()
                    }

                    if (isInBufferBefore) {
                        log.debug { "load timeline events before" }
                        timeline.loadBefore(loadConfig)

                        if (visibleTimelineSize > timelineMaxSize && !isInBufferAfter) {
                            log.debug { "drop timeline events after" }
                            val dropAfterElement = timelineElements[timelineSize - config.timelineBuffer - 1]
                            timeline.dropAfter(dropAfterElement.roomId, dropAfterElement.eventId)
                        }
                    }
                    if (isInBufferAfter) {
                        log.debug { "load timeline events after" }
                        coroutineScope {
                            select {
                                    async { synchronizedViewState.first { it != synchronizedViewStateValue } }
                                        .onAwait { null }
                                    // IMPORTANT: when we are at the end of the timeline, this suspends and waits for
                                    // new messages
                                    async { timeline.loadAfter(loadConfig) }.onAwait { it }
                                }
                                .also { currentCoroutineContext().cancelChildren() }
                        }
                        if (visibleTimelineSize > timelineMaxSize && !isInBufferBefore) {
                            log.debug { "drop timeline events before" }
                            val dropBeforeElement = timelineElements[config.timelineBuffer]
                            timeline.dropBefore(dropBeforeElement.roomId, dropBeforeElement.eventId)
                        }
                    }
                    log.trace { "continuouslyLoadTimeline (finished)" }
                }
        }
    }

    private suspend fun scrollTo(key: String) = scrollToMutex.withLock {
        if (config.timelineViewStateSupported) {
            val findKeyResult =
                withTimeoutOrNull(5.seconds) {
                    synchronizedViewState.conflate().first { it.elements.any { it.key == key } }
                    Unit
                }
            if (findKeyResult == null) {
                log.warn { "could not scroll to $key, because element not found in loadedElements" }
            }
        }
        log.debug { "scroll to $key" }
        coroutineScope {
            val waitForScrollResult = async {
                withTimeoutOrNull(5.seconds) {
                    if (config.timelineViewStateSupported)
                        synchronizedViewState.first { it.viewState.finishedScrollTo == key }
                    else finishedScrollTo.first { it == key }
                }
            }
            _scrollTo.value = key
            if (waitForScrollResult.await() == null) {
                log.warn { "could not scroll to $key, because UI did not set finishedScrollTo" }
            }
            _scrollTo.value = null
        }
    }

    // used for jumping to replied elements
    internal fun jumpTo(roomId: RoomId, eventId: EventId) {
        coroutineScope.launch {
            val elementsFlow =
                if (config.timelineViewStateSupported) synchronizedViewState.map { it.elements } else elements
            var element =
                elements.first().firstOrNull {
                    it is TimelineElementHolderViewModel && it.eventId == eventId && it.roomId == roomId
                }
            if (element == null) {
                log.debug { "Element $roomId-$eventId is not loaded, re-initialize timeline" }
                initTimeline(eventId)
                element =
                    withTimeoutOrNull(10.seconds) {
                        elementsFlow
                            .mapNotNull {
                                it.firstOrNull {
                                    it is TimelineElementHolderViewModel && it.eventId == eventId && it.roomId == roomId
                                }
                            }
                            .first()
                    }
            }

            if (element == null) {
                log.error { "Element could not be found" }
                return@launch
            }

            val elementKey = element.key
            log.debug { "Jump to element $elementKey in timeline" }
            scrollTo(elementKey)
        }
    }

    override fun jumpToEndOfTimeline() {
        coroutineScope.launch {
            val lastEventId = matrixClient.room.getById(roomId).mapNotNull { it?.lastEventId }.first()
            val lastTimelineEventKey =
                matrixClient.room.getTimelineEvent(roomId, lastEventId).filterNotNull().first().asKey()
            val elements =
                if (config.timelineViewStateSupported) synchronizedViewState.map { it.elements }.first()
                else elements.first()
            val lastOutboxElementKey = (elements.lastOrNull() as? OutboxElementHolderViewModel)?.key
            if (elements.none { it.key == lastTimelineEventKey }) {
                log.debug { "last timeline event $lastEventId not part of timeline, therefore re-init timeline" }
                initTimeline(lastEventId)
            }
            val scrollToKey = lastOutboxElementKey ?: lastTimelineEventKey
            log.debug { "jump to end of timeline (key=$scrollToKey)" }
            scrollTo(scrollToKey)
        }
    }

    override suspend fun loadBefore() {
        if (config.timelineViewStateSupported)
            throw IllegalStateException(
                "cannot be used with MatrixMessengerConfiguration.timelineViewStateSupported to be enabled"
            )
        timeline.loadBefore(loadConfig)
    }

    override suspend fun loadAfter() {
        if (config.timelineViewStateSupported)
            throw IllegalStateException(
                "cannot be used with MatrixMessengerConfiguration.timelineViewStateSupported to be enabled"
            )
        timeline.loadAfter(loadConfig)
    }

    override suspend fun dropBefore(key: String) {
        if (config.timelineViewStateSupported)
            throw IllegalStateException(
                "cannot be used with MatrixMessengerConfiguration.timelineViewStateSupported to be enabled"
            )
        val element = timelineElements.value.find { it.key == key }
        if (element != null) {
            timeline.dropBefore(element.roomId, element.eventId)
        }
    }

    override suspend fun dropAfter(key: String) {
        if (config.timelineViewStateSupported)
            throw IllegalStateException(
                "cannot be used with MatrixMessengerConfiguration.timelineViewStateSupported to be enabled"
            )
        val element = timelineElements.value.find { it.key == key }
        if (element != null) {
            timeline.dropAfter(element.roomId, element.eventId)
        }
    }

    override suspend fun markAsRead(key: String) {
        if (config.timelineViewStateSupported)
            throw IllegalStateException(
                "cannot be used with MatrixMessengerConfiguration.timelineViewStateSupported to be enabled"
            )
        markAsReadInternal(key)
    }

    override suspend fun finishedScrollTo(key: String) {
        finishedScrollTo.emit(key)
    }

    private suspend fun markAsReadInternal(key: String) {
        val (alreadyReadUntilRoomId, alreadyReadUntil) = readEvent.value ?: (null to null)
        val elements = elements.value
        val (nextReadUntilRoomId, nextReadUntil) =
            elements
                .asReversedFlow()
                .dropWhile { it.key != key }
                .filterIsInstance<TimelineElementHolderViewModel>()
                .firstOrNull()
                ?.let { it.roomId to it.eventId } ?: (null to null)

        if (nextReadUntil == null || nextReadUntilRoomId == null) {
            log.trace { "ignore event marked as read, because not found in loaded timeline" }
            return
        }
        if (nextReadUntil == alreadyReadUntil && nextReadUntilRoomId == alreadyReadUntilRoomId) {
            log.trace { "ignore event marked as read, because already marked as read" }
            return
        }

        val nextReadUntilIsAfterAlreadyReadUntil =
            if (alreadyReadUntil == null || alreadyReadUntilRoomId == null) true
            else
                coroutineScope {
                    val searchBefore = async {
                        matrixClient.room
                            .getTimelineEvents(nextReadUntilRoomId, nextReadUntil, GetEvents.Direction.BACKWARDS) {
                                decryptionTimeout = Duration.ZERO
                            }
                            .map { it.first() }
                            .mapNotNull { timelineEvent ->
                                when {
                                    timelineEvent.roomId == alreadyReadUntilRoomId &&
                                        timelineEvent.eventId == alreadyReadUntil -> true
                                    timelineEvent.isFirst -> false
                                    timelineEvent.roomId != roomId -> false // don't search between room upgrades
                                    else -> null
                                }
                            }
                            .first()
                    }
                    val searchAfter = async {
                        matrixClient.room
                            .getTimelineEvents(nextReadUntilRoomId, nextReadUntil, GetEvents.Direction.FORWARDS) {
                                decryptionTimeout = Duration.ZERO
                            }
                            .map { it.first() }
                            .mapNotNull { timelineEvent ->
                                when {
                                    timelineEvent.roomId == alreadyReadUntilRoomId &&
                                        timelineEvent.eventId == alreadyReadUntil -> false
                                    timelineEvent.isLast -> true
                                    timelineEvent.roomId != roomId -> false // don't search between room upgrades
                                    else -> null
                                }
                            }
                            .first()
                    }
                    select {
                            searchBefore.onAwait { it }
                            searchAfter.onAwait { it }
                        }
                        .also {
                            searchBefore.cancel()
                            searchAfter.cancel()
                        }
                }

        if (nextReadUntilIsAfterAlreadyReadUntil.not()) {
            log.trace { "ignore event marked as read, because a newer event was already marked as read" }
            return
        }

        readEvent.value = (nextReadUntilRoomId to nextReadUntil)
        val readMarkerIsPublic = messengerSettings[userId].first()?.base?.readMarkerIsPublic == true

        outerScope
            .launch {
                // we have to execute this in the outerScope, since otherwise the view model would be cleaned up and
                // with
                // it the scope where this code is executed
                launch {
                    matrixClient.api.room
                        .setReadMarkers(
                            roomId = nextReadUntilRoomId,
                            read = if (readMarkerIsPublic) nextReadUntil else null,
                            fullyRead = nextReadUntil,
                            privateRead = nextReadUntil,
                        )
                        .onFailure {
                            log.error(it) { "cannot set read marker for event $nextReadUntil in $nextReadUntilRoomId" }
                        }
                        .onSuccess {
                            log.debug {
                                "successfully set read marker for message: $nextReadUntil in $nextReadUntilRoomId"
                            }
                        }
                }
                removeMarkAsUnread()
            }
            .join()
    }

    private fun CoroutineScope.removeMarkAsUnread() {
        this.launch {
            if (
                matrixClient.room
                    .getAccountData(roomId, MarkedUnreadEventContent::class)
                    .map { it?.unread }
                    .firstOrNull() ?: false
            ) {
                matrixClient.api.room
                    .setAccountData(MarkedUnreadEventContent(false), roomId, userId)
                    .onFailure { log.warn(it) { "could not reset unread in $roomId" } }
                    .onSuccess { log.debug { "successfully reset unread in $roomId" } }
            }
        }
    }

    private val getReceiptsByEventCache = concurrentMutableMap<RoomId, Flow<Map<EventId, Set<UserId>>>>()

    private fun getReceipts(roomId: RoomId): Flow<Map<EventId, Set<UserId>>> = flow {
        emitAll(
            getReceiptsByEventCache.read { get(roomId) }
                ?: getReceiptsByEventCache.write {
                    getOrPut(roomId) {
                        matrixClient.user
                            .getAllReceipts(roomId)
                            .byEventId(userId)
                            .stateIn(coroutineScope, WhileSubscribed(), emptyMap())
                    }
                }
        )
    }

    private fun onVerifyUser() {
        coroutineScope.launch {
            log.debug { "try to create new user verification" }
            val isDirectRoom = matrixClient.room.getById(roomId).first()?.isDirect == true
            log.debug { "is direct room: $isDirectRoom" }
            val otherUserId =
                matrixClient.user
                    .getAll(roomId)
                    .map { users -> (users.keys - matrixClient.userId).firstOrNull() }
                    .first() ?: return@launch
            log.debug { "create new user verification with user $otherUserId" }
            matrixClient.verification.createUserVerificationRequest(otherUserId)
        }
    }

    private fun TimelineEvent.asKey() =
        event.unsigned?.transactionId?.asKey(event.roomId) ?: eventId.asKey(event.roomId)

    private fun EventId.asKey(roomId: RoomId? = null) = (roomId ?: this@TimelineViewModelImpl.roomId).full + "-" + full

    private fun String.asKey(roomId: RoomId? = null) = (roomId ?: this@TimelineViewModelImpl.roomId).full + "-" + this
}

class PreviewTimelineViewModel : TimelineViewModel {
    override val elements: MutableStateFlow<List<BaseTimelineElementHolderViewModel>> =
        MutableStateFlow(listOf(PreviewTimelineElementViewModel1(), PreviewTimelineElementViewModel2()))
    override val canLoadBefore: StateFlow<Boolean?> = MutableStateFlow(null)
    override val canLoadAfter: StateFlow<Boolean?> = MutableStateFlow(null)
    override val scrollTo: StateFlow<String?> = MutableStateFlow(null)
    override val isDirect: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val roomHeaderViewModel: RoomHeaderViewModel = PreviewRoomHeaderViewModel()
    override val inputAreaViewModel: InputAreaViewModel = PreviewInputAreaViewModel()
    override val sendAttachmentStack: Value<ChildStack<Config, Wrapper>> =
        MutableValue(ChildStack(configuration = Config.None, instance = Wrapper.None))
    override val unreadCount: StateFlow<String?> = MutableStateFlow(null)

    override val reportMessageStack: Value<ChildStack<ReportMessageRouter.Config, ReportMessageRouter.Wrapper>> =
        MutableValue(
            ChildStack(
                active =
                    Child.Created(
                        configuration = ReportMessageRouter.Config.None,
                        instance = ReportMessageRouter.Wrapper.None,
                    )
            )
        )
    override val draggedFile: MutableStateFlow<FileDescriptor?> = MutableStateFlow(null)

    init {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            delay(3.seconds)
            elements.value =
                listOf(
                    PreviewTimelineElementViewModel1(),
                    PreviewTimelineElementViewModel2(),
                    PreviewTimelineElementViewModel1(),
                )
        }
    }

    override fun setViewState(viewState: TimelineViewModel.ViewState) {}

    override fun errorDismiss() {}

    override fun leaveRoom() {}

    override fun jumpToEndOfTimeline() {}

    override suspend fun loadBefore() {}

    override suspend fun loadAfter() {}

    override suspend fun dropBefore(key: String) {}

    override suspend fun dropAfter(key: String) {}

    override suspend fun markAsRead(key: String) {}

    override suspend fun finishedScrollTo(key: String) {}
}
