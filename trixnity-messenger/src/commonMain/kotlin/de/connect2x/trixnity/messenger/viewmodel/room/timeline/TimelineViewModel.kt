package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.start
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
import de.connect2x.trixnity.messenger.viewmodel.util.DirectRoom
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import de.connect2x.trixnity.messenger.viewmodel.util.takeWhileInclusive
import de.connect2x.trixnity.messenger.viewmodel.util.throttleFirst
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
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.flatten
import net.folivo.trixnity.client.flattenNotNull
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.Timeline
import net.folivo.trixnity.client.room.getAccountData
import net.folivo.trixnity.client.store.RoomOutboxMessage
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.store.roomId
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.FullyReadEventContent
import net.folivo.trixnity.core.model.events.m.ReactionEventContent
import net.folivo.trixnity.core.model.events.m.ReceiptType.Read
import net.folivo.trixnity.utils.concurrentMutableMap
import org.koin.core.component.get
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger {}

interface TimelineViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        roomId: RoomId,
        isBackButtonVisible: MutableStateFlow<Boolean>,
        onShowSettings: () -> Unit,
        onBack: () -> Unit,
        onOpenMention: OpenMentionCallback,
    ): TimelineViewModel {
        return TimelineViewModelImpl(
            viewModelContext,
            roomId,
            isBackButtonVisible,
            onShowSettings,
            onBack,
            onOpenMention
        )
    }

    companion object : TimelineViewModelFactory
}

/**
 * Handles the timeline of a room and provides a list of view models that represent all elements of the timeline.
 */
interface TimelineViewModel {
    val elements: StateFlow<List<BaseTimelineElementHolderViewModel>>

    /**
     * Use this to set the state of the current UI.
     */
    val viewState: MutableStateFlow<ViewState?>

    /**
     * Emits a unique String each time the view should scroll to the given key. String is the key from [elements].
     */
    val scrollTo: Flow<String>
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

    fun jumpToEndOfTimeline()

    /**
     * Should be used, when [viewState] cannot bet set.
     */
    suspend fun loadBefore()

    /**
     * Should be used, when [viewState] cannot bet set.
     */
    suspend fun loadAfter()

    /**
     * Should be used, when [viewState] cannot bet set.
     */
    suspend fun dropBefore(key: String)

    /**
     * Should be used, when [viewState] cannot bet set.
     */
    suspend fun dropAfter(key: String)

    /**
     * Should be used, when [viewState] cannot bet set.
     */
    suspend fun markAsRead(key: String)

    /**
     * Should be used, when [viewState] cannot bet set.
     */
    fun markAsFullyRead()

    data class ViewState(
        val firstVisibleElement: String,
        val lastVisibleElement: String,
        val windowIsFocused: Boolean,
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
    private val isBackButtonVisible: MutableStateFlow<Boolean>,
    private val onShowSettings: () -> Unit,
    private val onBack: () -> Unit,
    private val onOpenMention: OpenMentionCallback,
) : MatrixClientViewModelContext by viewModelContext, TimelineViewModel {

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

    private val outerScope = get<CoroutineScope>()
    private val timeZone = get<TimeZone>()

    private val timelineStartFrom = MutableSharedFlow<EventId>(replay = 1)
    private val timeline: StateFlow<Timeline<TimelineElementWrapper>?> =
        timelineStartFrom.mapLatest { startFrom ->
            val oldTimeline = timeline.value
            if (oldTimeline != null) {
                log.debug { "forget old timeline elements" }
                oldTimeline.state.first().elements.forEach { it.lifecycle.destroy() }
            }
            log.debug { "try init timeline from $startFrom" }
            val newTimeline: Timeline<TimelineElementWrapper> =
                matrixClient.room.getTimeline(roomId) {
                    computeTimelineElement(it)
                }
            newTimeline.init(startFrom, configBefore = {
                maxSize = fetchSize
            })
            log.debug { "finished init timeline from $startFrom" }
            newTimeline
        }.stateIn(coroutineScope, WhileSubscribed(), null)
    private val timelineState =
        timeline.filterNotNull().flatMapLatest { it.state }.distinctUntilChanged()
            .shareIn(coroutineScope, WhileSubscribed(), 1)
    private val timelineElements =
        timelineState.map { it.elements }
            .stateIn(coroutineScope, Eagerly, listOf())
    private val timelineEvents =
        timelineElements.map { it.map { it.timelineEvent } }
            .stateIn(coroutineScope, Eagerly, listOf())

    private val readEvent = MutableStateFlow<EventId?>(null)
    private val fullyReadEvent = MutableStateFlow<EventId?>(null)

    private val outbox =
        matrixClient.room.getOutbox(roomId = roomId)
            .shareIn(coroutineScope, WhileSubscribed(), replay = 1)
    override val elements: StateFlow<List<BaseTimelineElementHolderViewModel>> =
        combine(
            timelineElements,
            outbox,
        ) { elements, outbox ->
            log.debug { "compute timeline elements" }
            val timelineElements = elements.map { it.viewModel } +
                    computeOutbox(outbox, elements.map { it.timelineEvent })
            log.debug { "finished compute timeline elements" }
            timelineElements
        }.stateIn(coroutineScope, WhileSubscribed(), listOf())

    override val scrollTo: MutableSharedFlow<String> =
        MutableSharedFlow(extraBufferCapacity = 1)

    override val viewState: MutableStateFlow<TimelineViewModel.ViewState?> = MutableStateFlow(null)

    override val isDirect: StateFlow<Boolean> =
        matrixClient.room.getById(roomId).map { it?.isDirect == true }
            .stateIn(coroutineScope, WhileSubscribed(), false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

    override val draggedFile: MutableStateFlow<FileDescriptor?> = MutableStateFlow(null)

    private val directRoom = get<DirectRoom>()
    private val messengerSettings = get<MatrixMessengerSettingsHolder>()

    override val roomHeaderViewModel: RoomHeaderViewModel =
        get<RoomHeaderViewModelFactory>().create(
            viewModelContext = childContext("roomHeaderViewModel"),
            selectedRoomId = roomId,
            isBackButtonVisible = isBackButtonVisible,
            onBack = onBack,
            onVerifyUser = ::onVerifyUser,
            onShowRoomSettings = onShowSettings,
        )

    override val inputAreaViewModel: InputAreaViewModel =
        get<InputAreaViewModelFactory>().create(
            viewModelContext = childContext("inputAreaViewModel"),
            selectedRoomId = roomId,
            onMessageReplaceFinished = ::onMessageReplaceFinished,
            onMessageReplyFinished = ::onMessageReplyFinished,
            onShowAttachmentSendView = ::onShowAttachmentSendView,
            onOpenMention = onOpenMention,
        )

    private val reportMessageRouter: ReportMessageRouter = ReportMessageRouterImpl(
        viewModelContext = viewModelContext,
        onShowReportMessageDialog = ::showReportMessageDialog,
        onReportMessageDialogDismiss = ::onReportMessageDialogDismiss
    )

    internal fun onReportMessageDialogDismiss() = coroutineScope.launch {
        log.trace { "Closing report popup dialog" }
        reportMessageRouter.closeReportMessage()
    }

    internal fun showReportMessageDialog(roomId: RoomId, eventId: EventId) = coroutineScope.launch {
        reportMessageRouter.showReportMessage(roomId, eventId)
    }

    override val reportMessageStack = reportMessageRouter.stack

    // TODO should be router
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
                selectedRoomId = roomId,
                onCloseAttachmentSendView = ::closeAttachmentSendView,
            )
        )
    }

    init {
        coroutineScope.launch {
            matrixClient.user.getReceiptsById(roomId, matrixClient.userId)
                .filterNotNull()
                .map { it.receipts[Read]?.eventId }
                .collect {
                    readEvent.value = it
                }
        }
        coroutineScope.launch {
            matrixClient.room.getAccountData<FullyReadEventContent>(roomId).filterNotNull()
                .map { it.eventId }
                .collect {
                    fullyReadEvent.value = it
                }
        }

        initTimeline()
        continuouslyLoadBefore()
        continuouslyLoadAfter()
        continuouslyDropBefore()
        continuouslyDropAfter()
        scrollToEndOnNewOutboxElement()

        markLastVisibleEventAsReadWhenItChanges()
        markFullyReadWhenRoomOrAppIsClosed()
        markFullyReadWhenFocusIsLost()

        coroutineScope.launch {
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
                matrixClient.room.getAccountData<FullyReadEventContent>(roomId)
                    .first()?.eventId?.let { lastReadEventId ->
                        withTimeoutOrNull(10.seconds) {
                            lastReadEventId.takeIf {
                                matrixClient.room.getTimelineEvent(roomId, lastReadEventId)
                                    .first() != null // just check, that event is stored locally
                            }.also {
                                if (it == null) log.warn { "cannot use fully read event as start for timeline, because event not stored locally" }
                                else log.debug { "use fully read event as start for timeline" }
                            }
                        }
                    } ?: matrixClient.room.getById(roomId).map { it?.lastEventId }
                    .filterNotNull().first()
                    .also { log.debug { "use last known event as start for timeline" } }

            val foundTimelineEvent =
                withTimeoutOrNull(10.seconds) {
                    matrixClient.room.getTimelineEvent(roomId, initTimelineFrom)
                        .first() != null // just check, that event is stored locally
                }
            if (foundTimelineEvent == null) {
                log.error { "could not load start point of timeline" }
            }
            timelineStartFrom.emit(initTimelineFrom)
            scheduleScrollTo(initTimelineFrom.asKey(roomId))
        }
    }

    private fun scrollToEndOnNewOutboxElement() {
        coroutineScope.launch {
            outbox.flatten()
                .scan(emptySet<String>()) { transactionIdsOld, outboxNew ->
                    val transactionIdsNew =
                        outboxNew
                            .filter { it.content !is ReactionEventContent }
                            .map { it.transactionId }
                            .toSet()
                    val diff = (transactionIdsNew - transactionIdsOld).toSet()
                    if (diff.isNotEmpty()) {
                        log.debug { "submitted a new message to the outbox -> should scroll to it" }
                        val lastEventId =
                            matrixClient.room.getById(roomId).map { it?.lastEventId }.filterNotNull().first()
                        timelineStartFrom.emit(lastEventId)
                        scheduleScrollTo(diff.last().asKey(roomId))
                    }
                    transactionIdsNew
                }.collect()
        }
    }

    private fun markLastVisibleEventAsReadWhenItChanges() {
        coroutineScope.launch {
            viewState
                .filterNotNull()
                .map { it.lastVisibleElement to it.windowIsFocused }
                .distinctUntilChanged()
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
            viewState
                .filterNotNull()
                .map { it.windowIsFocused }
                .distinctUntilChanged()
                .collectLatest {
                    if (it.not()) markAsFullyRead()
                }
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun computeTimelineElement(
        timelineEventFlow: Flow<TimelineEvent>,
    ): TimelineElementWrapper {
        val timelineEvent = timelineEventFlow.first()
        val roomId = timelineEvent.roomId
        val eventId = timelineEvent.eventId
        val sender = timelineEvent.sender
        val key = timelineEvent.event.unsigned?.transactionId?.asKey(timelineEvent.roomId)
            ?: eventId.asKey(timelineEvent.roomId)
        log.trace { "compute timeline element $eventId" }
        val lifecycleRegistry = LifecycleRegistry()
        lifecycleRegistry.start()
        val canLoadBefore = timelineState.map {
            it.canLoadBefore && it.elements.firstOrNull()?.viewModel?.eventId == eventId
        }
        val canLoadAfter = timelineState.map {
            it.canLoadAfter && it.elements.lastOrNull()?.viewModel?.eventId == eventId
        }
            // prevent flicker in UI, because for a short moment, this is true (while the UI loads new elements)
            .debounce(300.milliseconds)

        val formattedDate =
            formatDate(Instant.fromEpochMilliseconds(timelineEvent.originTimestamp).toLocalDateTime(timeZone))
        val formattedTime =
            formatTime(Instant.fromEpochMilliseconds(timelineEvent.originTimestamp).toLocalDateTime(timeZone))

        val viewModel = get<TimelineElementHolderViewModelFactory>().create(
            viewModelContext = childContextWithOwnLifecycle(lifecycleRegistry),
            key = key,
            timelineEventFlow = timelineEventFlow,
            roomId = roomId,
            eventId = eventId,
            sender = sender,
            formattedDate = formattedDate,
            formattedTime = formattedTime,
            canLoadBefore = canLoadBefore,
            canLoadAfter = canLoadAfter,
            getReceipts = ::getReceipts,
            onMessageReplace = ::onMessageReplace,
            onMessageReply = ::onMessageReply,
            onMessageReport = ::onShowReportMessageModal,
            onOpenMention = onOpenMention,
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

    private val outboxElementHolderViewModelCache = mutableMapOf<String, OutboxElementWrapper>()

    private suspend fun computeOutbox(
        outbox: List<Flow<RoomOutboxMessage<*>?>>,
        timelineEventList: List<Flow<TimelineEvent>>,
    ): List<OutboxElementHolderViewModel> = coroutineScope {
        log.debug { "compute outbox" }

        if (outbox.isEmpty()) {
            log.debug { "empty outbox" }
            return@coroutineScope emptyList()
        }

        val outboxMap = outbox.mapNotNull {
            it.first()?.transactionId?.let { tid -> tid to it }
        }.toMap()

        val timelineEventsTransactionIds =
            timelineEventList.mapNotNull { it.first().event.unsigned?.transactionId }.toSet()

        val relevantOutboxEntries = outboxMap
            .filterNot { (transactionId, _) -> timelineEventsTransactionIds.contains(transactionId) }

        outboxElementHolderViewModelCache.mapNotNull { (key, wrapper) ->
            if (relevantOutboxEntries[key] == null) {
                wrapper.lifecycle.destroy()
                key
            } else null
        }.forEach { key -> outboxElementHolderViewModelCache.remove(key) }

        relevantOutboxEntries.mapNotNull { (transactionId, outboxMessage) ->
            outboxElementHolderViewModelCache[transactionId]?.viewModel
                ?: run {
                    val outboxMessageSnapshot = outboxMessage.first()
                    if (outboxMessageSnapshot == null) return@run null
                    val formattedDate = formatDate(outboxMessageSnapshot.createdAt.toLocalDateTime(timeZone))
                    val formattedTime = formatTime(outboxMessageSnapshot.createdAt.toLocalDateTime(timeZone))

                    val lifecycleRegistry = LifecycleRegistry()
                    lifecycleRegistry.start()
                    get<OutboxElementHolderViewModelFactory>().create(
                        viewModelContext = childContextWithOwnLifecycle(lifecycleRegistry),
                        key = transactionId.asKey(roomId),
                        outboxMessageFlow = outboxMessage,
                        roomId = roomId,
                        transactionId = transactionId,
                        formattedDate = formattedDate,
                        formattedTime = formattedTime,
                        onOpenMention = onOpenMention,
                    ).also {
                        outboxElementHolderViewModelCache[transactionId] = OutboxElementWrapper(
                            transactionId,
                            it,
                            lifecycleRegistry
                        )
                    }
                }
        }.also {
            log.debug { "finished compute outbox" }
        }
    }


    override fun errorDismiss() {
        error.value = null
    }

    private fun onShowAttachmentSendView(file: FileDescriptor) {
        sendAttachmentNavigation.launchPush(coroutineScope, Config.SendAttachmentView(file))
    }

    private fun onShowReportMessageModal(roomId: RoomId, eventId: EventId) = coroutineScope.launch {
        log.debug { "report to message $eventId" }
        reportMessageRouter.showReportMessage(roomId, eventId)
    }

    private fun closeAttachmentSendView() {
        sendAttachmentNavigation.launchPopWhile(coroutineScope) { it !is Config.None }
        jumpToEndOfTimeline()
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
            } else {
                matrixClient.api.room.leaveRoom(roomId).fold(onSuccess = {
                    onBack()
                }, onFailure = {
                    if (it is CancellationException) {
                        return@launch
                    }
                    log.error(it) { "cannot leave room $roomId" }
                    val groupOrChat =
                        if (isDirect.value) i18n.eventChangeChatGenitive()
                        else i18n.eventChangeGroupGenitive()
                    error.value = i18n.timelineLeaveRoomError(groupOrChat)
                })
            }
        }
    }

    override suspend fun loadBefore() {
        timeline.value?.loadBefore()
    }

    override suspend fun loadAfter() {
        timeline.value?.loadAfter()
    }

    private fun continuouslyLoadBefore() {
        coroutineScope.launch {
            // only start when a view state is set
            viewState.filterNotNull().first()
            timeline.filterNotNull().collectLatest { timeline ->
                combine(
                    elements,
                    viewState.map { it?.firstVisibleElement }.distinctUntilChanged()
                ) { timelineElementViewModels, firstVisibleTimelineElement ->
                    log.trace { "continuouslyLoadBefore (check) : ${timelineElementViewModels.map { it.key }}, firstVisible: $firstVisibleTimelineElement" }
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

    private fun continuouslyLoadAfter() {
        coroutineScope.launch {
            // only start when a view state is set
            viewState.filterNotNull().first()
            timeline.filterNotNull().collectLatest { timeline ->
                combine(
                    elements,
                    viewState.map { it?.lastVisibleElement }.distinctUntilChanged(),
                ) { changedTimelineElementViewModels, changedLastVisibleTimelineElement ->
                    changedTimelineElementViewModels to changedLastVisibleTimelineElement
                }.collectLatest { (timelineElementViewModels, lastVisibleTimelineElement) ->
                    log.debug { "continuouslyLoadAfter (check) : ${timelineElementViewModels.map { it.key }}, lastVisible: $lastVisibleTimelineElement" }
                    val indexOfLastVisibleTimelineElement =
                        timelineElementViewModels.indexOfFirst { it.key == lastVisibleTimelineElement }
                    if (indexOfLastVisibleTimelineElement >= 0 &&
                        indexOfLastVisibleTimelineElement > (timelineElementViewModels.size - 10)
                    ) {
                        val lastEventIdBeforeChange =
                            matrixClient.room.getById(roomId).map { it?.lastEventId }
                                .first()

                        // IMPORTANT: when we are at the end of the timeline, this suspends and waits for new messages
                        log.debug { "load more timeline events after" }
                        val timelineStateChange = timeline.loadAfter()
                        log.debug { "finished load more timeline events after" }

                        if (timelineStateChange.newElements.isNotEmpty()
                            && viewState.value?.windowIsFocused == true
                            && timelineStateChange.elementsBeforeChange.endOfTimelineVisible(lastEventIdBeforeChange)
                        ) {
                            val newLastEvent = timelineStateChange.newElements.last().key
                            val lastVisibleTimelineEvent = lastVisibleTimelineElement?.let {
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
                }
            }
        }
    }

    override suspend fun dropBefore(key: String) {
        val element = timelineElements.value.find { it.key == key }
        if (element != null) {
            timeline.value?.dropBefore(element.roomId, element.eventId)?.also { change ->
                change.removedElements.forEach { it.lifecycle.destroy() }
            }
        }
    }

    override suspend fun dropAfter(key: String) {
        val element = timelineElements.value.find { it.key == key }
        if (element != null) {
            timeline.value?.dropAfter(element.roomId, element.eventId)?.also { change ->
                change.removedElements.forEach { it.lifecycle.destroy() }
            }
        }
    }

    private fun continuouslyDropBefore() {
        coroutineScope.launch {
            timeline.filterNotNull().collectLatest { timeline ->
                combine(
                    timelineElements,
                    viewState.map { it?.firstVisibleElement }.distinctUntilChanged(),
                ) { elements, firstVisibleTimelineElement ->
                    val indexOfFirstVisibleTimelineElement =
                        elements.indexOfFirst { it.key == firstVisibleTimelineElement }
                    log.debug { "dropBefore (check): indexOfFirstVisibleTimelineElement: $indexOfFirstVisibleTimelineElement" }
                    if (indexOfFirstVisibleTimelineElement > 100) {
                        val dropBeforeElement = elements[indexOfFirstVisibleTimelineElement - 20]
                        val change = timeline.dropBefore(
                            dropBeforeElement.roomId,
                            dropBeforeElement.eventId,
                        )
                        change.removedElements.forEach { it.lifecycle.destroy() }
                    }
                }.collect()
            }
        }
    }

    private fun continuouslyDropAfter() {
        coroutineScope.launch {
            timeline.filterNotNull().collectLatest { timeline ->
                combine(
                    timelineElements,
                    viewState.map { it?.lastVisibleElement }.distinctUntilChanged(),
                ) { elements, lastVisibleTimelineElement ->
                    val indexOfLastVisibleTimelineElement =
                        elements.indexOfFirst { it.key == lastVisibleTimelineElement }
                    log.debug { "dropAfter (check): indexOfLastVisibleTimelineElement: $indexOfLastVisibleTimelineElement, allSize=${elements.size}" }
                    if (indexOfLastVisibleTimelineElement >= 0 &&
                        indexOfLastVisibleTimelineElement < (elements.size - 100)
                    ) {
                        val dropAfterElement = elements[indexOfLastVisibleTimelineElement + 20]
                        val change = timeline.dropAfter(
                            dropAfterElement.roomId,
                            dropAfterElement.eventId,
                        )
                        change.removedElements.forEach { it.lifecycle.destroy() }
                    }
                }.collect()
            }
        }
    }

    private suspend fun List<TimelineElementWrapper>.endOfTimelineVisible(lastEventId: EventId?): Boolean {
        val timelineElementKeys = map { it.key }
        val lastVisibleTimelineElementKey = viewState.value?.lastVisibleElement
        val lastEventKey = lastEventId?.let {
            matrixClient.room.getTimelineEvent(roomId, it)
                .first().let { it?.event?.unsigned?.transactionId?.asKey(it.roomId) }
                ?: it.asKey(roomId)
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
                matrixClient.room.getById(roomId).map { it?.lastEventId }.filterNotNull()
                    .first()
            log.debug { "jump to end of timeline to $lastEventId" }
            timelineStartFrom.emit(lastEventId)
            val lastEventKey =
                matrixClient.room.getTimelineEvent(roomId, lastEventId).filterNotNull()
                    .first()
                    .run { event.unsigned?.transactionId?.asKey(event.roomId) ?: eventId.asKey(event.roomId) }
            scheduleScrollTo(lastEventKey)
        }
    }

    override suspend fun markAsRead(key: String) {
        val alreadyReadUntil = readEvent.value
        val requestedNextReadUntil = elements.value.findLastWithEventId(key) ?: return
        val eventId =
            if (alreadyReadUntil != null) {
                val timelineEvents = timelineEvents.value
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
            roomId = roomId,
            read = if (readMarkerIsPublic) eventId else null,
            privateRead = eventId,
        ).onFailure { log.error(it) { "cannot set read marker for event $eventId" } }
            .onSuccess { log.debug { "successfully set read marker for message: $eventId" } }
    }

    override fun markAsFullyRead() {
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
                        matrixClient.api.room.setReadMarkers(roomId, fullyRead = readUntil)
                            .onFailure { log.error(it) { "cannot set message as fully read: $readUntil" } }
                            .onSuccess { log.debug { "set message as fully read: $readUntil" } }
                    } else {
                        if (readUntil == null) log.warn { "cannot mark message as read, since readUntil == null" }
                    }
                }
            }
        } catch (_: CancellationException) {
            log.debug { "mark as fully read has been cancelled before completing" }
        }
    }

    private val getReceiptsByEventCache = concurrentMutableMap<RoomId, Flow<Map<EventId, Set<UserId>>>>()
    private fun getReceipts(roomId: RoomId): Flow<Map<EventId, Set<UserId>>> =
        flow {
            emitAll(
                getReceiptsByEventCache.read { get(roomId) }
                    ?: getReceiptsByEventCache.write {
                        getOrPut(roomId) {
                            matrixClient.user.getAllReceipts(roomId)
                                .flattenNotNull()
                                .map { receipts ->
                                    receipts
                                        .mapNotNull { (key, value) ->
                                            if (key == userId) null
                                            else value.receipts[Read]
                                                ?.let { it.eventId to key }
                                        }
                                        .groupBy { it.first }
                                        .mapValues { it.value.map { it.second }.toSet() }
                                }.distinctUntilChanged()
                                .stateIn(coroutineScope, WhileSubscribed(), emptyMap())
                        }
                    }
            )
        }

    private fun onVerifyUser() {
        coroutineScope.launch {
            log.debug { "try to create new user verification" }
            val isDirectRoom = matrixClient.room.getById(roomId).first()?.isDirect ?: false
            log.debug { "is direct room: $isDirectRoom" }
            directRoom.getUsers(matrixClient, roomId).first().firstOrNull()
                ?.let { otherUserId ->
                    log.debug { "create new user verification with user $otherUserId" }
                    matrixClient.verification.createUserVerificationRequest(otherUserId)
                }
        }
    }

    private fun scheduleScrollTo(key: String) {
        coroutineScope.launch {
            val result = withTimeoutOrNull(2.seconds) {
                elements.first { vms -> vms.any { it.key == key } }
                log.debug { "scheduled scroll to $key" }
                scrollTo.emit(key)
            }
            if (result == null) log.warn { "could not scroll to $key, because view model does not exist" }
        }
    }

    private fun List<BaseTimelineElementHolderViewModel>.findLastWithEventId(key: String) =
        takeWhileInclusive { it.key != key }
            .reversed()
            .firstNotNullOfOrNull { if (it is TimelineElementHolderViewModel) it.eventId else null }

    private fun EventId.asKey(roomId: RoomId? = null) = (roomId ?: this@TimelineViewModelImpl.roomId).full + "-" + full
    private fun String.asKey(roomId: RoomId? = null) = (roomId ?: this@TimelineViewModelImpl.roomId).full + "-" + this
}

class PreviewTimelineViewModel : TimelineViewModel {
    override val elements: MutableStateFlow<List<BaseTimelineElementHolderViewModel>> =
        MutableStateFlow(
            listOf(
                PreviewTimelineElementViewModel1(),
                PreviewTimelineElementViewModel2(),
            )
        )
    override val viewState: MutableStateFlow<TimelineViewModel.ViewState?> = MutableStateFlow(null)
    override val scrollTo: Flow<String> = MutableSharedFlow()
    override val isDirect: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val roomHeaderViewModel: RoomHeaderViewModel = PreviewRoomHeaderViewModel()
    override val inputAreaViewModel: InputAreaViewModel = PreviewInputAreaViewModel()
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
    override val draggedFile: MutableStateFlow<FileDescriptor?> = MutableStateFlow(null)

    init {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            delay(3.seconds)
            elements.value = listOf(
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

    override suspend fun loadBefore() {
    }

    override suspend fun loadAfter() {
    }

    override suspend fun dropBefore(key: String) {
    }

    override suspend fun dropAfter(key: String) {
    }

    override suspend fun markAsRead(key: String) {
    }

    override fun markAsFullyRead() {
    }
}
