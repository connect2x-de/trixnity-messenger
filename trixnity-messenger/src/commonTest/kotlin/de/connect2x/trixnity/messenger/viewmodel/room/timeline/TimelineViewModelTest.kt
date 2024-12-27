package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.eqNull
import de.connect2x.trixnity.messenger.firstWithClue
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.TextBasedRoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
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
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.DefaultAsserter.fail
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 10_000

    private lateinit var lifecycleRegistry: LifecycleRegistry

    private val roomId = RoomId("room1", "localhost")
    private val me = UserId("user1", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val userServiceMock = mock<UserService>()

    val matrixClientServerApiMock = mock<MatrixClientServerApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    val roomHeaderViewModelMock = mock<RoomHeaderViewModel>()

    val inputAreaViewModelMock = mock<InputAreaViewModel>()

    val clock = mock<Clock>()

    lateinit var coroutineScope: CoroutineScope

    private lateinit var syncStateMocker: BlockingAnsweringScope<StateFlow<SyncState>>
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
            resetMocks(
                matrixClientMock,
                roomServiceMock,
                userServiceMock,
                matrixClientServerApiMock,
                roomsApiClientMock,
                roomHeaderViewModelMock,
                inputAreaViewModelMock,
                clock
            )
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
            everySuspend {
                roomsApiClientMock.setReadMarkers(any(), any(), any(), any(), eqNull())
            } returns Result.success(Unit)
            everySuspend {
                roomsApiClientMock.setReceipt(any(), any(), any(), any(), eqNull())
            } returns Result.success(Unit)

            every { roomServiceMock.getOutbox() } returns outboxMessagesFlow.map {
                it.map { MutableStateFlow(it) }
            }.stateIn(coroutineScope)
            every { roomServiceMock.getOutbox(roomId = any()) } returns outboxMessagesFlow.map {
                it.filter { it.roomId == roomId }.map { MutableStateFlow(it) }
            }.stateIn(coroutineScope)
            every { userServiceMock.canRedactEvent(any(), any()) } returns flowOf(true)
            every { userServiceMock.canSendEvent(any(), any()) } returns flowOf(true)
            every { userServiceMock.getReceiptsById(any(), any()) } returns flowOf(null)

            every { roomServiceMock.getTimelineEvent(any(), any(), any()) } returns dummyEvent
            every { roomServiceMock.getNextTimelineEvent(any(), any()) } returns flowOf(null)
            every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns
                    MutableStateFlow(emptyMap())

            every {
                userServiceMock.getAll(roomId)
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
            every { userServiceMock.getAllReceipts(eq(roomId)) } returns MutableStateFlow(emptyMap())
            every {
                userServiceMock.getById(eq(roomId), any())
            } returns MutableStateFlow(null)
            every { userServiceMock.getById(eq(roomId), any()) } returns flowOf(null)
            everySuspend { userServiceMock.loadMembers(roomId, false) } returns Unit

            every { clock.now() } returns Instant.parse("2020-09-01T01:00:00.000Z")

            outboxMessagesFlow.value = listOf() // reset

            lifecycleRegistry = LifecycleRegistry()
            lifecycleRegistry.start()
        }

        afterTest {
            lifecycleRegistry.destroy()
            coroutineScope.cancel()
        }

        context(TimelineViewModel::elements.name) {
            should("show new messages when at the end of timeline (end of timeline has been starting element)") {
                val timelineMock = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) {
                        text("Hello")
                    }
                    +messageEvent(sender = alice) {
                        text("World")
                    }
                }

                val cut = timelineViewModel(mock())
                cut.elements waitForSize 2
                cut.viewState.value = TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "$roomId-1",
                    firstLoadedElement = "notRelevant",
                    lastLoadedElement = "notRelevant",
                    windowIsFocused = true
                )
                delay(200) // give the viewmodel time to compute derived values

                timelineMock.addEvents {
                    +messageEvent(sender = alice) {
                        text("Woohoo")
                    }
                }

                cut.elements waitForSize 3
                cut.elements.value.last().key shouldBe "$roomId-2"
            }
            should("only show outbox messages of this room") {
                outboxMessagesFlow.value =
                    listOf(
                        RoomOutboxMessage(
                            transactionId = "1",
                            roomId = RoomId("not this room", "localhost"),
                            content = RoomMessageEventContent.TextBased.Text(body = "Hello"),
                            createdAt = Instant.fromEpochMilliseconds(0)
                        ),
                        RoomOutboxMessage(
                            transactionId = "2",
                            roomId = roomId,
                            content = RoomMessageEventContent.TextBased.Text(body = "Right"),
                            createdAt = Instant.fromEpochMilliseconds(1)
                        ),
                        RoomOutboxMessage(
                            transactionId = "3",
                            roomId = RoomId("totally not this room", "localhost"),
                            content = RoomMessageEventContent.TextBased.Text(body = "from outer space"),
                            createdAt = Instant.fromEpochMilliseconds(2)
                        )
                    )
                val timelineMock = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) {
                        text("Hello")
                    }
                }
                val cut = timelineViewModel()

                cut.elements waitForSize 2  // 1 message + 1 outbox message
            }
            should("filter outbox message that is already in the timeline") {
                outboxMessagesFlow.value =
                    listOf(
                        RoomOutboxMessage(
                            transactionId = "1",
                            roomId = roomId,
                            content = RoomMessageEventContent.TextBased.Text(body = "Hello"),
                            createdAt = Instant.fromEpochMilliseconds(0)
                        ),
                        RoomOutboxMessage(
                            transactionId = "2",
                            roomId = roomId,
                            content = RoomMessageEventContent.TextBased.Text(body = "World"),
                            createdAt = Instant.fromEpochMilliseconds(1)
                        ),
                    )

                timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = me, transactionId = "1") {
                        text("Hello")
                    }
                }

                val cut = timelineViewModel()
                cut.elements waitForSize 2

                cut.elements.first() shouldHaveSize 2
            }
            should("add new outbox message and when it is received as timeline event from the server not show as outbox message") {
                val timelineMock = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = me, transactionId = "1") {
                        text("Hello")
                    }
                }
                val cut = timelineViewModel()
                cut.elements waitForSize 1

                outboxMessagesFlow.value = listOf(
                    RoomOutboxMessage(
                        transactionId = "transactionId-1",
                        roomId = roomId,
                        content = RoomMessageEventContent.TextBased.Text(body = "Hello"),
                        createdAt = Instant.fromEpochMilliseconds(0)
                    ),
                )
                cut.elements waitForSize 2

                delay(500.milliseconds)
                timelineMock.addEvents {
                    +messageEvent(sender = me, transactionId = "transactionId-1") {
                        text("Hello")
                    }
                }

                continually(2.seconds) {
                    cut.elements.first() shouldHaveSize 2
                    cut.elements.first()[1].key shouldBe "$roomId-transactionId-1"
                }
            }
            should("only contain the newest version of a replace event") {
                timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) {
                        text("Hello!!!")
                    }
                    +MessageEvent(
                        content = RoomMessageEventContent.TextBased.Text(
                            body = "Hello again.",
                            relatesTo = RelatesTo.Replace(EventId("0")),
                        ),
                        id = EventId("replace-1"),
                        sender = alice,
                        roomId = roomId,
                        originTimestamp = 1234,
                    )
                    +MessageEvent(
                        content = RoomMessageEventContent.TextBased.Text(
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
                val job = launch {
                    cut.elements.collect {}
                }
                cut.elements.first().lastOrNull()?.key shouldBe "${roomId.full}-replace-2"
                eventually(2.seconds) {
                    val elementViewModel = cut.elements.first().firstOrNull()?.element?.value
                    if (elementViewModel is TextBasedRoomMessageTimelineElementViewModel) {
                        elementViewModel.body shouldBe "Hello!!!"
                    } else fail("")
                    cut.elements.first().getOrNull(1)?.element?.value shouldBe TimelineElementViewModel.Empty
                    cut.elements.first().lastOrNull()?.element?.value shouldBe TimelineElementViewModel.Empty
                }

                job.cancel()
            }
        }
        context(TimelineViewModel::viewState.name) {
            should("load more messages before") {
                val timelineMock = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) {
                        text("Hello")
                    }
                    (1..19).forEach {
                        +messageEvent(sender = alice) {
                            text("World-$it")
                        }
                    }
                }

                val cut = timelineViewModel()
                cut.elements waitForSize 11

                // timeline starts at the end (no read messages) -> [9..19] are shown, if first visible is in the first 10 -> load before
                cut.viewState.value = TimelineViewModel.ViewState(
                    firstVisibleElement = "$roomId-9",
                    lastVisibleElement = "notRelevant",
                    firstLoadedElement = "notRelevant",
                    lastLoadedElement = "notRelevant",
                    windowIsFocused = true
                )
                cut.elements waitForSize 20
            }
            should("not load more messages before") {
                val timelineMock = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) {
                        text("Hello")
                    }
                    (1..19).forEach {
                        +messageEvent(sender = alice) {
                            text("World-$it")
                        }
                    }
                }

                val cut = timelineViewModel()
                cut.elements waitForSize 11

                cut.viewState.value = TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "$roomId-8",// [9..19], see above
                    firstLoadedElement = "notRelevant",
                    lastLoadedElement = "notRelevant",
                    windowIsFocused = true
                )
                continually(1.seconds) {
                    cut.elements.value.size shouldBe 11
                }
            }
            should("load more messages after") {
                val timelineMock = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) {
                        text("Hello")
                    }
                    (1..19).forEach {
                        +messageEvent(sender = alice) {
                            text("World-$it")
                        }
                    }
                }
                timelineMock.fullyReadEventIndex.value = 0

                val cut = timelineViewModel()
                cut.elements waitForSize 11

                // fully read events is set -> start at beginning -> [0..10], 9 is in last messages -> load after
                cut.viewState.value = TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "$roomId-9",
                    firstLoadedElement = "notRelevant",
                    lastLoadedElement = "notRelevant",
                    windowIsFocused = true
                )
                cut.elements waitForSize 20
            }
            should("load more messages after when outbox is last message") {
                val timelineMock = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) {
                        text("Hello")
                    }
                }
                timelineMock.fullyReadEventIndex.value = 0

                outboxMessagesFlow.value = listOf(
                    RoomOutboxMessage(
                        transactionId = "transactionId-1",
                        roomId = roomId,
                        content = RoomMessageEventContent.TextBased.Text(body = "Hello to you!"),
                        createdAt = Instant.fromEpochMilliseconds(0)
                    ),
                )

                val cut = timelineViewModel()
                cut.elements waitForSize 2

                cut.viewState.value = TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "$roomId-transactionId-1",
                    firstLoadedElement = "notRelevant",
                    lastLoadedElement = "notRelevant",
                    windowIsFocused = true
                )

                timelineMock.addEvents {
                    +messageEvent(sender = alice) {
                        text("Hello")
                    }
                }

                cut.elements waitForSize 3
            }
            should("not load more messages after") {
                val timelineMock = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) {
                        text("Hello")
                    }
                    (1..40).forEach {
                        +messageEvent(sender = alice) {
                            text("World-$it")
                        }
                    }
                }
                timelineMock.fullyReadEventIndex.value = 0

                val cut = timelineViewModel()
                cut.elements waitForSize 11

                // 0 is at beginning -> do NOT load after
                cut.viewState.value = TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "$roomId-0",
                    firstLoadedElement = "notRelevant",
                    lastLoadedElement = "notRelevant",
                    windowIsFocused = true
                )
                continually(1.seconds) {
                    cut.elements.value.size shouldBe 11
                }
            }
        }
        context(TimelineViewModel::jumpToEndOfTimeline.name) {
            should("directly jump to the end of the timeline if the last event is already in the timeline") {
                timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) {
                        text("Hello")
                    }
                    (1..9).forEach {
                        +messageEvent(sender = alice) {
                            text("World-$it")
                        }
                    }
                }

                val cut = timelineViewModel()
                cut.elements waitForSize 10

                cut.jumpToEndOfTimeline()
                cut.elements waitForSize 10
            }
            should("load the last event of the room and add it to the timeline if it is not yet present in the timeline") {
                val timelineMock = timeline(roomServiceMock, roomId) {
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
                timelineMock.fullyReadEventIndex.value = 0

                val cut = timelineViewModel()
                cut.elements waitForSize 11
                cut.elements.value.last().key shouldBe "$roomId-10"

                cut.jumpToEndOfTimeline()
                cut.elements waitForSize 11
                cut.elements.first { it.last().key == "$roomId-11" }
            }
        }
        context(TimelineViewModel::leaveRoom.name) {
            should("show an error message when trying to leave a room and we are not connected") {
                syncStateMocker returns MutableStateFlow(SyncState.ERROR)
                timeline(roomServiceMock, roomId) {}

                val cut = timelineViewModel()
                cut.leaveRoom()

                cut.error.filterNotNull().first()
                // we have not mocked roomsApiClientMock.leaveRoom() and onBackMock.invoke(), so if they would be called, an exception would be thrown

                cut.errorDismiss()
                cut.error.value shouldBe null
            }
            should("show an error message when leaving the room fails") {
                everySuspend {
                    roomsApiClientMock.leaveRoom(roomId, any(), eqNull())
                } returns Result.failure(RuntimeException("Oh no!"))

                timeline(roomServiceMock, roomId) {}

                val cut = timelineViewModel()
                cut.leaveRoom()

                // onBackMock is not mocked correctly, so if called, an exception would be thrown
                cut.error.filterNotNull().first()
            }
        }
        context(TimelineViewModel::scrollTo.name) {
            should("scroll to the end when we put a message in the outbox") {
                val timelineMock = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) {
                        text("Hello!")
                    }
                }
                val cut = timelineViewModel()
                val scrollToCalled =
                    cut.scrollTo.scan(listOf<String>()) { old, new -> old + new }.stateIn(coroutineScope)
                cut.elements waitForSize 1
                val coroutineScope = CoroutineScope(Dispatchers.Default)
                scrollToCalled.map { it.size }.firstWithClue { 1 } // initial scroll ("0")

                outboxMessagesFlow.value = listOf(
                    RoomOutboxMessage(
                        transactionId = "transactionId-1",
                        roomId = roomId,
                        content = RoomMessageEventContent.TextBased.Text(body = "Hello to you!"),
                        createdAt = Instant.fromEpochMilliseconds(0),
                    ),
                )
                cut.elements waitForSize 2
                scrollToCalled.firstWithClue(listOf("$roomId-0", "$roomId-transactionId-1"))
                outboxMessagesFlow.value = listOf(
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
                    )
                )
                cut.elements waitForSize 3
                scrollToCalled.firstWithClue(listOf("$roomId-0", "$roomId-transactionId-1", "$roomId-transactionId-2"))

                coroutineScope.cancel()
            }

            should("scroll to the end when a new message is added at the end of the timeline where the user is active") {
                val timelineMock = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) {
                        text("Hello!")
                    }
                }
                val cut = timelineViewModel()

                cut.elements waitForSize 1
                cut.viewState.value = TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "$roomId-0",
                    firstLoadedElement = "notRelevant",
                    lastLoadedElement = "notRelevant",
                    windowIsFocused = true,
                )
                delay(200) // give the viewmodel time to compute derived values

                val coroutineScope = CoroutineScope(Dispatchers.Default)
                val scrollToCalled =
                    cut.scrollTo.scan(listOf<String>()) { old, new -> old + new }.stateIn(coroutineScope)
                scrollToCalled.value.shouldBeEmpty()

                timelineMock.addEvents {
                    +messageEvent(sender = alice) {
                        text("World!")
                    }
                }

                cut.elements waitForSize 2
                scrollToCalled.firstWithClue(listOf("$roomId-1"))

                coroutineScope.cancel()
            }

            should("not scroll to the end when a new message is added, but the end of the timeline is not visible") {
                val timelineMock = timeline(roomServiceMock, roomId) {
                    +messageEvent(sender = alice) {
                        text("Hello!")
                    }
                    +messageEvent(sender = alice) {
                        text("World!")
                    }
                }
                val cut = timelineViewModel()

                cut.elements waitForSize 2

                cut.viewState.value = TimelineViewModel.ViewState(
                    firstVisibleElement = "notRelevant",
                    lastVisibleElement = "$roomId-0",
                    firstLoadedElement = "notRelevant",
                    lastLoadedElement = "notRelevant",
                    windowIsFocused = true
                )
                delay(500.milliseconds) // give scrollTo time to be cleared

                val coroutineScope = CoroutineScope(Dispatchers.Default)
                val scrollToCalled =
                    cut.scrollTo.scan(listOf<String>()) { old, new -> old + new }.stateIn(coroutineScope)
                scrollToCalled.value.shouldBeEmpty()

                // this will not trigger a creation of a viewmodel as we are not at the end of the timeline
                timelineMock.addEvents {
                    +messageEvent(sender = alice) {
                        text("Dino!")
                    }
                }

                continually(500.milliseconds) {
                    scrollToCalled.value.shouldBeEmpty()
                }

                coroutineScope.cancel()
            }
        }
    }

    private suspend fun timelineViewModel(
        onBackMock: () -> Unit = mock(),
    ): TimelineViewModel {
        Dispatchers.setMain(Dispatchers.Unconfined)
        return TimelineViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(lifecycleRegistry),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(
                                UserId("test", "server") to matrixClientMock
                            )
                        ) + module {
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
                                        isBackButtonVisible: MutableStateFlow<Boolean>,
                                        onBack: () -> Unit,
                                        onVerifyUser: () -> Unit,
                                        onShowRoomSettings: () -> Unit,
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
                                        onOpenMention: OpenMentionCallback
                                    ): InputAreaViewModel {
                                        return inputAreaViewModelMock
                                    }
                                }
                            }
                        })
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = currentCoroutineContext(),
            ),
            roomId = roomId,
            isBackButtonVisible = MutableStateFlow(false),
            onShowSettings = mock(),
            onBack = onBackMock,
            onOpenMention = mock(),
            onOpenMetadata = mock(),
        )
    }
}
