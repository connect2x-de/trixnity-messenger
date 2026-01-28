package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import de.connect2x.trixnity.core.model.events.EventType
import de.connect2x.trixnity.core.model.events.UnsignedRoomEventData
import de.connect2x.trixnity.core.model.events.m.room.PowerLevelsEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds

class PowerLevelsTimelineElementViewModelTest {
    private val alice = UserId("alice", "example.com")
    private val testRoom = RoomId("!testRoom")
    private val event = EventId("testEvent")
    private val testEventType = EventType(null, "test")

    private val matrixClient = mock<MatrixClient>()
    private val roomService = mock<RoomService>()
    private val userService = mock<UserService>()

    init {
        every { userService.getById(testRoom, alice) } returns flowOf(null)
        every { matrixClient.di } returns koinApplication {
            modules(module {
                single { roomService }
                single { userService }
            })
        }.koin
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `message even if no difference`() = runTest {
        val pl = PowerLevelsEventContent(ban = 123L)
        val model = testModel(pl, pl)
        backgroundScope.launch { model.changeMessage.collect { } }
        delay(500.milliseconds)
        assertNotNull(model.changeMessage.value)
    }

    @Test
    fun `ban change results in message`() = runTest {
        val model = testModel(
            PowerLevelsEventContent(),
            PowerLevelsEventContent(ban = 123L),
        )
        delay(500.milliseconds)
        assertNotNull(model.changeMessage.value)
    }

    @Test
    fun `invite change results in message`() = runTest {
        val model = testModel(
            PowerLevelsEventContent(),
            PowerLevelsEventContent(invite = 123L),
        )
        delay(500.milliseconds)
        assertNotNull(model.changeMessage.value)
    }

    @Test
    fun `kick change results in message`() = runTest {
        val model = testModel(
            PowerLevelsEventContent(),
            PowerLevelsEventContent(kick = 123L),
        )
        delay(500.milliseconds)
        assertNotNull(model.changeMessage.value)
    }

    @Test
    fun `redact change results in message`() = runTest {
        val model = testModel(
            PowerLevelsEventContent(),
            PowerLevelsEventContent(redact = 123L),
        )
        delay(500.milliseconds)
        assertNotNull(model.changeMessage.value)
    }

    @Test
    fun `eventsDefault =  change results in message`() = runTest {
        val model = testModel(
            PowerLevelsEventContent(),
            PowerLevelsEventContent(eventsDefault = 123L),
        )
        delay(500.milliseconds)
        assertNotNull(model.changeMessage.value)
    }

    @Test
    fun `stateDefault =  change results in message`() = runTest {
        val model = testModel(
            PowerLevelsEventContent(),
            PowerLevelsEventContent(stateDefault = 123L),
        )
        delay(500.milliseconds)
        assertNotNull(model.changeMessage.value)
    }

    @Test
    fun `usersDefault =  change results in message`() = runTest {
        val model = testModel(
            PowerLevelsEventContent(),
            PowerLevelsEventContent(usersDefault = 123L),
        )
        delay(500.milliseconds)
        assertNotNull(model.changeMessage.value)
    }

    @Test
    fun `event removal results in message`() = runTest {
        val model = testModel(
            PowerLevelsEventContent(events = mapOf(testEventType to 42L)),
            PowerLevelsEventContent(),
        )
        delay(500.milliseconds)
        assertNotNull(model.changeMessage.value)
    }

    @Test
    fun `event added results in message`() = runTest {
        val model = testModel(
            PowerLevelsEventContent(),
            PowerLevelsEventContent(events = mapOf(testEventType to 42L)),
        )
        delay(500.milliseconds)
        assertNotNull(model.changeMessage.value)
    }

    @Test
    fun `event changes results in message`() = runTest {
        val model = testModel(
            PowerLevelsEventContent(events = mapOf(testEventType to 0L)),
            PowerLevelsEventContent(events = mapOf(testEventType to 42L)),
        )
        delay(500.milliseconds)
        assertNotNull(model.changeMessage.value)
    }

    @Test
    fun `user removal results in message`() = runTest {
        val model = testModel(
            PowerLevelsEventContent(users = mapOf(alice to 42L)),
            PowerLevelsEventContent(),
        )
        delay(500.milliseconds)
        assertNotNull(model.changeMessage.value)
    }

    @Test
    fun `user added results in message`() = runTest {
        val model = testModel(
            PowerLevelsEventContent(),
            PowerLevelsEventContent(users = mapOf(alice to 42L)),
        )
        delay(500.milliseconds)
        assertNotNull(model.changeMessage.value)
    }

    @Test
    fun `user changes results in message`() = runTest {
        val model = testModel(
            PowerLevelsEventContent(users = mapOf(alice to 0L)),
            PowerLevelsEventContent(users = mapOf(alice to 42L)),
        )
        delay(500.milliseconds)
        assertNotNull(model.changeMessage.value)
    }

    private fun TestScope.testModel(
        previous: PowerLevelsEventContent,
        now: PowerLevelsEventContent,
    ): PowerLevelsTimelineElementViewModel {
        val timelineEvent = TimelineEvent(
            event = StateEvent(
                content = previous,
                id = event,
                sender = alice,
                roomId = testRoom,
                originTimestamp = 123,
                unsigned = UnsignedRoomEventData.UnsignedStateEventData(previousContent = previous),
                stateKey = ""
            ), previousEventId = null, nextEventId = null, gap = null
        )

        every { roomService.getTimelineEvent(testRoom, event) } returns flowOf(timelineEvent)

        val model = PowerLevelsTimelineElementViewModelFactoryImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(createTestDefaultTrixnityMessengerModules(mapOf(alice to matrixClient)))
                }.koin,
                userId = alice,
            ), content = now, roomId = testRoom, eventId = event
        )

        backgroundScope.launch { model.changeMessage.collect { } }

        return model
    }
}
