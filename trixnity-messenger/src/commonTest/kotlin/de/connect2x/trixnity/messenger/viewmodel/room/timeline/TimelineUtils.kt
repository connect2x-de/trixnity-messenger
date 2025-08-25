package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import io.kotest.assertions.withClue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout
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
import net.folivo.trixnity.client.store.eventId
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
import net.folivo.trixnity.core.model.events.m.ReceiptEventContent
import net.folivo.trixnity.core.model.events.m.ReceiptType
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RedactionEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

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
        every { userService.getAll(roomId) } returns users.map {
            it.associate { (user, _) ->
                user.userId to flowOf(
                    user
                )
            }
        }
        every { userService.getAllReceipts(roomId) } returns users.map {
            it.associate { (_, receipts) ->
                receipts.userId to flowOf(
                    receipts
                )
            }
        }
    }

    operator fun RoomUserWithReceipts.unaryPlus() {
        every { userService.getById(roomId, this@unaryPlus.user.userId) } returns flowOf(this.user)
        users.update { it + this }
    }

    fun roomUser(name: String, id: UserId = UserId(name), lastReadMessage: EventId? = null): RoomUserWithReceipts =
        RoomUserWithReceipts(
            RoomUser(
                roomId,
                id,
                name,
                StateEvent(
                    MemberEventContent(membership = Membership.JOIN),
                    EventId(""),
                    id,
                    roomId,
                    0L,
                    stateKey = ""
                ),
            ),
            RoomUserReceipts(
                roomId, id,
                lastReadMessage?.let {
                    mapOf<ReceiptType, RoomUserReceipts.Receipt>(
                        ReceiptType.Read to RoomUserReceipts.Receipt(it, ReceiptEventContent.Receipt(24))
                    )
                }.orEmpty()
            )
        )
}

fun timeline(
    roomServiceMock: RoomService,
    roomId: RoomId,
    pageSize: Int = 20,
    block: TimelineBuilder.(startFrom: EventId) -> Unit,
): TimelineMock {
    val fullyReadMock = every {
        roomServiceMock.getAccountData(roomId, FullyReadEventContent::class)
    }
    val fullyReadEventIndex = MutableStateFlow<Int?>(null)
    fullyReadMock returns fullyReadEventIndex.map { it?.let { FullyReadEventContent(EventId("$it")) } }

    val room = MutableStateFlow(Room(roomId))
    every { roomServiceMock.getById(roomId) } returns room

    val timelineMock = TimelineMock(room, fullyReadEventIndex, roomServiceMock, block)
        .apply { initEvents(EventId("dummy"), block) }
    every { roomServiceMock.getLastTimelineEvent(roomId, any()) } returns
            timelineMock.eventsInStore.map { it.lastOrNull() }
    every { roomServiceMock.getLastTimelineEvents(roomId, any()) } returns
            timelineMock.eventsInStore.map { it.reversed().asFlow() }
    every {
        roomServiceMock.getTimeline(
            any<suspend (TimelineStateChange<TimelineViewModelImpl.TimelineElementWrapper>) -> Unit>(),
            any<suspend (Flow<TimelineEvent>) -> TimelineViewModelImpl.TimelineElementWrapper>()
        )
    } calls {
        @Suppress("UNCHECKED_CAST")
        MockedTimeline(
            pageSize,
            timelineMock,
            it.args[1] as (suspend (Flow<TimelineEvent>) -> TimelineViewModelImpl.TimelineElementWrapper)
        )
    }

    return timelineMock
}

class TimelineMock(
    val room: MutableStateFlow<Room>,
    val fullyReadEventIndex: MutableStateFlow<Int?>,
    private val roomServiceMock: RoomService,
    val initBlock: TimelineBuilder.(startFrom: EventId) -> Unit
) {
    private var timelineBuilder = TimelineBuilder(room, roomServiceMock)
    val eventsInStore: MutableStateFlow<List<StateFlow<TimelineEvent>>> = MutableStateFlow(listOf())
    val loadBeforeCalledCount = MutableStateFlow(0)
    val loadAfterCalledCount = MutableStateFlow(0)

    fun addEvents(block: TimelineBuilder.() -> Unit) {
        eventsInStore.value = timelineBuilder.apply(block).also { it.createMocks() }.build()
    }

    fun initEvents(startFrom: EventId, block: TimelineBuilder.(startFrom: EventId) -> Unit) {
        val timelineBuilder = TimelineBuilder(room, roomServiceMock)
        val newEvents = timelineBuilder.apply { block.invoke(timelineBuilder, startFrom) }.build()
        val eventContentsInStore = eventsInStore.value.map { it.value.content }
        if (newEvents.all { eventFlow -> eventContentsInStore.contains(eventFlow.value.content) }) {
            return
        }

        this.timelineBuilder = timelineBuilder
        this.timelineBuilder.createMocks()
        this.eventsInStore.value = newEvents
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

    @Deprecated("use init with explicit roomId instead")
    override suspend fun init(
        startFrom: EventId,
        configStart: GetTimelineEventConfig.() -> Unit,
        configBefore: GetTimelineEventsConfig.() -> Unit,
        configAfter: GetTimelineEventsConfig.() -> Unit
    ): TimelineStateChange<TimelineViewModelImpl.TimelineElementWrapper> {
        throw NotImplementedError("calling this only works with legacy TimelineImpl")
    }

    override suspend fun internalInit(
        roomId: RoomId,
        startFrom: EventId,
        configStart: GetTimelineEventConfig.() -> Unit,
        configBefore: GetTimelineEventsConfig.() -> Unit,
        configAfter: GetTimelineEventsConfig.() -> Unit
    ): List<Flow<TimelineEvent>> {
        timelineMock.initEvents(startFrom, timelineMock.initBlock)
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
        roomId: RoomId,
        startFrom: EventId,
        config: GetTimelineEventsConfig.() -> Unit
    ): List<Flow<TimelineEvent>> {
        timelineMock.loadBeforeCalledCount.value++
        val events = eventsInStore.value
        val indexOfStartEvent = events.indexOfFirst { it.value.eventId == startFrom }
        return events.take(indexOfStartEvent).takeLast(pageSize)
    }

    override suspend fun internalLoadAfter(
        roomId: RoomId,
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

    @Deprecated("use init with explicit roomId instead")
    override suspend fun init(
        startFrom: EventId,
        configStart: GetTimelineEventConfig.() -> Unit,
        configBefore: GetTimelineEventsConfig.() -> Unit,
        configAfter: GetTimelineEventsConfig.() -> Unit
    ): TimelineStateChange<T> = TimelineStateChange()

    override suspend fun init(
        roomId: RoomId,
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

    fun build(): List<StateFlow<TimelineEvent>> {
        return timelineEvents.value
    }

    fun createMocks() {
        for (newTimelineEvent in timelineEvents.value) {
            val eventId = newTimelineEvent.value.eventId

            every {
                roomServiceMock.getTimelineEvent(roomId, eventId, any())
            } returns newTimelineEvent
            every {
                roomServiceMock.getTimelineEvents(roomId, eventId, GetEvents.Direction.FORWARDS, any())
            } returns channelFlow {
                val alreadyEmittedEvents = mutableSetOf<EventId>()
                timelineEvents.collectLatest {
                    it.dropWhile { it.value.eventId != eventId }
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
                timelineEvents.collectLatest {
                    it.reversed()
                        .dropWhile { it.value.eventId != eventId }
                        .filterNot { alreadyEmittedEvents.contains(it.value.eventId) }
                        .forEach {
                            alreadyEmittedEvents.add(it.value.eventId)
                            send(it)
                        }
                }
            }
        }
    }

    private var idCounter = 0

    operator fun TimelineEvent.unaryPlus(): MutableStateFlow<TimelineEvent> {
        val previousTimelineEvent = timelineEvents.value.lastOrNull()
        val newTimelineEvent = MutableStateFlow(this.copy(previousEventId = previousTimelineEvent?.value?.eventId))

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
            gap = null
        )


    infix fun MutableStateFlow<TimelineEvent>.withContent(content: Result<RoomEventContent>) {
        update { it.copy(content = content) }
    }

    fun messageEvent(
        sender: UserId,
        sentAt: Instant = Instant.fromEpochMilliseconds(0),
        eventId: EventId = EventId("${idCounter++}"),
        transactionId: String? = null,
        block: MessageEventBuilder.() -> Unit
    ): MessageEvent<*> = messageEvent(sender, eventId, roomId, sentAt, transactionId, block)

    fun stateEvent(
        sender: UserId,
        sentAt: Instant = Instant.fromEpochMilliseconds(0),
        block: StateEventBuilder.() -> Unit
    ): StateEvent<*> =
        stateEvent(sender, EventId("${idCounter++}"), roomId, sentAt, block)
}

fun messageEvent(
    sender: UserId,
    eventId: EventId,
    roomId: RoomId,
    sentAt: Instant = Instant.fromEpochMilliseconds(0),
    transactionId: String? = null,
    block: MessageEventBuilder.() -> Unit
): MessageEvent<*> {
    val content = MessageEventBuilder().apply(block).content
    val result = content?.let {
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
    }
    return checkNotNull(result)
}

class MessageEventBuilder {
    var content: MessageEventContent? = null
    fun text(message: String): RoomMessageEventContent.TextBased.Text {
        val result = RoomMessageEventContent.TextBased.Text(message)
        content = result
        return result
    }

    fun answerTo(message: String, eventId: EventId): RoomMessageEventContent.TextBased.Text {
        val result = RoomMessageEventContent.TextBased.Text(
            body = message,
            relatesTo = RelatesTo.Reply(replyTo = RelatesTo.ReplyTo(eventId))
        )
        content = result
        return result
    }

    fun reaction(relatesTo: EventId): UnknownEventContent {
        val result = UnknownEventContent(
            raw = JsonObject(mapOf("m.relates_to" to JsonPrimitive(relatesTo.full))),
            eventType = "m.reaction"
        )
        content = result
        return result
    }

    fun redacted(): RedactedEventContent {
        val result = RedactedEventContent(eventType = "m.room.encrypted")
        content = result
        return result
    }

    fun redact(redacts: EventId): RedactionEventContent {
        val result = RedactionEventContent(redacts = redacts)
        content = result
        return result
    }

    fun encrypted(): EncryptedMessageEventContent {
        val result = EncryptedMessageEventContent.MegolmEncryptedMessageEventContent(
            ciphertext = "",
            sessionId = ""
        )
        content = result
        return result
    }
}

fun stateEvent(
    sender: UserId,
    eventId: EventId,
    roomId: RoomId,
    sentAt: Instant = Instant.fromEpochMilliseconds(0),
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
        val result = CreateEventContent()
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
