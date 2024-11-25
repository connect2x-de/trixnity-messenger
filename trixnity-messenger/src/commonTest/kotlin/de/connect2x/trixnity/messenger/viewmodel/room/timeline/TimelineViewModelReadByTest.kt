package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import de.connect2x.trixnity.messenger.eqNull
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModelReadByTest : ShouldSpec() {
    override fun timeout(): Long = 5_000

    private lateinit var lifecycleRegistry: LifecycleRegistry

    private val roomId = RoomId("room1", "localhost")
    private val me = UserId("user1", "localhost")
    private val alice = UserId("alice", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val userServiceMock = mock<UserService>()

    val matrixClientServerApiMock = mock<MatrixClientServerApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    val downloadManagerMock = mock<DownloadManager>()

    val roomHeaderViewModelMock = mock<RoomHeaderViewModel>()

    val inputAreaViewModelMock = mock<InputAreaViewModel>()

    init {
        Dispatchers.setMain(Dispatchers.Unconfined)
        beforeTest {
            resetMocks(
                matrixClientMock,
                roomServiceMock,
                userServiceMock,
                matrixClientServerApiMock,
                roomsApiClientMock,
                downloadManagerMock,
                roomHeaderViewModelMock,
                inputAreaViewModelMock
            )

            lifecycleRegistry = LifecycleRegistry()
            lifecycleRegistry.start()


            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock }
                        single { userServiceMock }
                    }
                )
            }.koin
            every { matrixClientMock.userId } returns me
            every { matrixClientMock.syncState } returns MutableStateFlow(SyncState.STARTED)
            every { matrixClientMock.api } returns matrixClientServerApiMock

            every { matrixClientServerApiMock.room } returns roomsApiClientMock
            everySuspend {
                roomsApiClientMock.setReadMarkers(any(), any(), any(), any(), eqNull())
            } returns Result.success(Unit)

            every { roomServiceMock.getOutbox() } returns MutableStateFlow(listOf())
            every { roomServiceMock.getOutbox(roomId = any()) } returns MutableStateFlow(listOf())
            every { userServiceMock.canRedactEvent(any(), any()) } returns flowOf(true)
            every { userServiceMock.canSendEvent(any(), any()) } returns flowOf(true)

            val dummyEvent = flowOf(
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
            every { roomServiceMock.getTimelineEvent(any(), any(), any()) } returns
                    dummyEvent
            every { roomServiceMock.getPreviousTimelineEvent(any(), any()) } returns
                    dummyEvent
            every { roomServiceMock.getNextTimelineEvent(any(), any()) } returns flowOf(null)
            every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns
                    MutableStateFlow(emptyMap())

            every { userServiceMock.getById(eq(roomId), any()) } returns
                    MutableStateFlow(null)
            everySuspend { userServiceMock.loadMembers(roomId, false) } returns Unit
        }
        afterTest {
            lifecycleRegistry.destroy()
        }

        should("return empty list if my message has been read by no one else") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                (1..9).forEach {
                    +messageEvent(sender = alice) {
                        text("World-$it")
                    }
                }
            }
            timelineMock.fullyReadEventIndex.value = 0
            timelineMock.mockGetTimelineEventsFromLast()
            roomUsers(userServiceMock, roomId) {}

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 10
            cut.timelineElementHolderViewModels.value.first()
                .shouldBeInstanceOf<TimelineElementHolderViewModel>().isReadBy() shouldBe emptyList()
        }

        should("return list of users that have my message as their last read message") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                (1..9).forEach {
                    +messageEvent(sender = alice) {
                        text("World-$it")
                    }
                }
            }
            timelineMock.fullyReadEventIndex.value = 0
            timelineMock.mockGetTimelineEventsFromLast()
            roomUsers(userServiceMock, roomId) {
                +roomUser("1", lastReadMessage = EventId("0"))
                +roomUser("2", lastReadMessage = EventId("0"))
            }

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 10
            cut.timelineElementHolderViewModels.value.first()
                .shouldBeInstanceOf<TimelineElementHolderViewModel>().isReadBy() shouldBe listOf("1", "2")
            // bug: the second time, the users were added again
            cut.timelineElementHolderViewModels.value.first()
                .shouldBeInstanceOf<TimelineElementHolderViewModel>().isReadBy() shouldBe listOf("1", "2")
        }

        should("return list of users that have read one of the last 5 messages in the timeline (that are all users in the room)") {
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
            timelineMock.mockGetTimelineEventsFromLast()
            roomUsers(userServiceMock, roomId) {
                +roomUser("1", lastReadMessage = EventId("17"))
                +roomUser("2", lastReadMessage = EventId("18"))
                +roomUser("3", lastReadMessage = EventId("19"))
            }

            val cut = timelineViewModel()
            // even though there are more timeline events available, initially only show 11
            cut.timelineElementHolderViewModels waitForSize 11

            cut.timelineElementHolderViewModels.value.first()
                .shouldBeInstanceOf<TimelineElementHolderViewModel>().isReadBy() shouldBe listOf("1", "2", "3")
        }

        should("increase the number of messages to be considered to 30 until our message is searched") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                (1..29).forEach {
                    +messageEvent(sender = alice) {
                        text("World-$it")
                    }
                }
            }
            timelineMock.fullyReadEventIndex.value = 0
            timelineMock.mockGetTimelineEventsFromLast()
            roomUsers(userServiceMock, roomId) {
                +roomUser("1", lastReadMessage = EventId("7"))
                +roomUser("2", lastReadMessage = EventId("8"))
                +roomUser("3", lastReadMessage = EventId("9"))
                +roomUser("noReader", lastReadMessage = null)
            }

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 11

            cut.timelineElementHolderViewModels.value.first()
                .shouldBeInstanceOf<TimelineElementHolderViewModel>().isReadBy() shouldBe listOf("1", "2", "3")
        }

        should("stop at 11 users that read my message") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                (1..9).forEach {
                    +messageEvent(sender = alice) {
                        text("World-$it")
                    }
                }
            }
            timelineMock.fullyReadEventIndex.value = 0
            timelineMock.mockGetTimelineEventsFromLast()
            roomUsers(userServiceMock, roomId) {
                (1..12).forEach {
                    +roomUser("$it", lastReadMessage = EventId("9"))
                }
            }

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 10

            cut.timelineElementHolderViewModels.value.first()
                .shouldBeInstanceOf<TimelineElementHolderViewModel>()
                .isReadBy() shouldBe listOf("1", "10", "11", "2", "3", "4", "5", "6", "7", "8", "9")
        }

        should("not consider more than 100 messages for the computation of the read by users list") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                (1..109).forEach {
                    +messageEvent(sender = alice) {
                        text("World-$it")
                    }
                }
            }
            timelineMock.fullyReadEventIndex.value = 0
            timelineMock.mockGetTimelineEventsFromLast()
            roomUsers(userServiceMock, roomId) {
                +roomUser("readOnlyFirstMessage", lastReadMessage = EventId("0"))
                (1..5).forEach {
                    +roomUser("$it", lastReadMessage = EventId("109"))
                }
            }

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 11

            cut.timelineElementHolderViewModels.value.first()
                .shouldBeInstanceOf<TimelineElementHolderViewModel>().isReadBy() shouldBe listOf("1", "2", "3", "4", "5")
        }

        should("only mark own messages as read that have at least one message of someone else following") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(me) {
                    text("Hello")
                }
                +messageEvent(alice) {
                    text("World!")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0
            timelineMock.mockGetTimelineEventsFromLast()
            roomUsers(userServiceMock, roomId) {
                +roomUser(name = "Alice", lastReadMessage = EventId("1"))
            }

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 2
            val viewModelMessage2 = cut.timelineElementHolderViewModels.value[0]
            viewModelMessage2.shouldBeInstanceOf<TimelineElementHolderViewModel>()
            viewModelMessage2.isRead.first { it }
            val viewModelMessage1 = cut.timelineElementHolderViewModels.value[1]
            viewModelMessage1.shouldBeInstanceOf<TimelineElementHolderViewModel>()
            viewModelMessage1.isRead.first { it.not() }
            viewModelMessage1.isReadBy() shouldBe listOf("Alice")
        }

        should("only mark own messages as read that have no messages following, but another user in the room has it as her last read message") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(me) {
                    text("Hello")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0
            timelineMock.mockGetTimelineEventsFromLast()
            roomUsers(userServiceMock, roomId) {
                +roomUser(name = "Alice", lastReadMessage = EventId("0"))
            }

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 1

            val viewModelMessage1 = cut.timelineElementHolderViewModels.value[0]
            viewModelMessage1.shouldBeInstanceOf<TimelineElementHolderViewModel>()
            viewModelMessage1.isRead.first { it }
            viewModelMessage1.isReadBy() shouldBe listOf("Alice")
        }

        should("not mark own messsage as read when it is the last message in the timeline and no one has read it") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(me) {
                    text("Hello")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0
            timelineMock.mockGetTimelineEventsFromLast()
            roomUsers(userServiceMock, roomId) {
                +roomUser(name = "Alice", lastReadMessage = null)
            }

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 1

            val viewModelMessage1 = cut.timelineElementHolderViewModels.value[0]
            viewModelMessage1.shouldBeInstanceOf<TimelineElementHolderViewModel>()
            viewModelMessage1.isRead.first { it.not() }
            viewModelMessage1.isReadBy() shouldBe emptyList()
        }

        should("not mark own message as read when there are only own messages following and none of those are marked as the last read message for another user") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = me) {
                    text("Hello")
                }
                +messageEvent(sender = me) {
                    text("World!")
                }
                +messageEvent(sender = me) {
                    text("Hello?")
                }
            }
            timelineMock.mockGetTimelineEventsFromLast()
            roomUsers(userServiceMock, roomId) {
                roomUser(name = "Alice", lastReadMessage = null)
            }

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 3

            val viewModelMessage3 = cut.timelineElementHolderViewModels.value[0]
            viewModelMessage3.shouldBeInstanceOf<TimelineElementHolderViewModel>()
            viewModelMessage3.isRead.first { it.not() }
            viewModelMessage3.isReadBy() shouldBe emptyList()
            val viewModelMessage2 = cut.timelineElementHolderViewModels.value[1]
            viewModelMessage2.shouldBeInstanceOf<TimelineElementHolderViewModel>()
            viewModelMessage2.isRead.first { it.not() }
            viewModelMessage2.isReadBy() shouldBe emptyList()
            val viewModelMessage1 = cut.timelineElementHolderViewModels.value[2]
            viewModelMessage1.shouldBeInstanceOf<TimelineElementHolderViewModel>()
            viewModelMessage1.isRead.first { it.not() }
            viewModelMessage1.isReadBy() shouldBe emptyList()
        }
    }

    private fun timelineViewModel(): TimelineViewModelImpl {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(
                    mapOf(
                        UserId("test", "server") to matrixClientMock
                    )
                ) + module {
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
                                onMessageEditFinished: (EventId) -> Unit,
                                onMessageReplyToFinished: (EventId) -> Unit,
                                onShowAttachmentSendView: (file: FileDescriptor) -> Unit
                            ): InputAreaViewModel {
                                return inputAreaViewModelMock
                            }
                        }
                    }
                })
        }.koin
        return TimelineViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(lifecycleRegistry),
                di = di,
                userId = UserId("test", "server"),
            ),
            selectedRoomId = roomId,
            isBackButtonVisible = MutableStateFlow(false),
            onBack = mock(),
            onOpenMedia = mock(),
            onShowSettings = mock(),
            onOpenMention = mock(),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TimelineMock.mockGetTimelineEventsFromLast() {
        every {
            roomServiceMock.getTimelineEvents(
                any(),
                any(),
                any(),
                any(),
            )
        } returns eventsInStore.flatMapLatest { it.reversed().asFlow() }
    }
}
