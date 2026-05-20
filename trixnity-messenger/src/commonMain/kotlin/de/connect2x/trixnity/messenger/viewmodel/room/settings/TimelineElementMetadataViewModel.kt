package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.getTimelineEventReplaceAggregation
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.store.originTimestamp
import de.connect2x.trixnity.client.store.sender
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.html.HtmlNode
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.PreviewTimelineElementViewModel1
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.EventReaction
import de.connect2x.trixnity.messenger.viewmodel.util.EventReactions
import de.connect2x.trixnity.messenger.viewmodel.util.byEventId
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import de.connect2x.trixnity.messenger.viewmodel.util.previewImageByteArray
import de.connect2x.trixnity.utils.concurrentMutableMap
import kotlin.time.Instant
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.get

interface TimelineElementMetadataViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        eventId: EventId,
        roomId: RoomId,
        onOpenUserProfile: (UserId) -> Unit,
        onOpenDevInfo: () -> Unit,
        onBack: () -> Unit,
    ): TimelineElementMetadataViewModel =
        TimelineElementMetadataViewModelImpl(
            viewModelContext = viewModelContext,
            eventId = eventId,
            roomId = roomId,
            onOpenUserProfile = onOpenUserProfile,
            onOpenDevInfo = onOpenDevInfo,
            onBack = onBack,
        )

    companion object : TimelineElementMetadataViewModelFactory
}

interface TimelineElementMetadataViewModel {
    val elementHistory: StateFlow<List<TimelineElementHolderViewModel>?>
    val element: StateFlow<TimelineElementHolderViewModel?>

    fun openUserProfile(userId: UserId)

    fun openDevInfo()

    fun back()
}

class TimelineElementMetadataViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    eventId: EventId,
    roomId: RoomId,
    private val onOpenUserProfile: (UserId) -> Unit,
    private val onOpenDevInfo: () -> Unit,
    private val onBack: () -> Unit,
) : TimelineElementMetadataViewModel, MatrixClientViewModelContext by viewModelContext {
    private val timeZone = get<TimeZone>()

    private val backCallback = BackCallback { onBack() }

    init {
        registerBackCallback(backCallback)
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

    private val timelineEventFlow: SharedFlow<TimelineEvent> =
        matrixClient.room
            .getTimelineEvent(roomId, eventId)
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
                        formattedDate =
                            formatDate(
                                Instant.fromEpochMilliseconds(timelineEvent.originTimestamp).toLocalDateTime(timeZone)
                            ),
                        formattedTime =
                            formatTime(
                                Instant.fromEpochMilliseconds(timelineEvent.originTimestamp).toLocalDateTime(timeZone)
                            ),
                        showLoadingIndicatorBefore = flowOf(false),
                        showLoadingIndicatorAfter = flowOf(false),
                        showUnreadMarker = flowOf(false),
                        ignoreReplacedEvents = true,
                        getReceipts = ::getReceipts,
                        onMessageReplace = { _, _ -> },
                        onMessageReply = { _, _ -> },
                        onMessageReport = { _, _ -> },
                        onOpenMention = { _, _ -> },
                        onOpenMetadata = {},
                        jumpTo = { _, _ -> },
                    )
                )
            }
            .stateIn(coroutineScope, Lazily, null) // only calculate once!

    private data class TimelineElementHolderViewModelWrapper(
        val viewModel: TimelineElementHolderViewModel,
        val lifecycle: LifecycleRegistry,
    )

    private val elementHistoryCache = MutableStateFlow<Map<EventId, TimelineElementHolderViewModelWrapper>>(emptyMap())
    override val elementHistory: StateFlow<List<TimelineElementHolderViewModel>?> =
        matrixClient.room
            .getTimelineEventReplaceAggregation(roomId, eventId)
            .map { replaceAggregation ->
                val history = listOf(eventId) + replaceAggregation.history
                (elementHistoryCache.value - history.toSet()).forEach { (_, wrapper) -> wrapper.lifecycle.destroy() }
                log.debug { "history: $history" }
                history.map historyMap@{ historyEventId ->
                    val elementHistoryCacheValue = elementHistoryCache.value[historyEventId]
                    if (elementHistoryCacheValue != null) return@historyMap elementHistoryCacheValue.viewModel
                    val timelineEventFlow =
                        matrixClient.room
                            .getTimelineEvent(roomId, historyEventId) { allowReplaceContent = false }
                            .filterNotNull()
                    val timelineEvent = timelineEventFlow.first()
                    val lifecycle = LifecycleRegistry()
                    lifecycle.start()
                    log.debug { "create Holder for: $timelineEvent" }
                    timelineElementHolderViewModelFactory
                        .create(
                            viewModelContext = childContextWithOwnLifecycle(historyEventId.full, lifecycle),
                            key = "element-history-${historyEventId.full}",
                            timelineEventFlow = timelineEventFlow,
                            roomId = roomId,
                            eventId = historyEventId,
                            sender = timelineEvent.sender,
                            formattedDate =
                                formatDate(
                                    Instant.fromEpochMilliseconds(timelineEvent.originTimestamp)
                                        .toLocalDateTime(timeZone)
                                ),
                            formattedTime =
                                formatTime(
                                    Instant.fromEpochMilliseconds(timelineEvent.originTimestamp)
                                        .toLocalDateTime(timeZone)
                                ),
                            showLoadingIndicatorBefore = flowOf(false),
                            showLoadingIndicatorAfter = flowOf(false),
                            showUnreadMarker = flowOf(false),
                            ignoreReplacedEvents = false,
                            getReceipts = ::getReceipts,
                            onMessageReplace = { _, _ -> },
                            onMessageReply = { _, _ -> },
                            onMessageReport = { _, _ -> },
                            onOpenMention = { _, _ -> },
                            onOpenMetadata = {},
                            jumpTo = { _, _ -> },
                        )
                        .also { viewModel ->
                            elementHistoryCache.update {
                                it + (historyEventId to TimelineElementHolderViewModelWrapper(viewModel, lifecycle))
                            }
                        }
                }
            }
            .stateIn(coroutineScope, WhileSubscribed(), null)

    override fun back() {
        onBack()
    }

    override fun openUserProfile(userId: UserId) {
        onOpenUserProfile(userId)
    }

    override fun openDevInfo() {
        onOpenDevInfo()
    }
}

class PreviewTimelineElementMetadataViewModel1 : TimelineElementMetadataViewModel {
    override val elementHistory: StateFlow<List<TimelineElementHolderViewModel>?> =
        MutableStateFlow(
            listOf(
                PreviewTimelineElementViewModel1().apply {
                    element.value =
                        object : RoomMessageTimelineElementViewModel.TextBased.Text {
                            override val body: String = "Edit 1"
                            override val formattedBody: String? = null
                            override val formattedBodyContent: HtmlNode.HtmlElement? = null
                            override val mentionsInBody: Map<IntRange, MutableStateFlow<TimelineElementMention>> =
                                mapOf()
                            override val mentionsInFormattedBody: StateFlow<Map<String, TimelineElementMention?>> =
                                MutableStateFlow(mapOf())

                            override fun openMention(mention: TimelineElementMention) {}
                        }
                    repliedElement.value = null
                },
                PreviewTimelineElementViewModel1().apply {
                    element.value =
                        object : RoomMessageTimelineElementViewModel.TextBased.Text {
                            override val body: String = "Edit 2"
                            override val formattedBody: String? = null
                            override val formattedBodyContent: HtmlNode.HtmlElement? = null
                            override val mentionsInBody: Map<IntRange, MutableStateFlow<TimelineElementMention>> =
                                mapOf()
                            override val mentionsInFormattedBody: StateFlow<Map<String, TimelineElementMention?>> =
                                MutableStateFlow(mapOf())

                            override fun openMention(mention: TimelineElementMention) {}
                        }
                    repliedElement.value = null
                },
                PreviewTimelineElementViewModel1().apply {
                    element.value =
                        object : RoomMessageTimelineElementViewModel.TextBased.Text {
                            override val body: String = "Edit 3"
                            override val formattedBody: String? = null
                            override val formattedBodyContent: HtmlNode.HtmlElement? = null
                            override val mentionsInBody: Map<IntRange, MutableStateFlow<TimelineElementMention>> =
                                mapOf()
                            override val mentionsInFormattedBody: StateFlow<Map<String, TimelineElementMention?>> =
                                MutableStateFlow(mapOf())

                            override fun openMention(mention: TimelineElementMention) {}
                        }
                    repliedElement.value = null
                },
            )
        )
    override val element: StateFlow<TimelineElementHolderViewModel?> =
        MutableStateFlow(
            PreviewTimelineElementViewModel1().apply {
                element.value =
                    object : RoomMessageTimelineElementViewModel.TextBased.Text {
                        override val body: String = "Edit 4"
                        override val formattedBody: String? = null
                        override val formattedBodyContent: HtmlNode.HtmlElement? = null
                        override val mentionsInBody: Map<IntRange, MutableStateFlow<TimelineElementMention>> = mapOf()
                        override val mentionsInFormattedBody: StateFlow<Map<String, TimelineElementMention?>> =
                            MutableStateFlow(mapOf())

                        override fun openMention(mention: TimelineElementMention) {}
                    }
                sender.value =
                    UserInfoElement(
                        userId = UserId("@martin:localhost"),
                        name = "Martin",
                        initials = "M",
                        image = MutableStateFlow(previewImageByteArray()),
                    )
                reactions.value =
                    EventReactions(
                        setOf(
                            EventReaction(
                                "👍",
                                UserInfoElement(
                                    userId = UserId("@martin:localhost"),
                                    name = "Martin",
                                    initials = "M",
                                    image = MutableStateFlow(previewImageByteArray()),
                                ),
                                EventId("r1"),
                                false,
                            ),
                            EventReaction(
                                "😀",
                                UserInfoElement(
                                    userId = UserId("@michael:localhost"),
                                    name = "Michael",
                                    initials = "M",
                                    image = MutableStateFlow(previewImageByteArray()),
                                ),
                                EventId("r2"),
                                false,
                            ),
                        )
                    )
                readers.value =
                    listOf(
                        UserInfoElement(
                            userId = UserId("@benedict:localhost"),
                            name = "Benedict",
                            initials = "B",
                            image = MutableStateFlow(previewImageByteArray()),
                        ),
                        UserInfoElement(
                            userId = UserId("@michael:localhost"),
                            name = "Michael",
                            initials = "M",
                            image = MutableStateFlow(previewImageByteArray()),
                        ),
                    )
            }
        )

    override fun openUserProfile(userId: UserId) {}

    override fun openDevInfo() {}

    override fun back() {}
}
