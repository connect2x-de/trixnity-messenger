package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import de.connect2x.lognity.api.backend.Backend
import de.connect2x.lognity.api.logger.Level
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.RoomOutboxMessage
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.RoomApiClient
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import de.connect2x.trixnity.core.model.events.RoomEventContent
import de.connect2x.trixnity.core.model.events.m.MarkedUnreadEventContent
import de.connect2x.trixnity.core.model.events.m.RelatesTo
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.continually
import de.connect2x.trixnity.messenger.coroutineDispatcher
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.DefaultAsserter.fail
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonObject
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@Suppress("NonAsciiCharacters")
class TimelineViewModelTest {
    private var lifecycleRegistry: LifecycleRegistry

    private val roomId = RoomId("!room1")
    private val me = UserId("user1", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()
    val roomsApiClientMock = mock<RoomApiClient>()
    val roomHeaderViewModelMock = mock<RoomHeaderViewModel>()
    val inputAreaViewModelMock = mock<InputAreaViewModel>()
    private val matrixClientServerApiMock = mock<MatrixClientServerApiClient>()
    private val clock = mock<Clock>()

    private var syncStateMocker: BlockingAnsweringScope<StateFlow<SyncState>>
    private val outboxMessagesFlow = MutableStateFlow(emptyList<RoomOutboxMessage<*>>())

    private val dummyEvent =
        flowOf(
            TimelineEvent(
                messageEvent(sender = alice, roomId = roomId, eventId = EventId("dummy")) { text("dummy") },
                gap = null,
                previousEventId = null,
                nextEventId = null,
            )
        )

    init {
        resetMocks(
            matrixClientMock,
            roomServiceMock,
            userServiceMock,
            matrixClientServerApiMock,
            roomsApiClientMock,
            roomHeaderViewModelMock,
            inputAreaViewModelMock,
            clock,
        )
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
        every { matrixClientMock.api } returns matrixClientServerApiMock
        every { matrixClientMock.userId } returns me

        syncStateMocker = every { matrixClientMock.syncState }
        syncStateMocker returns MutableStateFlow(SyncState.STARTED)

        every { matrixClientServerApiMock.room } returns roomsApiClientMock
        everySuspend { roomsApiClientMock.setReadMarkers(any(), any(), any(), any()) } returns Result.success(Unit)
        everySuspend { roomsApiClientMock.setAccountData(any(), any(), any(), any()) } returns Result.success(Unit)
        everySuspend { roomsApiClientMock.setReceipt(any(), any(), any(), any()) } returns Result.success(Unit)

        every { roomServiceMock.getOutbox() } returns outboxMessagesFlow.map { it.map { MutableStateFlow(it) } }

        every { roomServiceMock.getOutbox(roomId = any()) } returns
            outboxMessagesFlow.map { it.filter { it.roomId == roomId }.map { MutableStateFlow(it) } }

        every { userServiceMock.canRedactEvent(any(), any()) } returns flowOf(true)
        every { userServiceMock.canSendEvent(any(), any<KClass<out RoomEventContent>>()) } returns flowOf(true)
        every { userServiceMock.getReceiptsById(any(), any()) } returns flowOf(null)

        every { roomServiceMock.getTimelineEvent(any(), any(), any()) } returns dummyEvent
        every { roomServiceMock.getNextTimelineEvent(any(), any()) } returns flowOf(null)
        every { roomServiceMock.getTimelineEventRelations(roomId, any(), any()) } returns flowOf(null)

        every { userServiceMock.getAll(roomId) } returns
            MutableStateFlow(
                mapOf(
                    me to
                        flowOf(
                            RoomUser(
                                roomId,
                                me,
                                "User1",
                                StateEvent(
                                    MemberEventContent(membership = Membership.JOIN),
                                    EventId(""),
                                    me,
                                    roomId,
                                    0L,
                                    stateKey = "",
                                ),
                            )
                        ),
                    alice to
                        flowOf(
                            RoomUser(
                                roomId,
                                alice,
                                "Alice",
                                StateEvent(
                                    MemberEventContent(membership = Membership.JOIN),
                                    EventId(""),
                                    alice,
                                    roomId,
                                    0L,
                                    stateKey = "",
                                ),
                            )
                        ),
                    bob to
                        flowOf(
                            RoomUser(
                                roomId,
                                bob,
                                "Bob",
                                StateEvent(
                                    MemberEventContent(membership = Membership.JOIN),
                                    EventId(""),
                                    bob,
                                    roomId,
                                    0L,
                                    stateKey = "",
                                ),
                            )
                        ),
                )
            )
        every { userServiceMock.getAllReceipts(roomId) } returns MutableStateFlow(emptyMap())
        every { userServiceMock.getById(roomId, any()) } returns MutableStateFlow(null)
        every { userServiceMock.getById(roomId, any()) } returns flowOf(null)
        everySuspend { userServiceMock.loadMembers(roomId, false) } returns Unit

        every { clock.now() } returns Instant.parse("2020-09-01T01:00:00.000Z")

        every { roomServiceMock.getDraftMessage(any()) } returns flowOf(null)

        outboxMessagesFlow.value = listOf() // reset

        lifecycleRegistry = LifecycleRegistry()
        lifecycleRegistry.start()
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
        val baseConfigSpec = Backend.configSpec
        Backend.configSpec = {
            baseConfigSpec()
            override {
                applyWhen { logger, _, _ -> logger.context.get(Logger.Name)?.name?.startsWith("VM:Timeline") == true }
                level = Level.TRACE
            }
        }
    }

    // TODO
    @AfterTest
    fun afterTest() {
        lifecycleRegistry.destroy()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun runTest(action: suspend TestScope.() -> Unit) =
        kotlinx.coroutines.test.runTest {
            Dispatchers.setMain(coroutineDispatcher)
            action()
        }

    @Test
    fun `elements » show new messages when at the end of timeline end of timeline has been starting element`() =
        runTest {
            val timelineMock =
                timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) { text("Hello") }
                    +messageEvent(sender = alice) { text("World") }
                }

            val cut = timelineViewModel(mock())
            delay(100.milliseconds)
            cut.elements waitForSize 2
            cut.setViewState(
                TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "$roomId-1",
                    firstLoadedElement = "$roomId-0",
                    lastLoadedElement = "$roomId-1",
                    timelineIsFocused = true,
                    finishedScrollTo = "$roomId-1",
                )
            )
            delay(100.milliseconds) // give the viewmodel time to compute derived values

            timelineMock.addEvents { +messageEvent(sender = alice) { text("Woohoo") } }

            cut.elements waitForSize 3
            cut.elements.value.last().key shouldBe "$roomId-2"
        }

    @Test
    fun `elements » only show outbox messages of this room`() = runTest {
        outboxMessagesFlow.value =
            listOf(
                RoomOutboxMessage(
                    transactionId = "1",
                    roomId = RoomId("not this room"),
                    content = RoomMessageEventContent.TextBased.Text(body = "Hello"),
                    createdAt = Instant.fromEpochMilliseconds(0),
                ),
                RoomOutboxMessage(
                    transactionId = "2",
                    roomId = roomId,
                    content = RoomMessageEventContent.TextBased.Text(body = "Right"),
                    createdAt = Instant.fromEpochMilliseconds(1),
                ),
                RoomOutboxMessage(
                    transactionId = "3",
                    roomId = RoomId("totally not this room"),
                    content = RoomMessageEventContent.TextBased.Text(body = "from outer space"),
                    createdAt = Instant.fromEpochMilliseconds(2),
                ),
            )
        timeline(roomServiceMock, roomId) { +messageEvent(sender = alice) { text("Hello") } }
        val cut = timelineViewModel()
        cut.elements waitForSize 2 // 1 message + 1 outbox message
    }

    @Test
    fun `elements » filter outbox message that is already in the timeline`() = runTest {
        outboxMessagesFlow.value =
            listOf(
                RoomOutboxMessage(
                    transactionId = "1",
                    roomId = roomId,
                    content = RoomMessageEventContent.TextBased.Text(body = "Hello"),
                    createdAt = Instant.fromEpochMilliseconds(0),
                ),
                RoomOutboxMessage(
                    transactionId = "2",
                    roomId = roomId,
                    content = RoomMessageEventContent.TextBased.Text(body = "World"),
                    createdAt = Instant.fromEpochMilliseconds(1),
                ),
            )

        timeline(roomServiceMock, roomId) { +messageEvent(sender = me, transactionId = "1") { text("Hello") } }

        val cut = timelineViewModel()
        cut.elements waitForSize 2

        cut.elements.first() shouldHaveSize 2
    }

    @Test
    fun `elements » add new outbox message and when it is received as timeline event from the server not show as outbox message`() =
        runTest {
            val timelineMock =
                timeline(roomServiceMock, roomId) { +messageEvent(sender = me, transactionId = "1") { text("Hello") } }
            val cut = timelineViewModel()
            cut.elements waitForSize 1

            outboxMessagesFlow.value =
                listOf(
                    RoomOutboxMessage(
                        transactionId = "transactionId-1",
                        roomId = roomId,
                        content = RoomMessageEventContent.TextBased.Text(body = "Hello"),
                        createdAt = Instant.fromEpochMilliseconds(0),
                    )
                )
            cut.elements waitForSize 2

            delay(500.milliseconds)
            timelineMock.addEvents { +messageEvent(sender = me, transactionId = "transactionId-1") { text("Hello") } }

            continually(2.seconds) {
                cut.elements.first() shouldHaveSize 2
                cut.elements.first()[1].key shouldBe "$roomId-transactionId-1"
            }
        }

    @Test
    fun `elements » only contain the newest version of a replace event`() = runTest {
        timeline(roomServiceMock, roomId) {
            +messageEvent(sender = alice) { text("Hello!!!") }
            +MessageEvent(
                content =
                    RoomMessageEventContent.TextBased.Text(
                        body = "Hello again.",
                        relatesTo = RelatesTo.Replace(EventId("0")),
                    ),
                id = EventId("replace-1"),
                sender = alice,
                roomId = roomId,
                originTimestamp = 1234,
            )
            +MessageEvent(
                content =
                    RoomMessageEventContent.TextBased.Text(
                        body = "Hello!!!",
                        relatesTo = RelatesTo.Replace(EventId("replace-1")),
                    ),
                id = EventId("replace-2"),
                sender = alice,
                roomId = roomId,
                originTimestamp = 2345,
            )
        }
        val cut = timelineViewModel()
        cut.elements waitForSize 3
        val job = launch { cut.elements.collect {} }
        cut.elements.first().lastOrNull()?.key shouldBe "${roomId.full}-replace-2"
        eventually(2.seconds) {
            val elementViewModel = cut.elements.first().firstOrNull()?.element?.value
            if (elementViewModel is RoomMessageTimelineElementViewModel<*>) {
                elementViewModel.body shouldBe "Hello!!!"
            } else fail("")
            cut.elements.first().getOrNull(1)?.element?.value shouldBe TimelineElementViewModel.Empty
            cut.elements.first().lastOrNull()?.element?.value shouldBe TimelineElementViewModel.Empty
        }

        job.cancel()
    }

    @Test
    fun `viewState » load more messages before`() = runTest {
        timeline(roomServiceMock, roomId) {
            +messageEvent(sender = alice) { text("Hello") }
            (1..19).forEach { +messageEvent(sender = alice) { text("World-$it") } }
        }

        val cut = timelineViewModel()
        delay(100.milliseconds)
        cut.elements waitForSize 11

        // timeline starts at the end (no read messages) -> [9..19] are shown, if first visible is in the first 10 ->
        // load before
        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "$roomId-9",
                lastVisibleElement = "notRelevant",
                firstLoadedElement = "$roomId-9",
                lastLoadedElement = "$roomId-19",
                timelineIsFocused = true,
                finishedScrollTo = "$roomId-19",
            )
        )
        cut.elements waitForSize 20
    }

    @Test
    fun `viewState » not load more messages before`() = runTest {
        timeline(roomServiceMock, roomId) {
            +messageEvent(sender = alice) { text("Hello") }
            (1..19).forEach { +messageEvent(sender = alice) { text("World-$it") } }
        }

        val cut = timelineViewModel()
        cut.elements waitForSize 11

        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "notRelevant",
                lastVisibleElement = "$roomId-8", // [9..19], see above
                firstLoadedElement = "$roomId-9",
                lastLoadedElement = "$roomId-19",
                timelineIsFocused = true,
                finishedScrollTo = "$roomId-19",
            )
        )
        delay(100.milliseconds)
        cut.elements.value.size shouldBe 11
    }

    @Test
    fun `viewState » load more messages after`() = runTest {
        val timelineMock =
            timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) { text("Hello") }
                (1..19).forEach { +messageEvent(sender = alice) { text("World-$it") } }
            }
        timelineMock.fullyReadEventIndex.value = 0

        val cut = timelineViewModel()
        delay(100.milliseconds)
        cut.elements waitForSize 11

        // fully read events is set -> start at beginning -> [0..10], 9 is in last messages -> load after
        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "notRelevant",
                lastVisibleElement = "$roomId-9",
                firstLoadedElement = "$roomId-0",
                lastLoadedElement = "$roomId-10",
                timelineIsFocused = true,
                finishedScrollTo = "$roomId-0",
            )
        )
        delay(100.milliseconds)
        cut.elements waitForSize 20
    }

    @Test
    fun `viewState » load more messages after when outbox is last message`() = runTest {
        val timelineMock = timeline(roomServiceMock, roomId) { +messageEvent(sender = alice) { text("Hello") } }
        timelineMock.fullyReadEventIndex.value = 0

        outboxMessagesFlow.value =
            listOf(
                RoomOutboxMessage(
                    transactionId = "transactionId-1",
                    roomId = roomId,
                    content = RoomMessageEventContent.TextBased.Text(body = "Hello to you!"),
                    createdAt = Instant.fromEpochMilliseconds(0),
                )
            )

        val cut = timelineViewModel()
        delay(100.milliseconds)
        cut.elements waitForSize 2

        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "notRelevant",
                lastVisibleElement = "$roomId-transactionId-1",
                firstLoadedElement = "$roomId-0",
                lastLoadedElement = "$roomId-transactionId-1",
                timelineIsFocused = true,
                finishedScrollTo = "$roomId-0",
            )
        )
        timelineMock.addEvents { +messageEvent(sender = alice) { text("Hello") } }
        delay(100.milliseconds)

        cut.elements waitForSize 3
    }

    @Test
    fun `viewState » not load more messages after`() = runTest {
        val timelineMock =
            timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) { text("Hello") }
                (1..40).forEach { +messageEvent(sender = alice) { text("World-$it") } }
            }
        timelineMock.fullyReadEventIndex.value = 0

        val cut = timelineViewModel()
        cut.elements waitForSize 11

        // 0 is at beginning -> do NOT load after
        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "notRelevant",
                lastVisibleElement = "$roomId-0",
                firstLoadedElement = "$roomId-0",
                lastLoadedElement = "$roomId-10",
                timelineIsFocused = true,
                finishedScrollTo = "$roomId-0",
            )
        )
        delay(100.milliseconds)
        cut.elements.value.size shouldBe 11
    }

    @Test
    fun `jumpToEndOfTimeline » directly jump to the end of the timeline if the last event is already in the timeline`() =
        runTest {
            val timelineMock =
                timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) { text("Hello") }
                    (1..9).forEach { +messageEvent(sender = alice) { text("World-$it") } }
                }
            timelineMock.fullyReadEventIndex.value = 0

            val cut = timelineViewModel()
            delay(100.milliseconds)
            cut.setViewState(
                TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "$roomId-0",
                    firstLoadedElement = "$roomId-0",
                    lastLoadedElement = "$roomId-9",
                    timelineIsFocused = true,
                    finishedScrollTo = "$roomId-0",
                )
            )
            cut.elements waitForSize 10

            cut.jumpToEndOfTimeline()
            delay(100.milliseconds)
            cut.scrollTo.value shouldBe "$roomId-9"
            cut.elements waitForSize 10
        }

    @Test
    fun `jumpToEndOfTimeline » load the last event of the room and add it to the timeline if it is not yet present in the timeline`() =
        runTest {
            val timelineMock =
                timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) { text("Hello") }
                    (1..10).forEach { +messageEvent(sender = alice) { text("World-$it") } }

                    +messageEvent(sender = alice) { text("latest") }
                }
            timelineMock.fullyReadEventIndex.value = 0

            val cut = timelineViewModel()
            delay(100.milliseconds)
            cut.setViewState(
                TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "notRelevant",
                    firstLoadedElement = "$roomId-0",
                    lastLoadedElement = "$roomId-10",
                    timelineIsFocused = true,
                    finishedScrollTo = "$roomId-0",
                )
            )
            cut.elements waitForSize 11
            cut.elements.value.last().key shouldBe "$roomId-10"

            cut.jumpToEndOfTimeline()
            delay(100.milliseconds)
            cut.setViewState(
                TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "notRelevant",
                    firstLoadedElement = "$roomId-1",
                    lastLoadedElement = "$roomId-11",
                    timelineIsFocused = true,
                    finishedScrollTo = null,
                )
            )
            delay(100.milliseconds)
            cut.scrollTo.value shouldBe "$roomId-11"
            cut.setViewState(
                TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "notRelevant",
                    firstLoadedElement = "$roomId-1",
                    lastLoadedElement = "$roomId-11",
                    timelineIsFocused = true,
                    finishedScrollTo = "$roomId-11",
                )
            )
            cut.elements waitForSize 11
            cut.elements.first { it.last().key == "$roomId-11" }
        }

    @Test
    fun `leaveRoom » show an error message when trying to leave a room and we are not connected`() = runTest {
        syncStateMocker returns MutableStateFlow(SyncState.ERROR)
        timeline(roomServiceMock, roomId) {}

        val cut = timelineViewModel()
        cut.leaveRoom()

        cut.error.filterNotNull().first()
        // we have not mocked roomsApiClientMock.leaveRoom() and onBackMock.invoke(), so if they would be called, an
        // exception would be thrown

        cut.errorDismiss()
        cut.error.value shouldBe null
    }

    @Test
    fun `leaveRoom » show an error message when leaving the room fails`() = runTest {
        everySuspend { roomsApiClientMock.leaveRoom(roomId, any()) } returns Result.failure(RuntimeException("Oh no!"))

        timeline(roomServiceMock, roomId) {}

        val cut = timelineViewModel()
        cut.leaveRoom()

        // onBackMock is not mocked correctly, so if called, an exception would be thrown
        cut.error.filterNotNull().first()
    }

    @Test
    fun `scrollTo » scroll to the end when we put a message in the outbox and user is at end of timeline`() = runTest {
        val timelineMock = timeline(roomServiceMock, roomId) {}
        val cut = timelineViewModel()
        val scrollToCalled = cut.scrollTo.scan(listOf<String?>()) { old, new -> old + new }.stateIn(backgroundScope)
        timelineMock.addEvents { +messageEvent(sender = alice) { text("Hello!") } }
        delay(100.milliseconds)
        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "notRelevant",
                lastVisibleElement = "$roomId-0",
                firstLoadedElement = "$roomId-0",
                lastLoadedElement = "$roomId-0",
                timelineIsFocused = true,
                finishedScrollTo = "$roomId-0",
            )
        )
        delay(100.milliseconds)

        cut.elements waitForSize 1
        scrollToCalled.value shouldBe listOf(null, "$roomId-0", null)

        outboxMessagesFlow.value =
            listOf(
                RoomOutboxMessage(
                    transactionId = "transactionId-1",
                    roomId = roomId,
                    content = RoomMessageEventContent.TextBased.Text(body = "Hello to you!"),
                    createdAt = Instant.fromEpochMilliseconds(0),
                )
            )
        cut.elements waitForSize 2
        delay(100.milliseconds)
        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "notRelevant",
                lastVisibleElement = "$roomId-0",
                firstLoadedElement = "$roomId-0",
                lastLoadedElement = "$roomId-transactionId-1",
                timelineIsFocused = true,
                finishedScrollTo = null,
            )
        )
        delay(100.milliseconds)
        scrollToCalled.value shouldBe listOf(null, "$roomId-0", null, "$roomId-transactionId-1")

        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "notRelevant",
                lastVisibleElement = "$roomId-transactionId-1",
                firstLoadedElement = "$roomId-0",
                lastLoadedElement = "$roomId-transactionId-1",
                timelineIsFocused = true,
                finishedScrollTo = "$roomId-transactionId-1",
            )
        )
        delay(100.milliseconds)
        scrollToCalled.value shouldBe listOf(null, "$roomId-0", null, "$roomId-transactionId-1", null)

        outboxMessagesFlow.value =
            listOf(
                RoomOutboxMessage(
                    transactionId = "transactionId-1",
                    roomId = roomId,
                    content = RoomMessageEventContent.TextBased.Text(body = "Hello to you!"),
                    createdAt = Instant.fromEpochMilliseconds(0),
                ),
                RoomOutboxMessage(
                    transactionId = "transactionId-2",
                    roomId = roomId,
                    content = RoomMessageEventContent.TextBased.Text(body = "My second message."),
                    createdAt = Instant.fromEpochMilliseconds(1),
                ),
            )
        cut.elements waitForSize 3
        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "notRelevant",
                lastVisibleElement = "$roomId-transactionId-1",
                firstLoadedElement = "$roomId-0",
                lastLoadedElement = "$roomId-transactionId-2",
                timelineIsFocused = true,
                finishedScrollTo = null,
            )
        )
        delay(100.milliseconds)
        scrollToCalled.value shouldBe
            listOf(null, "$roomId-0", null, "$roomId-transactionId-1", null, "$roomId-transactionId-2")

        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "notRelevant",
                lastVisibleElement = "$roomId-transactionId-2",
                firstLoadedElement = "$roomId-0",
                lastLoadedElement = "$roomId-transactionId-2",
                timelineIsFocused = true,
                finishedScrollTo = "$roomId-transactionId-2",
            )
        )
        delay(100.milliseconds)
        scrollToCalled.value shouldBe
            listOf(null, "$roomId-0", null, "$roomId-transactionId-1", null, "$roomId-transactionId-2", null)
    }

    @Test
    fun `scrollTo » scroll to the end when a new message is added at the end of the timeline where the user is at the bottom`() =
        runTest {
            val timelineMock = timeline(roomServiceMock, roomId) { +messageEvent(sender = alice) { text("Hello!") } }
            val cut = timelineViewModel()
            val scrollToCalled = cut.scrollTo.scan(listOf<String?>()) { old, new -> old + new }.stateIn(backgroundScope)
            delay(100.milliseconds)

            cut.elements waitForSize 1
            cut.setViewState(
                TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "$roomId-0",
                    firstLoadedElement = "$roomId-0",
                    lastLoadedElement = "$roomId-0",
                    timelineIsFocused = true,
                    finishedScrollTo = "$roomId-0",
                )
            )
            delay(100.milliseconds)

            timelineMock.addEvents { +messageEvent(sender = alice) { text("World!") } }
            delay(100.milliseconds)

            cut.elements waitForSize 2
            cut.setViewState(
                TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "$roomId-0",
                    firstLoadedElement = "$roomId-0",
                    lastLoadedElement = "$roomId-1",
                    timelineIsFocused = true,
                    finishedScrollTo = null,
                )
            )
            delay(100.milliseconds)
            scrollToCalled.value shouldBe listOf("$roomId-0", null, "$roomId-1")
            cut.setViewState(
                TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "$roomId-0",
                    firstLoadedElement = "$roomId-0",
                    lastLoadedElement = "$roomId-1",
                    timelineIsFocused = true,
                    finishedScrollTo = "$roomId-1",
                )
            )
            delay(100.milliseconds)
            scrollToCalled.value shouldBe listOf("$roomId-0", null, "$roomId-1", null)
        }

    @Test
    fun `scrollTo » don't scroll to the end when a new message is added at the end of the timeline and there is an outbox message`() =
        runTest {
            val timelineMock = timeline(roomServiceMock, roomId) { +messageEvent(sender = alice) { text("Hello!") } }
            val cut = timelineViewModel()
            val scrollToCalled = cut.scrollTo.scan(listOf<String?>()) { old, new -> old + new }.stateIn(backgroundScope)
            delay(100.milliseconds)

            cut.elements waitForSize 1
            cut.setViewState(
                TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "$roomId-0",
                    firstLoadedElement = "$roomId-0",
                    lastLoadedElement = "$roomId-0",
                    timelineIsFocused = true,
                    finishedScrollTo = "$roomId-0",
                )
            )
            delay(100.milliseconds)
            scrollToCalled.value shouldBe listOf("!room1-0", null)

            outboxMessagesFlow.value =
                listOf(
                    RoomOutboxMessage(
                        transactionId = "transactionId-1",
                        roomId = roomId,
                        content = RoomMessageEventContent.TextBased.Text(body = "Hello to you!"),
                        createdAt = Instant.fromEpochMilliseconds(0),
                    )
                )
            delay(100.milliseconds)
            cut.setViewState(
                TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "$roomId-0",
                    firstLoadedElement = "$roomId-0",
                    lastLoadedElement = "!room1-transactionId-1",
                    timelineIsFocused = true,
                    finishedScrollTo = null,
                )
            )
            delay(100.milliseconds)
            scrollToCalled.value shouldBe listOf("!room1-0", null, "!room1-transactionId-1")
            cut.setViewState(
                TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "$roomId-0",
                    firstLoadedElement = "$roomId-0",
                    lastLoadedElement = "!room1-transactionId-1",
                    timelineIsFocused = true,
                    finishedScrollTo = "!room1-transactionId-1",
                )
            )
            delay(100.milliseconds)
            scrollToCalled.value shouldBe listOf("!room1-0", null, "!room1-transactionId-1", null)

            timelineMock.addEvents { +messageEvent(sender = alice) { text("World!") } }
            delay(100.milliseconds)

            cut.elements waitForSize 3
            scrollToCalled.value shouldBe listOf("!room1-0", null, "!room1-transactionId-1", null)
        }

    @Test
    fun `scrollTo » don't scroll to the end when a new message is added but the end of the timeline is not visible`() =
        runTest {
            val timelineMock =
                timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) { text("Hello!") }
                    +messageEvent(sender = alice) { text("World!") }
                }
            val cut = timelineViewModel()
            val scrollToCalled = cut.scrollTo.scan(listOf<String?>()) { old, new -> old + new }.stateIn(backgroundScope)
            delay(100.milliseconds)

            cut.elements waitForSize 2

            cut.setViewState(
                TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "$roomId-0",
                    firstLoadedElement = "$roomId-0",
                    lastLoadedElement = "$roomId-1",
                    timelineIsFocused = true,
                    finishedScrollTo = "$roomId-1",
                )
            )
            delay(100.milliseconds)

            scrollToCalled.value shouldBe listOf("!room1-1", null)

            // this will not trigger a creation of a viewmodel as we are not at the end of the timeline
            timelineMock.addEvents { +messageEvent(sender = alice) { text("Dino!") } }
            delay(100.milliseconds)
            cut.setViewState(
                TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "$roomId-0",
                    firstLoadedElement = "$roomId-0",
                    lastLoadedElement = "$roomId-2",
                    timelineIsFocused = true,
                    finishedScrollTo = null,
                )
            )
            delay(100.milliseconds)

            scrollToCalled.value shouldBe listOf("!room1-1", null)
        }

    @Test
    fun `jumpTo » scroll to a message when it's loaded and visible`() = runTest {
        timeline(roomServiceMock, roomId) { repeat(40) { +messageEvent(sender = alice) { text("Hello $it!") } } }
        val cut = timelineViewModel()
        delay(500.milliseconds)

        cut.elements waitForSize 11

        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "$roomId-30",
                lastVisibleElement = "$roomId-39",
                firstLoadedElement = "$roomId-29",
                lastLoadedElement = "$roomId-39",
                timelineIsFocused = true,
                finishedScrollTo = null,
            )
        )
        delay(100.milliseconds)
        val scrollToCalled = cut.scrollTo.scan(listOf<String?>()) { old, new -> old + new }.stateIn(backgroundScope)
        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "$roomId-30",
                lastVisibleElement = "$roomId-39",
                firstLoadedElement = "$roomId-29",
                lastLoadedElement = "$roomId-39",
                timelineIsFocused = true,
                finishedScrollTo = "!room1-39",
            )
        )
        delay(100.milliseconds)

        scrollToCalled.value shouldBe listOf("!room1-39", null)

        cut.jumpTo(roomId, EventId("35"))
        delay(100.milliseconds)

        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "$roomId-30",
                lastVisibleElement = "$roomId-39",
                firstLoadedElement = "$roomId-9",
                lastLoadedElement = "$roomId-39",
                timelineIsFocused = true,
                finishedScrollTo = "!room1-35",
            )
        )
        delay(100.milliseconds)

        scrollToCalled.value shouldBe listOf("!room1-39", null, "!room1-35", null)
    }

    @Test
    fun `jumpTo » scroll to a message when it's loaded but not visible`() = runTest {
        timeline(roomServiceMock, roomId) { repeat(40) { +messageEvent(sender = alice) { text("Hello $it!") } } }
        val cut = timelineViewModel()
        delay(100.milliseconds)

        cut.elements waitForSize 11

        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "$roomId-36",
                lastVisibleElement = "$roomId-39",
                firstLoadedElement = "$roomId-29",
                lastLoadedElement = "$roomId-39",
                timelineIsFocused = true,
                finishedScrollTo = "$roomId-39",
            )
        )
        delay(100.milliseconds)

        cut.jumpTo(roomId, EventId("35"))
        delay(100.milliseconds)

        cut.scrollTo.value shouldBe "!room1-35"

        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "$roomId-36",
                lastVisibleElement = "$roomId-39",
                firstLoadedElement = "$roomId-9",
                lastLoadedElement = "$roomId-39",
                timelineIsFocused = true,
                finishedScrollTo = "$roomId-35",
            )
        )
        delay(100.milliseconds)
        cut.scrollTo.value shouldBe null
    }

    @Test
    fun `jumpTo » scroll to a message when it's not loaded`() = runTest {
        timeline(roomServiceMock, roomId) { repeat(40) { +messageEvent(sender = alice) { text("Hello $it!") } } }
        val cut = timelineViewModel()
        delay(100.milliseconds)

        cut.elements waitForSize 11

        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "notRelevant",
                lastVisibleElement = "notRelevant",
                firstLoadedElement = "$roomId-29",
                lastLoadedElement = "$roomId-39",
                timelineIsFocused = true,
                finishedScrollTo = "$roomId-39",
            )
        )
        delay(100.milliseconds)

        cut.jumpTo(roomId, EventId("10"))
        delay(100.milliseconds)
        cut.scrollTo.value shouldBe null

        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "notRelevant",
                lastVisibleElement = "notRelevant",
                firstLoadedElement = "$roomId-0",
                lastLoadedElement = "$roomId-20",
                timelineIsFocused = true,
                finishedScrollTo = null,
            )
        )
        delay(100.milliseconds)

        cut.scrollTo.value shouldBe "!room1-10"

        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "notRelevant",
                lastVisibleElement = "notRelevant",
                firstLoadedElement = "$roomId-0",
                lastLoadedElement = "$roomId-20",
                timelineIsFocused = true,
                finishedScrollTo = "$roomId-10",
            )
        )
        delay(100.milliseconds)
        cut.scrollTo.value shouldBe null
    }

    @Test
    fun `unreadCount » count unread messages correctly when adding messages`() = runTest {
        val timelineMock = timeline(roomServiceMock, roomId) { +messageEvent(sender = alice) { text("Read message") } }

        timelineMock.fullyReadEventIndex.value = 0

        val cut = timelineViewModel()

        cut.unreadCount.launchIn(backgroundScope)

        continually(2.seconds) { cut.unreadCount.first() shouldBe null }
        timelineMock.addEvents { +messageEvent(sender = alice) { text("Unread message") } }
        eventually(3.seconds) { cut.unreadCount.first() shouldBe "1" }
    }

    @Test
    fun `unreadCount » count unread messages correctly when last read event changes`() = runTest {
        val timelineMock =
            timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) { text("Read message") }
                +messageEvent(sender = alice) { text("Unread message") }
                +messageEvent(sender = alice) { text("Unread message") }
            }

        timelineMock.fullyReadEventIndex.value = 0

        val cut = timelineViewModel()

        cut.unreadCount.launchIn(backgroundScope)

        eventually(3.seconds) { cut.unreadCount.first() shouldBe "2" }
        timelineMock.fullyReadEventIndex.value = 1
        eventually(3.seconds) { cut.unreadCount.first() shouldBe "1" }
        timelineMock.fullyReadEventIndex.value = 2
        eventually(3.seconds) { cut.unreadCount.first() shouldBe null }
    }

    @Test
    fun `unreadCount » show an indicator when there are more than 99 unread messages`() = runTest {
        val timelineMock =
            timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) { text("Read message") }
                (1..100).forEach { +messageEvent(sender = alice) { text("Unread message number $it") } }
            }

        timelineMock.fullyReadEventIndex.value = 0

        val cut = timelineViewModel()

        cut.unreadCount.launchIn(backgroundScope)

        eventually(3.seconds) { cut.unreadCount.first() shouldBe "99+" }
    }

    @Test
    fun `unreadCount » not count unsupported timeline events`() = runTest {
        val timelineMock =
            timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) { text("Read message") }
                +MessageEvent(
                    sender = alice,
                    id = EventId("1"),
                    roomId = roomId,
                    originTimestamp = 123,
                    content =
                        RoomMessageEventContent.Unknown(
                            "Unsupported Event",
                            body = "Unsupported",
                            raw = JsonObject(content = HashMap()),
                        ),
                )
            }

        timelineMock.fullyReadEventIndex.value = 0

        val cut = timelineViewModel()

        cut.unreadCount.launchIn(backgroundScope)

        continually(2.seconds) { cut.unreadCount.first() shouldBe null }

        timelineMock.addEvents {
            +MessageEvent(
                sender = alice,
                id = EventId("2"),
                roomId = roomId,
                originTimestamp = 234,
                content = RoomMessageEventContent.TextBased.Text("Supported message"),
            )
        }

        eventually(3.seconds) { cut.unreadCount.first() shouldBe "1" }

        timelineMock.fullyReadEventIndex.value = 1

        continually(2.seconds) { cut.unreadCount.first() shouldBe "1" }
    }

    @Test
    fun `mark room as read when opening`() = runTest {
        timeline(roomServiceMock, roomId) { +messageEvent(sender = alice) { text("Text message") } }
        every { roomServiceMock.getAccountData(any(), MarkedUnreadEventContent::class, any()) } returns
            flowOf(MarkedUnreadEventContent(true))
        var setRoomAsReadCalled = false
        everySuspend { roomsApiClientMock.setAccountData(MarkedUnreadEventContent(false), any(), any()) } calls
            {
                setRoomAsReadCalled = true
                Result.success(Unit)
            }
        val cut = timelineViewModel()
        cut.elements waitForSize 1

        cut.setViewState(
            TimelineViewModel.ViewState(
                firstVisibleElement = "notRelevant",
                lastVisibleElement = "$roomId-0",
                firstLoadedElement = "notRelevant",
                lastLoadedElement = "notRelevant",
                timelineIsFocused = true,
                finishedScrollTo = null,
            )
        )
        eventually(2.seconds) { setRoomAsReadCalled shouldBe true }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.timelineViewModel(onBackMock: () -> Unit = mock()): TimelineViewModelImpl {
        return TimelineViewModelImpl(
            viewModelContext =
                MatrixClientViewModelContextImpl(
                    componentContext = DefaultComponentContext(lifecycleRegistry),
                    di =
                        koinApplication {
                                modules(
                                    createTestDefaultTrixnityMessengerModules(
                                        mapOf(UserId("test", "server") to matrixClientMock)
                                    ) +
                                        module {
                                            single { clock }
                                            single<MatrixMessengerConfiguration> {
                                                MatrixMessengerConfiguration().apply {
                                                    timelineInitialSize = 10
                                                    timelineFetchSize = 20
                                                    timelineBuffer = 10
                                                    timelineMaxSize = 100
                                                }
                                            }
                                            single<RoomHeaderViewModelFactory> {
                                                object : RoomHeaderViewModelFactory {
                                                    override fun create(
                                                        viewModelContext: MatrixClientViewModelContext,
                                                        selectedRoomId: RoomId,
                                                        onBack: () -> Unit,
                                                        onVerifyUser: () -> Unit,
                                                        onOpenRoomSettings: () -> Unit,
                                                        onOpenUserProfile: (UserId) -> Unit,
                                                    ): RoomHeaderViewModel {
                                                        return roomHeaderViewModelMock
                                                    }
                                                }
                                            }
                                            single<InputAreaViewModelFactory> {
                                                object : InputAreaViewModelFactory {
                                                    override fun create(
                                                        viewModelContext: MatrixClientViewModelContext,
                                                        selectedRoomId: RoomId,
                                                        onMessageReplaceFinished: (RoomId, EventId) -> Unit,
                                                        onMessageReplyFinished: (RoomId, EventId) -> Unit,
                                                        onShowAttachmentSendView: (FileDescriptor) -> Unit,
                                                        onOpenMention: OpenMentionCallback,
                                                    ): InputAreaViewModel {
                                                        return inputAreaViewModelMock
                                                    }
                                                }
                                            }
                                        }
                                )
                            }
                            .koin,
                    userId = UserId("test", "server"),
                    coroutineContext = backgroundScope.coroutineContext,
                    name = "Timeline",
                ),
            roomId = roomId,
            onOpenSettings = mock(),
            onBack = onBackMock,
            onOpenMention = mock(),
            onOpenUserProfile = mock(),
            onOpenMetadata = mock(),
        )
    }
}
