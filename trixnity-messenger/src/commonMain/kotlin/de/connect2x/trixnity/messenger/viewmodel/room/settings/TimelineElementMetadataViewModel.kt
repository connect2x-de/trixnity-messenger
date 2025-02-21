package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.util.byEventId
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getTimelineEventReplaceAggregation
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.utils.concurrentMutableMap
import org.koin.core.component.get

private val log = KotlinLogging.logger {}

interface TimelineElementMetadataViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        eventId: EventId,
        roomId: RoomId,
        onOpenUserProfile: (UserId) -> Unit,
        onBack: () -> Unit,
    ): TimelineElementMetadataViewModel =
        TimelineElementMetadataViewModelImpl(
            viewModelContext = viewModelContext,
            eventId = eventId,
            roomId = roomId,
            onOpenUserProfile = onOpenUserProfile,
            onBack = onBack,
        )

    companion object : TimelineElementMetadataViewModelFactory
}

interface TimelineElementMetadataViewModel {
    val elementHistory: StateFlow<List<TimelineElementHolderViewModel>?>
    val element: StateFlow<TimelineElementHolderViewModel?>
    fun openUserProfile(userId: UserId)
    fun back()
}

class TimelineElementMetadataViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    eventId: EventId,
    roomId: RoomId,
    private val onOpenUserProfile: (UserId) -> Unit,
    private val onBack: () -> Unit,
) : TimelineElementMetadataViewModel, MatrixClientViewModelContext by viewModelContext {
    private val timeZone = get<TimeZone>()

    private val backCallback = BackCallback {
        onBack()
    }

    init {
        backHandler.register(backCallback)
    }

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

    private val timelineEventFlow: SharedFlow<TimelineEvent> =
        matrixClient.room.getTimelineEvent(roomId, eventId)
            .filterNotNull()
            .shareIn(coroutineScope, WhileSubscribed(), replay = 1)

    private val timelineElementHolderViewModelFactory = get<TimelineElementHolderViewModelFactory>()
    override val element: StateFlow<TimelineElementHolderViewModel?> =
        flow {
            val timelineEvent = timelineEventFlow.first()
            log.trace { "generate timeline element $eventId" }
            emit(
                timelineElementHolderViewModelFactory.create(
                    viewModelContext = childContext("element-original"),
                    key = "element-original",
                    timelineEventFlow = timelineEventFlow,
                    roomId = roomId,
                    eventId = eventId,
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
                    getReceipts = ::getReceipts,
                    onMessageReplace = { _, _ -> },
                    onMessageReply = { _, _ -> },
                    onMessageReport = { _, _ -> },
                    onOpenMention = { _, _ -> },
                    onOpenMetadata = {},
                )
            )
        }.stateIn(coroutineScope, Lazily, null) // only calculate once!

    private data class TimelineElementHolderViewModelWrapper(
        val viewModel: TimelineElementHolderViewModel,
        val lifecycle: LifecycleRegistry,
    )

    private val elementHistoryCache = MutableStateFlow<Map<EventId, TimelineElementHolderViewModelWrapper>>(emptyMap())
    override val elementHistory: StateFlow<List<TimelineElementHolderViewModel>?> =
        matrixClient.room.getTimelineEventReplaceAggregation(roomId, eventId)
            .map { replaceAggregation ->
                val history = replaceAggregation.history.dropLast(1)
                (elementHistoryCache.value - history.toSet()).forEach { (_, wrapper) ->
                    wrapper.lifecycle.destroy()
                }
                history.map historyMap@{ historyEventId ->
                    val elementHistoryCacheValue = elementHistoryCache.value[historyEventId]
                    if (elementHistoryCacheValue != null) return@historyMap elementHistoryCacheValue.viewModel
                    val timelineEventFlow = matrixClient.room.getTimelineEvent(roomId, historyEventId) {
                        allowReplaceContent = false
                    }.filterNotNull()
                    val timelineEvent = timelineEventFlow.first()
                    val lifecycle = LifecycleRegistry()
                    lifecycle.start()
                    timelineElementHolderViewModelFactory.create(
                        viewModelContext = childContextWithOwnLifecycle(lifecycle),
                        key = "element-history-${historyEventId.full}",
                        timelineEventFlow = timelineEventFlow,
                        roomId = roomId,
                        eventId = historyEventId,
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
                        getReceipts = ::getReceipts,
                        onMessageReplace = { _, _ -> },
                        onMessageReply = { _, _ -> },
                        onMessageReport = { _, _ -> },
                        onOpenMention = { _, _ -> },
                        onOpenMetadata = {},
                    ).also { viewModel ->
                        elementHistoryCache.update {
                            it + (historyEventId to TimelineElementHolderViewModelWrapper(viewModel, lifecycle))
                        }
                    }
                }
            }.stateIn(coroutineScope, WhileSubscribed(), null)

    override fun back() {
        onBack()
    }

    override fun openUserProfile(userId: UserId) {
        onOpenUserProfile(userId)
    }
}
