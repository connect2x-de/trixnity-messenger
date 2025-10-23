package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.KeyService
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.user.PowerLevel
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.EventType
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds


class PowerlevelViewModelTest {
    private val testRoom = RoomId("!testRoom")
    private val alice = UserId("alice", "example.com")
    private val alicePowerLevel = PowerLevel.User(50)

    private val matrixClient = mock<MatrixClient>()
    private val roomService = mock<RoomService>()
    private val keyService = mock<KeyService>()
    private val userService = mock<UserService>()
    private val matrixClientServerApiClient = mock<MatrixClientServerApiClient>()
    private val roomApiClient = mock<RoomApiClient>()

    init {
        resetMocks(matrixClient, roomService, keyService, userService)

        every {
            roomService.getState(testRoom, PowerLevelsEventContent::class, "")
        } returns MutableStateFlow(
            StateEvent(
                content = PowerLevelsEventContent(),
                id = EventId("eventId"),
                sender = alice,
                roomId = testRoom,
                originTimestamp = 123,
                unsigned = null,
                stateKey = "",
            )
        )

        every { userService.getPowerLevel(testRoom, alice) } returns flowOf(alicePowerLevel)
        every {
            userService.canSendEvent(testRoom, PowerLevelsEventContent::class)
        } returns flowOf(true)

        every { matrixClient.api } returns matrixClientServerApiClient
        every { matrixClientServerApiClient.room } returns roomApiClient

        every { matrixClient.userId } returns alice
        every { matrixClient.di } returns koinApplication {
            modules(module {
                single { roomService }
                single { userService }
                single { keyService }
            })
        }.koin
    }

    @Test
    fun `error if improperly configured`() = runTest {
        val model = testModel()
        model.ban.input.update("notANumber")
        delay(500.milliseconds)
        assertNull(model.error.value)
    }

    @Test
    fun `check whether we can modify events`() = runTest {
        val canModify = EventType(null, "foobar")
        val cannotModify = EventType(null, "baz")
        every {
            roomService.getState(testRoom, PowerLevelsEventContent::class, "")
        } returns MutableStateFlow(
            StateEvent(
                content = PowerLevelsEventContent(events = mapOf(canModify to 30, cannotModify to 60)),
                id = EventId("eventId"),
                sender = alice,
                roomId = testRoom,
                originTimestamp = 123,
                unsigned = null,
                stateKey = "",
            )
        )

        val model = testModel()

        backgroundScope.launch { model.events.collect { } }
        delay(500.milliseconds)

        val events = model.events.value
        assertContains(events, canModify)
        assertContains(events, cannotModify)

        backgroundScope.launch { events[canModify]!!.max.collect { } }
        backgroundScope.launch { events[canModify]!!.max.collect { } }
        delay(500.milliseconds)

        assertNotNull(events[canModify]!!.max.value)
        assertNull(events[cannotModify]!!.max.value)
    }

    @Test
    fun `errorDismiss clears the error`() = runTest {
        val msg = "some error"
        everySuspend {
            roomApiClient.sendStateEvent(any(), any(), any(), any())
        } returns Result.failure(RuntimeException(msg))

        val model = testModel()

        backgroundScope.launch { model.error.collect { } }
        delay(500.milliseconds)

        assertNull(model.error.value, "error before interaction")
        model.setPowerLevels()
        delay(500.milliseconds)
        assertEquals(msg, model.error.value, "no error exists even though sendStateEvent failed")
        model.errorDismiss()
        delay(500.milliseconds)
        assertNull(model.error.value, "error was not cleared")
    }

    @Test
    fun `cannotChangePowerLevels if powerLevel lessThan m-room-state_default`() = runTest {
        val model = testModel()

        backgroundScope.launch { model.canChangePowerLevels.collect { } }
        delay(500.milliseconds)
        assertTrue(model.canChangePowerLevels.value)
    }


    @Test
    fun `initialize Value`() = runTest {
        testPowerLevelsValue(
            Value(backgroundScope, MutableStateFlow(30L), MutableStateFlow(50L)),
            error = false,
            isModified = false,
            isUnderMaxPowerLevel = true,
            isValidLong = true,
            modifiedValue = 30L,
        )
    }

    @Test
    fun `change Value-input to a valid Long`() = runTest {
        testPowerLevelsValue(
            Value(backgroundScope, MutableStateFlow(30L), MutableStateFlow(50L)),
            update = "40",
            error = false,
            isModified = true,
            isUnderMaxPowerLevel = true,
            isValidLong = true,
            modifiedValue = 40L,
        )
    }

    @Test
    fun `change Value-input to a invalid number`() = runTest {
        testPowerLevelsValue(
            Value(backgroundScope, MutableStateFlow(30L), MutableStateFlow(50L)),
            update = "foo",
            error = true,
            isModified = true,
            isUnderMaxPowerLevel = false,
            isValidLong = false,
            modifiedValue = null,
        )
    }

    @Test
    fun `change Value-input to a value that's too high`() = runTest {
        testPowerLevelsValue(
            Value(backgroundScope, MutableStateFlow(30L), MutableStateFlow(50L)),
            update = "100",
            error = true,
            isModified = true,
            isUnderMaxPowerLevel = false,
            isValidLong = true,
            modifiedValue = 100L,
        )
    }

    @Test
    fun `initialize Value with max=null`() = runTest {
        testPowerLevelsValue(
            Value(backgroundScope, MutableStateFlow(30L), MutableStateFlow(null)),
            error = false,
            isModified = false,
            isUnderMaxPowerLevel = false,
            isValidLong = true,
            modifiedValue = 30L,
        )
    }


    @Test
    fun `add events and reset works`() = runTest {
        val model = testModel()
        val event = EventType(null, "foobar")

        backgroundScope.launch { model.events.collect { } }
        delay(500.milliseconds)
        model.newEvent(event)
        delay(500.milliseconds)
        assertNull(model.error.value)
        assertContains(model.events.value, event)
        model.resetPowerLevels()
        delay(500.milliseconds)
        assertNull(model.error.value)
        assertFalse(model.events.value.contains(event))
    }

    @Test
    fun `back calls onBack`() = runTest {
        var backCalled = 0
        val model = testModel(onBack = { backCalled++ })

        model.back()
        delay(500.milliseconds)
        assertEquals(1, backCalled)
    }

    private fun TestScope.testModel(onBack: () -> Unit = {}): PowerlevelViewModelImpl = PowerlevelViewModelImpl(
        viewModelContext = testMatrixClientViewModelContext(
            userId = alice,
            di = koinApplication {
                modules(createTestDefaultTrixnityMessengerModules(mapOf(alice to matrixClient)))
            }.koin,
        ),
        roomId = testRoom,
        onBack = onBack,
    )
}


suspend inline fun TestScope.testPowerLevelsValue(
    value: Value,
    update: String? = null,
    error: Boolean,
    isModified: Boolean,
    isUnderMaxPowerLevel: Boolean,
    isValidLong: Boolean,
    modifiedValue: Long?,
) {
    backgroundScope.launch { value.input.collect { } }
    backgroundScope.launch { value.error.collect { } }
    backgroundScope.launch { value.isModified.collect { } }
    backgroundScope.launch { value.isUnderMaxPowerLevel.collect { } }
    backgroundScope.launch { value.isValidLong.collect { } }
    delay(500.milliseconds)
    if (update != null) {
        value.input.update(update)
        delay(500.milliseconds)
    }

    assertEquals(error, value.error.first(), "error has the wrong value")
    assertEquals(isModified, value.isModified.first(), "isModified has the wrong value")
    assertEquals(isUnderMaxPowerLevel, value.isUnderMaxPowerLevel.first(), "isUnderMaxPowerLevel has the wrong value")
    assertEquals(isValidLong, value.isValidLong.first(), "isValidLong has the wrong value")
    assertEquals(modifiedValue, value.modifiedValue(), "modifiedValue has the wrong value")
}
