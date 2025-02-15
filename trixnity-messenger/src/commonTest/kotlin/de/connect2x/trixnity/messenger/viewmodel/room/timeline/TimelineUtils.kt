package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import de.connect2x.trixnity.messenger.util.ReactionKey
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.withClue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Instant
import kotlinx.datetime.Instant.Companion.fromEpochMilliseconds
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.folivo.trixnity.client.room.GetTimelineEventConfig
import net.folivo.trixnity.client.room.GetTimelineEventsConfig
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.room.Timeline
import net.folivo.trixnity.client.room.TimelineBase
import net.folivo.trixnity.client.room.TimelineState
import net.folivo.trixnity.client.room.TimelineStateChange
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.RoomUserReceipts
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.TimelineEventRelation
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.roomId
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.RedactedEventContent
import net.folivo.trixnity.core.model.events.RoomEventContent
import net.folivo.trixnity.core.model.events.StateEventContent
import net.folivo.trixnity.core.model.events.UnknownEventContent
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData
import net.folivo.trixnity.core.model.events.m.FullyReadEventContent
import net.folivo.trixnity.core.model.events.m.ReactionEventContent
import net.folivo.trixnity.core.model.events.m.ReceiptEventContent
import net.folivo.trixnity.core.model.events.m.ReceiptType
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.RelationType
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RedactionEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


private val log = KotlinLogging.logger {}

fun roomUsers(
    userService: UserService,
    roomId: RoomId,
    block: RoomUserBuilder.() -> Unit,
): RoomUserBuilder =
    RoomUserBuilder(userService, roomId).apply(block)

class RoomUserBuilder(
    private val userService: UserService,
    private val roomId: RoomId,
) {
    data class RoomUserWithReceipts(
        val user: RoomUser,
        val receipts: RoomUserReceipts,
    )

    val users = MutableStateFlow(listOf<RoomUserWithReceipts>())

    init {
        updateMocks()
    }

    private fun updateMocks() {
        every { userService.getAll(roomId) } calls {
            log.debug { "userService.getAll($roomId)" }
            users.map {
                it.associate { (user, _) ->
                    user.userId to flowOf(
                        user
                    )
                }
            }
        }
        every { userService.getAllReceipts(roomId) } calls {
            log.debug { "userService.getAllReceipts($roomId)" }
            users.map {
                it.associate { (_, receipts) ->
                    receipts.userId to flowOf(receipts)
                }
            }
        }
    }

    fun addOrUpdateUsers(block: RoomUserBuilder.() -> Unit) {
        this.apply(block)
    }

    suspend fun addOrUpdateUsersSubsequently(vararg blocks: RoomUserBuilder.() -> Unit) {
        blocks.forEach { block ->
            this.apply(block)
            delay(100.milliseconds)
        }
    }

    operator fun RoomUserWithReceipts.unaryPlus() {
        every { userService.getById(roomId, this@unaryPlus.user.userId) } calls {
            log.debug { "userService.getById($roomId, ${this@unaryPlus.user.userId})" }
            flowOf(this@unaryPlus.user)
        }
        users.update {
            it.filter { existingUser -> existingUser.user.userId != this.user.userId } + this
        }
    }

    fun roomUser(
        name: String,
        id: UserId = UserId(name),
        lastReadMessage: EventId? = null,
    ): RoomUserWithReceipts =
        RoomUserWithReceipts(
            RoomUser(
                roomId,
                id,
                name,
                StateEvent(
                    MemberEventContent(membership = Membership.JOIN),
                    EventId("$name-join"),
                    id,
                    roomId,
                    0L,
                    stateKey = "",
                ),
            ),
            RoomUserReceipts(
                roomId, id,
                lastReadMessage?.let {
                    mapOf<ReceiptType, RoomUserReceipts.Receipt>(
                        ReceiptType.Read to RoomUserReceipts.Receipt(it, ReceiptEventContent.Receipt(24))
                    )
                }.orEmpty()
            ),
        )
}

fun timeline(
    roomServiceMock: RoomService,
    roomId: RoomId,
    pageSize: Int = 20,
    block: TimelineBuilder.() -> Unit,
): TimelineMock {
    val fullyReadMock = every {
        roomServiceMock.getAccountData(roomId, FullyReadEventContent::class)
    }
    val fullyReadEventIndex = MutableStateFlow<Int?>(null)
    fullyReadMock returns fullyReadEventIndex
        .map { it?.let { FullyReadEventContent(EventId("$it")) } }

    val room = MutableStateFlow(Room(roomId))
    every { roomServiceMock.getById(roomId) } returns room

    val timelineMock = TimelineMock(room, fullyReadEventIndex, roomServiceMock).apply { addEvents(block) }
    every { roomServiceMock.getLastTimelineEvent(roomId, any()) } returns
            timelineMock.eventsInStore.map { it.lastOrNull() }
    every { roomServiceMock.getLastTimelineEvents(roomId, any()) } returns
            timelineMock.eventsInStore.map { it.reversed().asFlow() }
    every {
        roomServiceMock.getTimeline(
            eq(roomId),
            any<suspend (TimelineStateChange<TimelineViewModelImpl.TimelineElementWrapper>) -> Unit>(),
            any<suspend (Flow<TimelineEvent>) -> TimelineViewModelImpl.TimelineElementWrapper>()
        )
    } calls {
        @Suppress("UNCHECKED_CAST")
        MockedTimeline(
            pageSize,
            timelineMock,
            it.args[2] as (suspend (Flow<TimelineEvent>) -> TimelineViewModelImpl.TimelineElementWrapper)
        )
    }

    return timelineMock
}

class TimelineMock(
    val room: MutableStateFlow<Room>,
    val fullyReadEventIndex: MutableStateFlow<Int?>,
    roomServiceMock: RoomService,
) {
    private val timelineBuilder = TimelineBuilder(room, roomServiceMock)
    val eventsInStore: MutableStateFlow<List<StateFlow<TimelineEvent>>> = MutableStateFlow(listOf())
    val loadBeforeCalledCount = MutableStateFlow(0)
    val loadAfterCalledCount = MutableStateFlow(0)
    fun addEvents(block: TimelineBuilder.() -> Unit) {
        eventsInStore.value = timelineBuilder.apply(block).build()
    }

    suspend fun addEventsSubsequently(vararg blocks: TimelineBuilder.() -> Unit) {
        blocks.forEach { block ->
            addEvents(block)
            delay(100.milliseconds)
        }
    }
}

internal class MockedTimeline(
    private val pageSize: Int,
    private val timelineMock: TimelineMock,
    transformer: suspend (Flow<TimelineEvent>) -> TimelineViewModelImpl.TimelineElementWrapper
) : TimelineBase<TimelineViewModelImpl.TimelineElementWrapper>({}, transformer) {
    private val eventsInStore = timelineMock.eventsInStore

    override suspend fun Flow<TimelineEvent>.canLoadBefore(): Flow<Boolean> = flowOf(true)
    override suspend fun Flow<TimelineEvent>.canLoadAfter(): Flow<Boolean> = flowOf(true)

    override val state = combine(super.state, eventsInStore) { state, allEvents ->
        state.copy(
            canLoadBefore = allEvents.indexOfFirst { it.value.eventId == state.elements.firstOrNull()?.timelineEvent?.first()?.eventId } > 0,
            canLoadAfter = allEvents.indexOfLast { it.value.eventId == state.elements.lastOrNull()?.timelineEvent?.first()?.eventId } < (allEvents.size - 1),
        )
    }

    override suspend fun internalInit(
        startFrom: EventId,
        configStart: GetTimelineEventConfig.() -> Unit,
        configBefore: GetTimelineEventsConfig.() -> Unit,
        configAfter: GetTimelineEventsConfig.() -> Unit
    ): List<Flow<TimelineEvent>> {
        val events = eventsInStore.value
        val startEvent = events.firstOrNull { it.value.eventId == startFrom }
            ?: throw IllegalArgumentException("startFrom=$startFrom could not be found in ${eventsInStore.value.map { it.value.eventId }}")
        val indexOfStartEvent = events.indexOf(startEvent)
        return if (events.size > 1) {
            val eventsBefore = events.take(indexOfStartEvent).takeLast(pageSize / 2)
            val eventsAfter = events.drop(indexOfStartEvent + 1).take(pageSize / 2)
            eventsBefore + startEvent + eventsAfter
        } else listOf(startEvent)
    }

    override suspend fun internalLoadBefore(
        startFrom: EventId,
        config: GetTimelineEventsConfig.() -> Unit
    ): List<Flow<TimelineEvent>> {
        timelineMock.loadBeforeCalledCount.value++
        val events = eventsInStore.value
        val indexOfStartEvent = events.indexOfFirst { it.value.eventId == startFrom }
        return events.take(indexOfStartEvent).takeLast(pageSize)
    }

    override suspend fun internalLoadAfter(
        startFrom: EventId,
        config: GetTimelineEventsConfig.() -> Unit
    ): List<Flow<TimelineEvent>> {
        timelineMock.loadAfterCalledCount.value++
        return eventsInStore.map { events ->
            val indexOfStartEvent = events.indexOfFirst { it.value.eventId == startFrom }
            events.drop(indexOfStartEvent + 1).take(pageSize)
        }.first { it.isNotEmpty() }
    }
}

class NoOpTimeline<T> : Timeline<T> {
    override val state: Flow<TimelineState<T>> = flowOf(TimelineState())

    override suspend fun init(
        startFrom: EventId,
        configStart: GetTimelineEventConfig.() -> Unit,
        configBefore: GetTimelineEventsConfig.() -> Unit,
        configAfter: GetTimelineEventsConfig.() -> Unit
    ): TimelineStateChange<T> = TimelineStateChange()

    override suspend fun loadBefore(config: GetTimelineEventsConfig.() -> Unit): TimelineStateChange<T> =
        TimelineStateChange()

    override suspend fun loadAfter(config: GetTimelineEventsConfig.() -> Unit): TimelineStateChange<T> =
        TimelineStateChange()

    override suspend fun dropBefore(roomId: RoomId, eventId: EventId): TimelineStateChange<T> =
        TimelineStateChange()

    override suspend fun dropAfter(roomId: RoomId, eventId: EventId): TimelineStateChange<T> =
        TimelineStateChange()
}

class TimelineBuilder(
    private val room: MutableStateFlow<Room>,
    private val roomServiceMock: RoomService,
) {
    private val roomId = room.value.roomId
    private val timelineEvents: MutableStateFlow<List<MutableStateFlow<TimelineEvent>>> = MutableStateFlow(listOf())

    fun build(): List<StateFlow<TimelineEvent>> = timelineEvents.value

    private var idCounter: Int = 0
    private var timeCounter: Long = 1

    operator fun TimelineEvent.unaryPlus(): MutableStateFlow<TimelineEvent> {
        val previousTimelineEvent = timelineEvents.value.lastOrNull()
        val newTimelineEvent = MutableStateFlow(this.copy(previousEventId = previousTimelineEvent?.value?.eventId))
        every {
            roomServiceMock.getTimelineEvent(roomId, eventId, any())
        } returns newTimelineEvent
        every {
            roomServiceMock.getTimelineEvents(roomId, eventId, GetEvents.Direction.FORWARDS, any())
        } returns channelFlow {
            val alreadyEmittedEvents = mutableSetOf<EventId>()
            timelineEvents.collectLatest { eventFlows ->
                eventFlows.dropWhile { it.value.eventId != eventId }
                    .filterNot { alreadyEmittedEvents.contains(it.value.eventId) }
                    .forEach {
                        alreadyEmittedEvents.add(it.value.eventId)
                        send(it)
                    }
            }
        }
        every {
            roomServiceMock.getTimelineEvents(roomId, eventId, GetEvents.Direction.BACKWARDS, any())
        } returns channelFlow {
            val alreadyEmittedEvents = mutableSetOf<EventId>()
            timelineEvents.collectLatest { eventFlows ->
                eventFlows.reversed()
                    .dropWhile { it.value.eventId != eventId }
                    .filterNot { alreadyEmittedEvents.contains(it.value.eventId) }
                    .forEach {
                        alreadyEmittedEvents.add(it.value.eventId)
                        send(it)
                    }
            }
        }
        every {
            roomServiceMock.getTimelineEventRelations(roomId, eventId, eq(RelationType.Annotation))
        } returns channelFlow {
            log.debug { "roomServiceMock.getTimelineEventRelations($roomId, $eventId, RelationType.Annotation)" }
            timelineEvents.collectLatest { eventFlows ->
                val reactionFlows = eventFlows
                    .filter {
                        it.value.content?.fold({ it }, { it }).let { content ->
                            content is ReactionEventContent
                                    && content.relatesTo != null
                                    && content.relatesTo?.eventId == eventId
                        }
                    }
                combine(reactionFlows) {
                    val reactions = it.map { event ->
                        event.content?.fold({ it }, { it }).let { content ->
                            if (content !is ReactionEventContent) return@let null
                            content.relatesTo?.let { relatesTo ->
                                Pair(
                                    event.eventId, flowOf(
                                        TimelineEventRelation(
                                            roomId = event.roomId,
                                            eventId = event.eventId,
                                            relationType = relatesTo.relationType,
                                            relatedEventId = relatesTo.eventId,
                                        )
                                    )
                                )
                            }
                        }
                    }.filterNotNull().toMap()
                    send(reactions)
                }
                    .firstOrNull()
                    ?: send(null)
            }
        }
        every {
            roomServiceMock.getTimelineEventRelations(roomId, eventId, eq(RelationType.Replace))
        } returns channelFlow {
            send(null) // TODO: Return message edit relations.
        }
        previousTimelineEvent?.update {
            it.copy(nextEventId = eventId)
        }
        room.update { it.copy(lastEventId = eventId, lastRelevantEventId = eventId) }
        timelineEvents.value += newTimelineEvent
        return newTimelineEvent
    }

    operator fun RoomEvent<*>.unaryPlus(): MutableStateFlow<TimelineEvent> =
        +TimelineEvent(
            event = this,
            previousEventId = null,
            nextEventId = null,
            gap = null,
        )

    infix fun MutableStateFlow<TimelineEvent>.withContent(content: Result<RoomEventContent>) {
        update { it.copy(content = content) }
    }

    fun messageEvent(
        sender: UserId,
        eventId: EventId? = null,
        sentAt: Instant = fromEpochMilliseconds(timeCounter++ * 1000),
        transactionId: String? = null,
        block: MessageEventBuilder.() -> Unit
    ): MessageEvent<*> = messageEvent(
        sender, eventId ?: EventId("${idCounter++}"),
        roomId, sentAt, transactionId, block,
    )

    fun stateEvent(
        sender: UserId,
        sentAt: Instant = fromEpochMilliseconds(0),
        block: StateEventBuilder.() -> Unit
    ): StateEvent<*> =
        stateEvent(sender, EventId("${idCounter++}"), roomId, sentAt, block)
}

fun messageEvent(
    sender: UserId,
    eventId: EventId,
    roomId: RoomId,
    sentAt: Instant = fromEpochMilliseconds(0),
    transactionId: String? = null,
    block: MessageEventBuilder.() -> Unit,
): MessageEvent<*> = MessageEventBuilder()
    .apply(block).content?.let { content ->
        MessageEvent(
            content = content,
            id = eventId,
            sender = sender,
            roomId = roomId,
            originTimestamp = sentAt.toEpochMilliseconds(),
            unsigned = transactionId?.let {
                UnsignedRoomEventData.UnsignedMessageEventData(
                    transactionId = it,
                )
            },
        )
    }.let { result ->
        checkNotNull(result)
    }

class MessageEventBuilder {
    var content: MessageEventContent? = null

    fun text(message: String) = RoomMessageEventContent.TextBased.Text(message)
        .also { content = it }

    fun reaction(relatesTo: EventId, reactionKey: ReactionKey) = ReactionEventContent(
        relatesTo = RelatesTo.Annotation(
            eventId = relatesTo,
            key = reactionKey,
        )
    ).also { content = it }

    fun redacted() = RedactedEventContent(eventType = "m.room.encrypted")
        .also { content = it }

    fun redact(redacts: EventId) = RedactionEventContent(redacts = redacts)
        .also { content = it }

    fun encrypted() = EncryptedMessageEventContent.MegolmEncryptedMessageEventContent(
        ciphertext = "",
        sessionId = "",
    ).also { content = it }
}

fun stateEvent(
    sender: UserId,
    eventId: EventId,
    roomId: RoomId,
    sentAt: Instant = fromEpochMilliseconds(0),
    block: StateEventBuilder.() -> Unit
): StateEvent<*> {
    val content = StateEventBuilder().apply(block).content
    val result = content?.let {
        StateEvent(
            content = content,
            id = eventId,
            sender = sender,
            roomId,
            originTimestamp = sentAt.toEpochMilliseconds(),
            stateKey = "",
        )
    }
    return checkNotNull(result)
}

class StateEventBuilder {
    var content: StateEventContent? = null
    fun createEvent(): CreateEventContent {
        val result = CreateEventContent(
            creator = UserId("Creator"),
        )
        content = result
        return result
    }

    fun unknownEvent(): UnknownEventContent {
        val result = UnknownEventContent(
            raw = JsonObject(mapOf("unknown" to JsonPrimitive("dino"))),
            eventType = "this_is_clearly_unknown"
        )
        content = result
        return result
    }
}

suspend infix fun StateFlow<List<BaseTimelineElementHolderViewModel>>.waitForSize(size: Int) =
    withClue({ "timelineElementViewModels size was ${value.size}, expected $size" }) {
        withTimeout(4.seconds) {
            first { it.size == size }
        }
    }
