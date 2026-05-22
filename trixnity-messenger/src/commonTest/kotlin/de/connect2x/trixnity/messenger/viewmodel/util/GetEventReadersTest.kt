package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.store.eventId
import de.connect2x.trixnity.client.store.roomId
import de.connect2x.trixnity.client.store.sender
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import de.connect2x.trixnity.core.model.events.RoomEventContent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.runTestWithCoroutineScope
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.timeline
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class GetEventReadersTest {
    private val cut = GetEventReadersImpl()

    private val roomId = RoomId("!room1")
    private val eventId = EventId("event")

    private val us = UserId("mimi", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()
    val initialsMock = mock<Initials>()

    val timelineEvent =
        TimelineEvent(
            event =
                MessageEvent(
                    TextBased.Text("Hi!"),
                    id = eventId,
                    sender = alice,
                    roomId = roomId,
                    originTimestamp = 123456789L,
                ),
            content = null,
            previousEventId = null,
            nextEventId = null,
            gap = null,
        )

    private val receipts = MutableStateFlow<Map<EventId, Set<UserId>>>(mapOf())

    @BeforeTest
    fun beforeTest() {
        configureTestLogging()
        resetCalls(matrixClientMock, roomServiceMock, userServiceMock, initialsMock)
        every { matrixClientMock.di } returns
            koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                            single { userServiceMock }
                        }
                    )
                }
                .koin
        every { matrixClientMock.userId } returns us
        every { userServiceMock.canSendEvent(roomId, any<KClass<out RoomEventContent>>()) } returns flowOf(true)
        every { userServiceMock.getById(roomId, any()) } calls
            { params ->
                val userId = params.args[1] as UserId
                flowOf(
                    RoomUser(
                        roomId,
                        userId,
                        userId.full,
                        ClientEvent.RoomEvent.StateEvent(
                            MemberEventContent(membership = Membership.JOIN),
                            id = eventId,
                            sender = userId,
                            roomId = roomId,
                            originTimestamp = 0L,
                            stateKey = userId.full,
                        ),
                    )
                )
            }
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())
        every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns emptyFlow()
        every { initialsMock.compute(any()) } returns ""

        receipts.value = mapOf()
    }

    private suspend fun GetEventReaders.isRead(coroutineScope: CoroutineScope): StateFlow<Boolean> =
        isRead(
                matrixClient = matrixClientMock,
                roomId = timelineEvent.roomId,
                eventId = timelineEvent.eventId,
                sender = timelineEvent.sender,
                getReceipts = { receipts },
            )
            .stateIn(coroutineScope)

    private suspend fun GetEventReaders.isReadBy(coroutineScope: CoroutineScope): StateFlow<List<UserInfoElement>?> =
        isReadBy(
                matrixClient = matrixClientMock,
                roomId = timelineEvent.roomId,
                eventId = timelineEvent.eventId,
                sender = timelineEvent.sender,
                getReceipts = { receipts },
                initials = initialsMock,
                maxMediaSizeInMemory = 100L,
            )
            .stateIn(coroutineScope)

    @Test
    fun `isRead - be true when read by third user`() = runTestWithCoroutineScope { coroutineScope ->
        timeline(roomServiceMock, roomId) { +timelineEvent }
        val isRead = cut.isRead(coroutineScope)
        delay(100.milliseconds)
        isRead.value shouldBe false

        receipts.value = mapOf(eventId to setOf(bob))
        delay(100.milliseconds)
        isRead.value shouldBe true
    }

    @Test
    fun `isRead - be true when message from third user after it`() = runTestWithCoroutineScope { coroutineScope ->
        val timeline = timeline(roomServiceMock, roomId) { +timelineEvent }
        val isRead = cut.isRead(coroutineScope)
        delay(100.milliseconds)
        isRead.value shouldBe false

        timeline.addEvents {
            // should be ignored
            +messageEvent(sender = alice) { text("Hi!") }
            +messageEvent(sender = bob) { text("Hi!") }
        }
        delay(100.milliseconds)
        isRead.value shouldBe true
    }

    @Test
    fun `isRead - be false when no on read or sent a message`() = runTestWithCoroutineScope { coroutineScope ->
        timeline(roomServiceMock, roomId) { +timelineEvent }
        val isRead = cut.isRead(coroutineScope)
        delay(100.milliseconds)
        isRead.value shouldBe false
    }

    @Test
    fun `isRead - be false when not read by anyone and only us send message after it`() =
        runTestWithCoroutineScope { coroutineScope ->
            timeline(roomServiceMock, roomId) {
                +timelineEvent
                +messageEvent(sender = us) { text("Hi!") }
            }
            val isRead = cut.isRead(coroutineScope)
            delay(100.milliseconds)
            isRead.value shouldBe false
        }

    @Test
    fun `isRead - be false when not read by anyone and only the same user send message after it`() =
        runTestWithCoroutineScope { coroutineScope ->
            timeline(roomServiceMock, roomId) {
                +timelineEvent
                +messageEvent(sender = alice) { text("Hi!") }
            }
            val isRead = cut.isRead(coroutineScope)
            delay(100.milliseconds)
            isRead.value shouldBe false
        }

    @Test
    fun `isRead - be false when only we read the event`() = runTestWithCoroutineScope { coroutineScope ->
        timeline(roomServiceMock, roomId) { +timelineEvent }
        val isRead = cut.isRead(coroutineScope)
        receipts.value = mapOf(eventId to setOf(us))
        delay(100.milliseconds)
        isRead.value shouldBe false
    }

    @Test
    fun `isRead - be false when only the sender read the event`() = runTestWithCoroutineScope { coroutineScope ->
        timeline(roomServiceMock, roomId) { +timelineEvent }
        val isRead = cut.isRead(coroutineScope)
        receipts.value = mapOf(eventId to setOf(alice))
        delay(100.milliseconds)
        isRead.value shouldBe false
    }

    @Test
    fun `isReadBy - be empty when not read`() = runTestWithCoroutineScope { coroutineScope ->
        timeline(roomServiceMock, roomId) { +timelineEvent }
        val isReadBy = cut.isReadBy(coroutineScope)
        delay(100.milliseconds)
        isReadBy.value shouldBe emptyList()
    }

    @Test
    fun `isReadBy - contain users from read markers`() = runTestWithCoroutineScope { coroutineScope ->
        timeline(roomServiceMock, roomId) { +timelineEvent }
        val isReadBy = cut.isReadBy(coroutineScope)

        delay(100.milliseconds)
        isReadBy.value shouldBe emptyList()

        receipts.value = mapOf(eventId to setOf(bob))
        delay(100.milliseconds)
        isReadBy.value?.map { it.userId } shouldBe listOf(bob)
    }

    @Test
    fun `isReadBy - contain sender of subsequent events`() = runTestWithCoroutineScope { coroutineScope ->
        val timeline = timeline(roomServiceMock, roomId) { +timelineEvent }
        val isReadBy = cut.isReadBy(coroutineScope)
        delay(100.milliseconds)
        isReadBy.value shouldBe emptyList()

        timeline.addEvents { +messageEvent(sender = bob) { text("Hi!") } }
        delay(100.milliseconds)
        isReadBy.value?.map { it.userId } shouldBe listOf(bob)
    }

    @Test
    fun `isReadBy - not contain us from read marker`() = runTestWithCoroutineScope { coroutineScope ->
        timeline(roomServiceMock, roomId) { +timelineEvent }
        val isReadBy = cut.isReadBy(coroutineScope)
        receipts.value = mapOf(eventId to setOf(us, bob))
        delay(100.milliseconds)
        isReadBy.value?.map { it.userId } shouldBe listOf(bob)
    }

    @Test
    fun `isReadBy - not contain sender from read marker`() = runTestWithCoroutineScope { coroutineScope ->
        timeline(roomServiceMock, roomId) { +timelineEvent }
        val isReadBy = cut.isReadBy(coroutineScope)
        receipts.value = mapOf(eventId to setOf(alice, bob))
        delay(100.milliseconds)
        isReadBy.value?.map { it.userId } shouldBe listOf(bob)
    }

    @Test
    fun `isReadBy - not contain us from subsequent events`() = runTestWithCoroutineScope { coroutineScope ->
        timeline(roomServiceMock, roomId) {
            +timelineEvent
            +messageEvent(sender = us) { text("Hi!") }
        }
        val isReadBy = cut.isReadBy(coroutineScope)
        delay(100.milliseconds)
        isReadBy.value shouldBe emptyList()
    }

    @Test
    fun `isReadBy - not contain sender from subsequent events`() = runTestWithCoroutineScope { coroutineScope ->
        timeline(roomServiceMock, roomId) {
            +timelineEvent
            +messageEvent(sender = alice) { text("Hi!") }
        }
        val isReadBy = cut.isReadBy(coroutineScope)
        delay(100.milliseconds)
        isReadBy.value shouldBe emptyList()
    }
}
