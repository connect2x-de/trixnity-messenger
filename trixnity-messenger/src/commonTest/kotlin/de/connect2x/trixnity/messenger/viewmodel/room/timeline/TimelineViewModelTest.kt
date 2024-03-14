package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import com.benasher44.uuid.uuid4
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.retry
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.RoomOutboxMessage
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.UnknownEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.kodein.mock.mockFunction4
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 5_000

    private val mocker = Mocker()

    private lateinit var lifecycleRegistry: LifecycleRegistry

    private val roomId = RoomId("room1", "localhost")
    private val me = UserId("user1", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var userServiceMock: UserService

    @Mock
    lateinit var matrixClientServerApiMock: MatrixClientServerApiClient

    @Mock
    lateinit var roomsApiClientMock: RoomApiClient

    @Mock
    lateinit var roomHeaderViewModelMock: RoomHeaderViewModel

    @Mock
    lateinit var inputAreaViewModelMock: InputAreaViewModel

    @Mock
    lateinit var clock: Clock

    lateinit var coroutineScope: CoroutineScope

    private lateinit var syncStateMocker: Mocker.Every<StateFlow<SyncState>>
    private val outboxMessagesFlow = MutableStateFlow(emptyList<RoomOutboxMessage<*>>())

    private val dummyEvent = flowOf(
        TimelineEvent(
            messageEvent(
                sender = alice,
                roomId = roomId,
                eventId = EventId("dummy")
            ) { text("dummy") },
            gap = null,
            previousEventId = null,
            nextEventId = null
        )
    )

    init {
        Dispatchers.setMain(Dispatchers.Unconfined)
        beforeTest {
            coroutineScope = CoroutineScope(Dispatchers.Default)
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                            single { userServiceMock }
                        }
                    )
                }.koin
                every { matrixClientMock.api } returns matrixClientServerApiMock
                every { matrixClientMock.userId } returns me

                syncStateMocker = every { matrixClientMock.syncState }
                syncStateMocker returns MutableStateFlow(SyncState.STARTED)

                every { matrixClientServerApiMock.room } returns roomsApiClientMock
                everySuspending {
                    roomsApiClientMock.setReadMarkers(isAny(), isAny(), isAny(), isAny(), isNull())
                } returns Result.success(Unit)
                everySuspending {
                    roomsApiClientMock.setReceipt(isAny(), isAny(), isAny(), isNull())
                } returns Result.success(Unit)

                every { roomServiceMock.getOutbox() } returns outboxMessagesFlow
                    .map { it.associate { it.transactionId to MutableStateFlow(it) } }
                    .stateIn(coroutineScope)
                every { userServiceMock.canRedactEvent(isAny(), isAny()) } returns flowOf(true)
                every { userServiceMock.canSendEvent(isAny(), isAny()) } returns flowOf(true)

                every { roomServiceMock.getTimelineEvent(isAny(), isAny(), isAny()) } returns
                        dummyEvent

                every {
                    userServiceMock.getAll(isEqual(roomId))
                } returns MutableStateFlow(
                    mapOf(
                        me to flowOf(
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
                                    stateKey = ""
                                )
                            )
                        ),
                        alice to flowOf(
                            RoomUser(
                                roomId, alice, "Alice", StateEvent(
                                    MemberEventContent(membership = Membership.JOIN),
                                    EventId(""),
                                    alice,
                                    roomId,
                                    0L,
                                    stateKey = ""
                                )
                            )
                        ),
                        bob to flowOf(
                            RoomUser(
                                roomId, bob, "Bob", StateEvent(
                                    MemberEventContent(membership = Membership.JOIN),
                                    EventId(""),
                                    bob,
                                    roomId,
                                    0L,
                                    stateKey = ""
                                )
                            )
                        ),
                    )
                )
                every { userServiceMock.getAllReceipts(isEqual(roomId)) } returns MutableStateFlow(emptyMap())
                every {
                    userServiceMock.getById(isEqual(roomId), isAny())
                } returns MutableStateFlow(null)
                every { userServiceMock.getById(isEqual(roomId), isAny()) } returns flowOf(null)
                everySuspending { userServiceMock.loadMembers(roomId, false) } returns Unit

                every { clock.now() } returns Instant.parse("2020-09-01T01:00:00.000Z")
            }

            outboxMessagesFlow.value = listOf() // reset

            lifecycleRegistry = LifecycleRegistry()
            lifecycleRegistry.start()
        }

        afterTest {
            lifecycleRegistry.destroy()
            coroutineScope.cancel()
        }

        should("show new messages when at the end of timeline (end of timeline has been starting element)") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +messageEvent(sender = alice) {
                    text("World")
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()

            val cut = timelineViewModel(mockFunction0(mocker))
            cut.timelineElementHolderViewModels.first { it.size == 2 }
            cut.lastVisibleTimelineElement.value = "1"
            delay(200) // give the viewmodel time to compute derived values

            timelineMock.addEvents {
                +messageEvent(sender = alice) {
                    text("Woohoo")
                }
            }

            cut.timelineElementHolderViewModels.first { it.size == 3 }
            cut.timelineElementHolderViewModels.value.last().key shouldBe "2"
        }

        should("load more messages before") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                (1..19).forEach {
                    +messageEvent(sender = alice) {
                        text("World-$it")
                    }
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()

            val cut = timelineViewModel(mockFunction0(mocker))
            withClue(lazy { "timelineElementViewModels size was ${cut.timelineElementHolderViewModels.value.size}, expected 11" }) {
                cut.timelineElementHolderViewModels.first { it.size == 11 }
            }

            // timeline starts at the end (no read messages) -> [9..19] are shown, if first visible is in the first 10 -> load before
            cut.firstVisibleTimelineElement.value = "9"
            cut.timelineElementHolderViewModels waitForSize 20
        }

        should("not load more messages before") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                (1..19).forEach {
                    +messageEvent(sender = alice) {
                        text("World-$it")
                    }
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()

            val cut = timelineViewModel(mockFunction0(mocker))
            cut.timelineElementHolderViewModels waitForSize 11

            cut.firstVisibleTimelineElement.value = "8" // [9..19], see above
            continually(1.seconds) {
                cut.timelineElementHolderViewModels.value.size shouldBe 11
            }
        }

        should("load more messages after") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                (1..19).forEach {
                    +messageEvent(sender = alice) {
                        text("World-$it")
                    }
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()
            timelineMock.fullyReadEventIndex.value = 0

            val cut = timelineViewModel(mockFunction0(mocker))
            cut.timelineElementHolderViewModels waitForSize 11

            // fully read events is set -> start at beginning -> [0..10], 9 is in last messages -> load after
            cut.lastVisibleTimelineElement.value = "9"
            cut.timelineElementHolderViewModels waitForSize 20
        }

        should("not load more messages after") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                (1..19).forEach {
                    +messageEvent(sender = alice) {
                        text("World-$it")
                    }
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()
            timelineMock.fullyReadEventIndex.value = 0

            val cut = timelineViewModel(mockFunction0(mocker))
            cut.timelineElementHolderViewModels waitForSize 11

            // see above, [0..10], 1 is at beginning -> do NOT load after
            cut.lastVisibleTimelineElement.value = "1"
            continually(1.seconds) {
                cut.timelineElementHolderViewModels.value.size shouldBe 11
            }
        }

        should("directly jump to the end of the timeline if the last event is already in the timeline") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                (1..9).forEach {
                    +messageEvent(sender = alice) {
                        text("World-$it")
                    }
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()

            val cut = timelineViewModel(mockFunction0(mocker))
            cut.timelineElementHolderViewModels waitForSize 10

            cut.jumpToEndOfTimeline()
            cut.timelineElementHolderViewModels waitForSize 10
        }

        should("load the last event of the room and add it to the timeline if it is not yet present in the timeline") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                (1..10).forEach {
                    +messageEvent(sender = alice) {
                        text("World-$it")
                    }
                }

                +messageEvent(sender = alice) {
                    text("latest")
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()
            timelineMock.fullyReadEventIndex.value = 0

            val cut = timelineViewModel(mockFunction0(mocker))
            cut.timelineElementHolderViewModels waitForSize 11
            cut.timelineElementHolderViewModels.value.last().key shouldBe "10"

            cut.jumpToEndOfTimeline()
            cut.timelineElementHolderViewModels waitForSize 11
            cut.timelineElementHolderViewModels.first { it.last().key == "11" }
        }

        // this test does flicker from time to time, so deactivate in the meantime; it is unclear whether this is a mocKMP or trixnity-messenger problem
//        should("go back to the room list view when leaving the room successfully") {
//            mocker.everySuspending {
//                roomsApiClientMock.leaveRoom(
//                    isEqual(roomId),
//                    isAny(),
//                    isNull()
//                )
//            } returns
//                    Result.success(Unit)
//            timeline(mocker, roomServiceMock, roomId) {}
//            val onBackMock = mockFunction0(mocker) {}
//            val cut = timelineViewModel(onBackMock)
//
//            cut.leaveRoom()
//
//            eventually(3.seconds) {
//                try {
//                    mocker.verifyWithSuspend(exhaustive = false, inOrder = false) {
//                        roomsApiClientMock.leaveRoom(isEqual(roomId), isAny(), isNull())
//                    }
//                } catch (npe: NullPointerException) { // this is due to mocKMP throwing an NPE from time to time...
//                    println("Mocker threw NPE: $npe")
//                }
//            }
//            mocker.verify(exhaustive = false) { called { onBackMock.invoke() } }
//        }

        should("show an error message when trying to leave a room and we are not connected") {
            syncStateMocker returns MutableStateFlow(SyncState.ERROR)
            timeline(mocker, roomServiceMock, roomId) {}

            val cut = timelineViewModel()
            cut.leaveRoom()

            cut.error.filterNotNull().first()
            // we have not mocked roomsApiClientMock.leaveRoom() and onBackMock.invoke(), so if they would be called, an exception would be thrown

            cut.errorDismiss()
            cut.error.value shouldBe null
        }

        should("show an error message when leaving the room fails") {
            mocker.everySuspending {
                roomsApiClientMock.leaveRoom(
                    isEqual(roomId),
                    isAny(),
                    isNull()
                )
            } returns Result.failure(RuntimeException("Oh no!"))

            timeline(mocker, roomServiceMock, roomId) {}

            val cut = timelineViewModel()
            cut.leaveRoom()

            // onBackMock is not mocked correctly, so if called, an exception would be thrown
            cut.error.filterNotNull().first()
        }

        should("only show outbox messages of this room") {
            outboxMessagesFlow.value =
                listOf(
                    RoomOutboxMessage(
                        transactionId = "1",
                        roomId = RoomId("not this room", "localhost"),
                        content = RoomMessageEventContent.TextBased.Text(body = "Hello"),
                    ),
                    RoomOutboxMessage(
                        transactionId = "2",
                        roomId = roomId,
                        content = RoomMessageEventContent.TextBased.Text(body = "Right")
                    ),
                    RoomOutboxMessage(
                        transactionId = "3",
                        roomId = RoomId("totally not this room", "localhost"),
                        content = RoomMessageEventContent.TextBased.Text(body = "from outer space")
                    )
                )
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()
            val cut = timelineViewModel()

            cut.timelineElementHolderViewModels waitForSize 2  // 1 message + 1 outbox message
        }

        should("not show the date above an encrypted message if the message before is of the same day") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(sender = alice, sentAt = Instant.parse("2020-01-01T03:30:00.000Z")) {
                    text("Hello")
                }
                +messageEvent(sender = alice, sentAt = Instant.parse("2020-01-01T03:40:00.000Z")) {
                    encrypted()
                }
                +messageEvent(sender = alice, sentAt = Instant.parse("2020-01-01T04:00:00.000Z")) {
                    text("World")
                }
                +messageEvent(sender = alice, sentAt = Instant.parse("2020-01-01T04:01:00.000Z")) {
                    encrypted()
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()
            timelineMock.mockRoomServiceTimelineEventCalls()

            val cut = timelineViewModel()
            val result = cut.timelineElementHolderViewModels waitForSize 4

            result[0].timelineElementViewModel.value?.showDateAbove shouldBe true // Hello
            result[1].timelineElementViewModel.value?.showDateAbove shouldBe false // encrypted event
            result[2].timelineElementViewModel.value?.showDateAbove shouldBe false // World
            result[3].timelineElementViewModel.value?.showDateAbove shouldBe false // encrypted event
        }

        should("show date above first outgoing message when no received message is in the timeline yet") {
            outboxMessagesFlow.value =
                listOf(
                    RoomOutboxMessage(
                        transactionId = "1",
                        roomId = roomId,
                        content = RoomMessageEventContent.TextBased.Text(body = "Hello World")
                    )
                )
            timeline(mocker, roomServiceMock, roomId) {}

            val cut = timelineViewModel()

            val result = cut.timelineElementHolderViewModels waitForSize 1
            retry(10, 1_000.milliseconds, 100.milliseconds) {
                result[0].timelineElementViewModel.value?.showDateAbove shouldBe true
            }
        }

        should("show date above first outgoing message when the last received message is from another day") {
            outboxMessagesFlow.value =
                listOf(
                    RoomOutboxMessage(
                        transactionId = "1",
                        roomId = roomId,
                        content = RoomMessageEventContent.TextBased.Text(body = "Hello World")
                    )
                )
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-01-01T04:00:00.000Z")
                ) {
                    text("Hello")
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()

            val cut = timelineViewModel()
            val result = cut.timelineElementHolderViewModels waitForSize 2

            result[0].timelineElementViewModel.value?.showDateAbove shouldBe true // outbox
            result[1].timelineElementViewModel.value?.showDateAbove shouldBe true // timeline
        }

        should("not show the date above first outgoing message when the last received message is from today") {
            outboxMessagesFlow.value =
                listOf(
                    RoomOutboxMessage(
                        transactionId = "1",
                        roomId = roomId,
                        content = RoomMessageEventContent.TextBased.Text(body = "Hello World")
                    )
                )
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-09-01T04:00:00.000Z")
                ) {
                    text("Hello")
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()

            val cut = timelineViewModel()
            val result = cut.timelineElementHolderViewModels waitForSize 2

            withClue("show date above last timeline event") {
                result[0].timelineElementViewModel.first { it?.showDateAbove == true }
            }
            withClue("not show data above outbox") {
                result[1].timelineElementViewModel.first { it?.showDateAbove == false }
            }
        }

        should("not show the date above a message when the following message is from the same day") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-08-01T11:00:00.000Z")
                ) {
                    text("Hello")
                }
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-08-01T11:20:00.000Z")
                ) {
                    text("World")
                }
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-08-01T11:40:00.000Z")
                ) {
                    text("!")
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()

            val cut = timelineViewModel()
            val result = cut.timelineElementHolderViewModels waitForSize 3

            result[0].timelineElementViewModel.value?.showDateAbove shouldBe true // Hello
            result[1].timelineElementViewModel.value?.showDateAbove shouldBe false // World
            result[2].timelineElementViewModel.value?.showDateAbove shouldBe false // !
        }

        should("not show the date above an unknown message event type when the following message is from the same day") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-08-01T11:00:00.000Z")
                ) {
                    text("Hello")
                }
                +stateEvent(sender = alice, sentAt = Instant.parse("2020-08-01T11:20:00.000Z")) {
                    unknownEvent()
                }
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-08-01T11:40:00.000Z")
                ) {
                    text("World")
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()

            val cut = timelineViewModel()
            val result = cut.timelineElementHolderViewModels waitForSize 3

            result[0].timelineElementViewModel.value?.showDateAbove shouldBe true // Hello
            result[1].timelineElementViewModel.value?.showDateAbove shouldBe false // unknown
            result[2].timelineElementViewModel.value?.showDateAbove shouldBe false // World
        }

        should("ignore redact messages as they are not displayed") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-08-01T11:00:00.000Z")
                ) {
                    redacted()
                }
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-08-02T12:00:00.000Z")
                ) {
                    redact(EventId("0"))
                }
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-08-02T12:45:00.000Z")
                ) {
                    text("World!")
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()

            val cut = timelineViewModel()
            val result = cut.timelineElementHolderViewModels waitForSize 3

            result[2].timelineElementViewModel.value?.showDateAbove shouldBe true // -redacted-
            result[1].timelineElementViewModel.value?.showDateAbove shouldBe false // redaction (NTLEVM is always false)
            result[0].timelineElementViewModel.value?.showDateAbove shouldBe true // World!
        }

        should("set sticky data to first visible message") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-08-01T11:00:00.000Z")
                ) {
                    redacted()
                }
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-08-02T12:00:00.000Z")
                ) {
                    redact(EventId("0"))
                }
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-08-03T13:00:00.000Z")
                ) {
                    text("World!")
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()

            val cut = timelineViewModel()
            cut.firstVisibleTimelineElement.value = "1"
            cut.timelineElementHolderViewModels waitForSize 3
            cut.stickyDate.first { it == "03.08.2020" }
        }

        should("show the sticky date for an encrypted event, even if the underlying event is not rendered") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-08-01T11:00:00.000Z")
                ) {
                    text("Hello")
                }
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-08-02T12:00:00.000Z")
                ) {
                    encrypted()
                } withContent Result.success(
                    UnknownEventContent(
                        raw = JsonObject(mapOf("dino" to JsonPrimitive("unicorn"))),
                        eventType = "relation"
                    )
                )
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-08-03T13:00:00.000Z")
                ) {
                    text("World!")
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()

            val cut = timelineViewModel()
            cut.firstVisibleTimelineElement.value = "1"
            cut.timelineElementHolderViewModels waitForSize 3
            cut.stickyDate.first { it == "02.08.2020" }

            // after the decryption, the UI would recognize that it does not need to render the unknown event
            cut.firstVisibleTimelineElement.value = "2"
            cut.stickyDate.first { it == "03.08.2020" }
        }

        should("correctly show the sticky date for a message that has a transaction id (since it was sent by us)") {
            val transactionId = uuid4().toString()
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-08-01T11:00:00.000Z"),
                ) {
                    text("Hello")
                }
                +messageEvent(
                    sender = me,
                    sentAt = Instant.parse("2020-08-02T12:00:00.000Z"),
                    transactionId = transactionId,
                ) {
                    text("Hi")
                }
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-08-03T12:00:00.000Z"),
                ) {
                    text("How are you?")
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()

            val cut = timelineViewModel()
            cut.firstVisibleTimelineElement.value = transactionId
            cut.timelineElementHolderViewModels waitForSize 3
            cut.stickyDate.first { it == "02.08.2020" }
        }

        should("show a bubble edge only for the first outbox element") {
            outboxMessagesFlow.value =
                listOf(
                    RoomOutboxMessage(
                        transactionId = "1",
                        roomId = roomId,
                        content = RoomMessageEventContent.TextBased.Text(body = "Hello"),
                    ),
                    RoomOutboxMessage(
                        transactionId = "2",
                        roomId = roomId,
                        content = RoomMessageEventContent.TextBased.Text(body = "World")
                    ),
                )

            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(
                    sender = alice,
                    sentAt = Instant.parse("2020-01-01T04:00:00.000Z")
                ) {
                    text("Hello")
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()

            val cut = timelineViewModel()

            val result = cut.timelineElementHolderViewModels waitForSize 3  // 1 message + 2 outbox
            continually(2.seconds) {
                (result[1].timelineElementViewModel.value as RoomMessageViewModel).showChatBubbleEdge shouldBe true
                (result[2].timelineElementViewModel.value as RoomMessageViewModel).showChatBubbleEdge shouldBe false
            }
        }

        should("filter outbox message that is already in the timeline") {
            outboxMessagesFlow.value =
                listOf(
                    RoomOutboxMessage(
                        transactionId = "1",
                        roomId = roomId,
                        content = RoomMessageEventContent.TextBased.Text(body = "Hello"),
                    ),
                    RoomOutboxMessage(
                        transactionId = "2",
                        roomId = roomId,
                        content = RoomMessageEventContent.TextBased.Text(body = "World")
                    ),
                )

            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(sender = me, transactionId = "1") {
                    text("Hello")
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 2

            cut.timelineElementHolderViewModels.first() shouldHaveSize 2
        }

        should("add new outbox message and when it is received as timeline event from the server not show as outbox message") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {}
            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels.value shouldHaveSize 0

            outboxMessagesFlow.value = listOf(
                RoomOutboxMessage(
                    transactionId = "transactionId-1",
                    roomId = roomId,
                    content = RoomMessageEventContent.TextBased.Text(body = "Hello"),
                ),
            )
            cut.timelineElementHolderViewModels waitForSize 1

            delay(500.milliseconds)
            timelineMock.addEvents {
                +messageEvent(sender = me, transactionId = "transactionId-1") {
                    text("Hello")
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()

            continually(2.seconds) {
                cut.timelineElementHolderViewModels.first() shouldHaveSize 1
                cut.timelineElementHolderViewModels.first()[0].key shouldBe "transactionId-1"
            }
        }

        should("scroll to the end when we put a message in the outbox") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello!")
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()
            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 1
            val coroutineScope = CoroutineScope(Dispatchers.Default)
            val scrollToCalled = cut.scrollTo.scan(listOf<String>()) { old, new -> old + new }.stateIn(coroutineScope)
            scrollToCalled.value.shouldBeEmpty()

            outboxMessagesFlow.value = listOf(
                RoomOutboxMessage(
                    transactionId = "transactionId-1",
                    roomId = roomId,
                    content = RoomMessageEventContent.TextBased.Text(body = "Hello to you!"),
                ),
            )
            cut.timelineElementHolderViewModels waitForSize 2
            scrollToCalled.first { it == listOf("transactionId-1") }
            outboxMessagesFlow.value = listOf(
                RoomOutboxMessage(
                    transactionId = "transactionId-1",
                    roomId = roomId,
                    content = RoomMessageEventContent.TextBased.Text(body = "Hello to you!"),
                ),
                RoomOutboxMessage(
                    transactionId = "transactionId-2",
                    roomId = roomId,
                    content = RoomMessageEventContent.TextBased.Text(body = "My second message.")
                )
            )
            cut.timelineElementHolderViewModels waitForSize 3
            scrollToCalled.onEach { println(it) }.first { it == listOf("transactionId-1", "transactionId-2") }

            coroutineScope.cancel()
        }

        should("scroll to the end when a new message is added at the end of the timeline where the user is active") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello!")
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()
            val cut = timelineViewModel()

            cut.timelineElementHolderViewModels waitForSize 1
            cut.windowIsFocused.value = true
            cut.lastVisibleTimelineElement.value = "0"
            delay(200) // give the viewmodel time to compute derived values

            val coroutineScope = CoroutineScope(Dispatchers.Default)
            val scrollToCalled = cut.scrollTo.scan(listOf<String>()) { old, new -> old + new }.stateIn(coroutineScope)
            scrollToCalled.value.shouldBeEmpty()

            timelineMock.addEvents {
                +messageEvent(sender = alice) {
                    text("World!")
                }
            }

            cut.timelineElementHolderViewModels waitForSize 2
            scrollToCalled.first { it == listOf("1") }

            coroutineScope.cancel()
        }

        should("not scroll to the end when a new message is added, but the end of the timeline is not visible") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello!")
                }
                +messageEvent(sender = alice) {
                    text("World!")
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()
            val cut = timelineViewModel()

            cut.timelineElementHolderViewModels waitForSize 2

            cut.lastVisibleTimelineElement.value = "0"
            cut.windowIsFocused.value = true
            delay(500.milliseconds) // give scrollTo time to be cleared

            val coroutineScope = CoroutineScope(Dispatchers.Default)
            val scrollToCalled = cut.scrollTo.scan(listOf<String>()) { old, new -> old + new }.stateIn(coroutineScope)
            scrollToCalled.value.shouldBeEmpty()

            // this will not trigger a creation of a viewmodel as we are not at the end of the timeline
            timelineMock.addEvents {
                +messageEvent(sender = alice) {
                    text("Dino!")
                }
            }
            timelineMock.mockRoomServiceTimelineEventCalls()

            continually(500.milliseconds) {
                scrollToCalled.value.shouldBeEmpty()
            }

            coroutineScope.cancel()
        }
    }

    private fun timelineViewModel(
        onBackMock: () -> Unit = mockFunction0(mocker),
    ) =
        TimelineViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(lifecycleRegistry),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(
                                UserId(
                                    "test",
                                    "server"
                                ) to matrixClientMock
                            )
                        ) + module {
                            single { clock }
                            single<RoomHeaderViewModelFactory> {
                                object : RoomHeaderViewModelFactory {
                                    override fun create(
                                        viewModelContext: MatrixClientViewModelContext,
                                        selectedRoomId: RoomId,
                                        isBackButtonVisible: MutableStateFlow<Boolean>,
                                        onBack: () -> Unit,
                                        onVerifyUser: () -> Unit,
                                        onShowRoomSettings: () -> Unit
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
                                        onMessageEditFinished: (EventId) -> Unit,
                                        onMessageReplyToFinished: (EventId) -> Unit,
                                        onShowAttachmentSendView: (file: FileDescriptor) -> Unit
                                    ): InputAreaViewModel {
                                        return inputAreaViewModelMock
                                    }
                                }
                            }
                        })
                }.koin,
                userId = UserId("test", "server"),
            ),
            selectedRoomId = roomId,
            isBackButtonVisible = MutableStateFlow(false),
            onShowSettings = mockFunction0(mocker),
            onBack = onBackMock,
            onOpenModal = mockFunction4(mocker),
        )

    private fun TimelineMock.mockRoomServiceTimelineEventCalls() {
        eventsInStore.value.reversed().windowed(2, partialWindows = true) { window ->
            mocker.every {
                roomServiceMock.getPreviousTimelineEvent(isEqual(window[0].value), isAny())
            } returns window.getOrNull(1)
        }
        mocker.every {
            roomServiceMock.getPreviousTimelineEvent(isAny(), isAny())
        } runs {
//            println("!!!!!!!!!!!!!!!") // just for debugging tests
//            println("getPreviousTimelineEvent call with ${(it[0] as TimelineEvent).eventId} has not been handled explicitly")
//            println("!!!!!!!!!!!!!!!")
            null
        }
    }
}