package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.key.KeyService
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.user.PowerLevel
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.RoomApiClient
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import de.connect2x.trixnity.core.model.events.EventType
import de.connect2x.trixnity.core.model.events.m.room.PowerLevelsEventContent
import de.connect2x.trixnity.core.serialization.events.EventContentSerializerMappings
import de.connect2x.trixnity.core.serialization.events.default
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
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
import kotlinx.datetime.TimeZone
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
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
                single { EventContentSerializerMappings.default }
            })
        }.koin
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `error if improperly configured`() = runTest {
        val model = testModel()
        model.ban.input.update("notANumber")
        delay(500.milliseconds)
        assertNull(model.error.value)
    }

    @Test
    fun `empty event state always results in no error`() = runTest {
        val event = EventType(null, "foobar")
        setPowerLevels(PowerLevelsEventContent(events = mapOf(event to 30)))

        val model = testModel()

        backgroundScope.launch { model.events.collect { } }
        backgroundScope.launch { model.inputError.collect { } }

        delay(500.milliseconds)

        val events1 = model.events.value
        assertContains(events1, event.name)
        assertEquals(1, events1.size)

        events1.forEach { (_, v) -> v.input.update("notANumber") }

        delay(500.milliseconds)

        events1.forEach { (_, v) -> assertNotNull(v.error.value) }
        assertTrue(model.inputError.value)

        setPowerLevels(PowerLevelsEventContent())

        delay(500.milliseconds)

        val events2 = model.events.value
        assertEquals(mapOf(), events2)

        assertFalse(model.inputError.value)
    }

    @Test
    fun `empty event state always results in unmodified`() = runTest {
        val event = EventType(null, "foobar")

        setPowerLevels(PowerLevelsEventContent(events = mapOf(event to 30)))

        val model = testModel()

        backgroundScope.launch { model.events.collect { } }
        backgroundScope.launch { model.isAnyInputModified.collect { } }

        delay(500.milliseconds)

        val events1 = model.events.value
        assertContains(events1, event.name)
        assertEquals(1, events1.size)

        events1.forEach { (_, v) -> v.input.update("12") }

        delay(500.milliseconds)

        events1.forEach { (_, v) -> assertTrue(v.isModified.value) }
        assertTrue(model.isAnyInputModified.value)

        setPowerLevels(PowerLevelsEventContent())

        delay(500.milliseconds)

        val events2 = model.events.value
        assertEquals(mapOf(), events2)

        assertFalse(model.isAnyInputModified.value)
    }

    @Test
    fun `errorDismiss clears the error`() = runTest {
        val msg = "some error"
        everySuspend {
            roomApiClient.sendStateEvent(any(), any(), any())
        } returns Result.failure(RuntimeException(msg))

        val model = testModel()

        backgroundScope.launch { model.error.collect { } }
        backgroundScope.launch { model.events.collect { } }
        backgroundScope.launch { model.invite.isModified.collect { } }
        delay(500.milliseconds)

        assertNull(model.error.value, "error before interaction")
        model.invite.input.update("12") // some modification
        delay(500.milliseconds)
        model.setPowerLevels()
        delay(500.milliseconds)
        assertEquals(msg, model.error.value, "no error exists even though sendStateEvent failed")
        model.errorDismiss()
        delay(500.milliseconds)
        assertNull(model.error.value, "error was not cleared")
    }

    @Test
    fun `no change in state does not send`() = runTest {
        everySuspend {
            roomApiClient.sendStateEvent(any(), any(), any())
        } returns Result.failure(RuntimeException("some error"))

        val model = testModel()

        backgroundScope.launch { model.error.collect { } }
        delay(500.milliseconds)

        model.setPowerLevels() // no change
        delay(500.milliseconds)

        assertNull(model.error.value)
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
            old = 30L,
            max = 50L,

            isModified = false,
            underMaxPowerLevelErrMsg = false,
            validLongErrMsg = false,
            canChange = true,
            modifiedValue = 30L,
        )
    }

    @Test
    fun `old higher than max`() = runTest {
        testPowerLevelsValue(
            old = 50L,
            max = 30L,

            isModified = false,
            underMaxPowerLevelErrMsg = false,
            validLongErrMsg = false,
            canChange = false,
            modifiedValue = 50L,
        )
    }

    @Test
    fun `change Value-input to a valid Long`() = runTest {
        testPowerLevelsValue(
            old = 30L,
            max = 50L,

            update = "40",

            isModified = true,
            underMaxPowerLevelErrMsg = false,
            validLongErrMsg = false,
            canChange = true,
            modifiedValue = 40L,
        )
    }

    @Test
    fun `change Value-input to a invalid number`() = runTest {
        testPowerLevelsValue(
            old = 30L,
            max = 50L,

            update = "foo",

            isModified = true,
            underMaxPowerLevelErrMsg = false,
            validLongErrMsg = true,
            canChange = true,
            modifiedValue = null,
        )
    }

    @Test
    fun `change Value-input to a value that's too high`() = runTest {
        testPowerLevelsValue(
            old = 30L,
            max = 50L,

            update = "100",

            isModified = true,
            underMaxPowerLevelErrMsg = true,
            validLongErrMsg = false,
            canChange = true,
            modifiedValue = 100L,
        )
    }

    @Test
    fun `initialize Value with max=null`() = runTest {
        testPowerLevelsValue(
            old = 30L,
            max = null,

            isModified = false,
            underMaxPowerLevelErrMsg = false,
            validLongErrMsg = false,
            canChange = false,
            modifiedValue = 30L,
        )
    }

    @Test
    fun `Value calls onRemove on remove`() = runTest {
        var called = 0
        val v = PowerlevelViewModelImpl.ValueImpl(
            scope = backgroundScope,
            i18n = I18n(
                DefaultLanguages,
                createTestMatrixMessengerSettingsHolder(),
                { "en" },
                TimeZone.of("CET"),
            ),
            old = MutableStateFlow(25L),
            max = MutableStateFlow(null),
            onRemove = { called++ }
        )

        v.remove()
        assertEquals(1, called)
    }

    @Test
    fun `add events and reset works`() = runTest {
        val model = testModel()
        val event = EventType(null, "foobar")

        backgroundScope.launch { model.events.collect { } }
        delay(500.milliseconds)
        model.newEventInput.update(event.name)
        model.newEventCreate()
        delay(500.milliseconds)
        assertNull(model.error.value)
        assertContains(model.events.value, event.name)
        model.resetAll()
        delay(500.milliseconds)
        assertNull(model.error.value)
        assertFalse(model.events.value.contains(event.name))
    }

    @Test
    fun `back calls onBack`() = runTest {
        var backCalled = 0
        val model = testModel(onBack = { backCalled++ })

        model.back()
        delay(500.milliseconds)
        assertEquals(1, backCalled)
    }

    @Test
    fun `create unknownEvent`() = runTest {
        val event = EventType(null, "foobar")
        val model = testModel()

        backgroundScope.launch { model.newEventError.collect { } }
        delay(500.milliseconds)

        assertNull(model.newEventError.value)

        model.newEventInput.update(event.name)
        delay(500.milliseconds)

        assertNull(model.newEventError.value)

        model.newEventCreate()
        delay(500.milliseconds)

        assertContains(model.events.value, event.name)
        // input was cleared
        assertEquals("", model.newEventInput.value.text)
    }

    @Test
    fun `error if unknownEvent already exists`() = runTest {
        val event = EventType(null, "foobar")

        setPowerLevels(PowerLevelsEventContent(events = mapOf(event to 25L)))

        val model = testModel()

        backgroundScope.launch { model.newEventError.collect { } }
        delay(500.milliseconds)

        assertNull(model.newEventError.value)

        model.newEventInput.update(event.name)
        delay(500.milliseconds)

        assertNotNull(model.newEventError.value)

        model.newEventCreate()
        delay(500.milliseconds)

        // input was not cleared due to error
        assertEquals(event.name, model.newEventInput.value.text)
    }

    @Test
    fun `removing a value removes it from events`() = runTest {
        val event = EventType(null, "foobar")

        setPowerLevels(PowerLevelsEventContent(events = mapOf(event to 25L)))

        val model = testModel()

        backgroundScope.launch { model.events.collect { } }
        delay(500.milliseconds)

        assertContains(model.events.value, event.name)
        assertEquals(1, model.events.value.size)

        model.events.value.values.forEach { it.remove() }
        delay(500.milliseconds)

        assertEquals(0, model.events.value.size)
    }

    @Test
    fun `removing an existing event and then resetting brings it back`() = runTest {
        val event = EventType(null, "foobar")

        setPowerLevels(PowerLevelsEventContent(events = mapOf(event to 25L)))

        val model = testModel()

        backgroundScope.launch { model.events.collect { } }
        delay(500.milliseconds)

        assertContains(model.events.value, event.name)
        assertEquals(1, model.events.value.size)

        model.events.value.values.forEach { it.remove() }
        delay(500.milliseconds)

        assertEquals(0, model.events.value.size)

        model.resetAll()
        delay(500.milliseconds)

        assertContains(model.events.value, event.name)
        assertEquals(1, model.events.value.size)
    }

    @Test
    fun `removing an existing event counts as a modification`() = runTest {
        val event = EventType(null, "foobar")

        setPowerLevels(PowerLevelsEventContent(events = mapOf(event to 25L)))

        val model = testModel()

        backgroundScope.launch { model.isAnyInputModified.collect { } }
        backgroundScope.launch { model.events.collect { } }
        delay(500.milliseconds)

        assertContains(model.events.value, event.name)
        assertEquals(1, model.events.value.size)

        model.events.value.values.forEach { it.remove() }
        delay(500.milliseconds)

        assertTrue(model.isAnyInputModified.value)
    }

    @Test
    fun `adding and then removing an event results in no changes`() = runTest {
        val event = EventType(null, "foobar")

        setPowerLevels(PowerLevelsEventContent())

        val model = testModel()

        backgroundScope.launch { model.isAnyInputModified.collect { } }
        backgroundScope.launch { model.events.collect { } }
        delay(500.milliseconds)

        model.newEventInput.update(event.name)
        model.newEventCreate()
        delay(500.milliseconds)

        assertContains(model.events.value, event.name)
        assertTrue(model.isAnyInputModified.value)

        model.events.value.values.forEach { it.remove() }
        delay(500.milliseconds)

        assertEquals(emptyMap(), model.events.value)
        assertFalse(model.isAnyInputModified.value)
    }

    @Test
    fun `removing an existing event makes it appear in availableUnsetEvents`() = runTest {
        val event = EventType(null, "m.room.message") // exists in DefaultEventContentSerializerMappings
        setPowerLevels(PowerLevelsEventContent(events = mapOf(event to 25L)))

        val model = testModel()

        backgroundScope.launch { model.availableUnsetEvents.collect { } }
        backgroundScope.launch { model.events.collect { } }
        delay(500.milliseconds)

        assertFalse(model.availableUnsetEvents.value.contains(event.name), "${event.name} in availableUnsetEvents")

        model.events.value.values.forEach { it.remove() }
        delay(500.milliseconds)

        assertContains(model.availableUnsetEvents.value, event.name)
    }

    @Test
    fun `creating an event makes removes it from availableUnsetEvents`() = runTest {
        val event = EventType(null, "m.room.message") // exists in DefaultEventContentSerializerMappings
        setPowerLevels(PowerLevelsEventContent())

        val model = testModel()

        backgroundScope.launch { model.availableUnsetEvents.collect { } }
        backgroundScope.launch { model.events.collect { } }
        delay(500.milliseconds)

        assertContains(model.availableUnsetEvents.value, event.name)

        model.newEventInput.update(event.name)
        model.newEventCreate()
        delay(500.milliseconds)

        assertFalse(model.availableUnsetEvents.value.contains(event.name), "${event.name} in availableUnsetEvents")
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

    private suspend inline fun TestScope.testPowerLevelsValue(
        old: Long,
        max: Long?,

        update: String? = null,

        isModified: Boolean,
        underMaxPowerLevelErrMsg: Boolean,
        validLongErrMsg: Boolean,
        canChange: Boolean,
        modifiedValue: Long?,
    ) {
        val i18n = I18n(
            DefaultLanguages,
            createTestMatrixMessengerSettingsHolder(),
            GetSystemLang { "en" },
            TimeZone.of("CET"),
        )

        val value = PowerlevelViewModelImpl.ValueImpl(
            scope = backgroundScope,
            i18n = i18n,
            old = MutableStateFlow(old),
            max = MutableStateFlow(max),
        )

        backgroundScope.launch { value.input.collect { } }
        backgroundScope.launch { value.error.collect { } }
        backgroundScope.launch { value.isModified.collect { } }
        backgroundScope.launch { value.canChange.collect { } }
        delay(500.milliseconds)

        if (update != null) {
            value.input.update(update)
            delay(500.milliseconds)
        }

        assertEquals(isModified, value.isModified.first(), "isModified has the wrong value")
        assertEquals(modifiedValue, value.input.value.text.toLongOrNull(), "modifiedValue has the wrong value")
        assertEquals(canChange, value.canChange.first(), "canChange has the wrong value")

        val err = value.error.first()
        when {
            validLongErrMsg ->
                assertEquals(i18n.powerLevelInputErrNotANumber(), err)

            underMaxPowerLevelErrMsg ->
                assertEquals(i18n.powerLevelInputErrAboveAllowedPowerLevel(50L), err)

            else ->
                assertNull(err)
        }
    }

    private var step = 1L
    private val state = MutableStateFlow(
        StateEvent(
            content = PowerLevelsEventContent(),
            id = EventId("eventId"),
            sender = alice,
            roomId = testRoom,
            originTimestamp = step * 100,
            unsigned = null,
            stateKey = "",
        )
    )

    init {
        every {
            roomService.getState(testRoom, PowerLevelsEventContent::class, "")
        } returns state
    }

    private fun setPowerLevels(pl: PowerLevelsEventContent) {
        step++
        state.value = StateEvent(
            content = pl,
            id = EventId("eventId"),
            sender = alice,
            roomId = testRoom,
            originTimestamp = step * 100,
            unsigned = null,
            stateKey = "",
        )
    }
}
