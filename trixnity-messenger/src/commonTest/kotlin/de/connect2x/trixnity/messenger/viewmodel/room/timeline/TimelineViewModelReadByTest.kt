package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.files.DownloadManager
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomsApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.kodein.mock.mockFunction4
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModelReadByTest : ShouldSpec() {
    override fun timeout(): Long = 5_000

    private val mocker = Mocker()

    private lateinit var lifecycleRegistry: LifecycleRegistry

    private val roomId = RoomId("room1", "localhost")
    private val me = UserId("user1", "localhost")
    private val alice = UserId("alice", "localhost")

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var userServiceMock: UserService

    @Mock
    lateinit var matrixClientServerApiMock: MatrixClientServerApiClient

    @Mock
    lateinit var roomsApiClientMock: RoomsApiClient

    @Mock
    lateinit var downloadManagerMock: DownloadManager

    @Mock
    lateinit var roomHeaderViewModelMock: RoomHeaderViewModel

    @Mock
    lateinit var inputAreaViewModelMock: InputAreaViewModel

    init {
        Dispatchers.setMain(testMainDispatcher)
        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            lifecycleRegistry = LifecycleRegistry()
            lifecycleRegistry.start()

            with(mocker) {
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

                every { matrixClientServerApiMock.rooms } returns roomsApiClientMock
                everySuspending {
                    roomsApiClientMock.setReadMarkers(isAny(), isAny(), isAny(), isAny(), isNull())
                } returns Result.success(Unit)

                every { roomServiceMock.getOutbox() } returns MutableStateFlow(mapOf())
                every { userServiceMock.canRedactEvent(isAny(), isAny()) } returns flowOf(true)
                every { userServiceMock.canSendEvent(isAny(), isAny()) } returns flowOf(true)

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
                every { roomServiceMock.getTimelineEvent(isAny(), isAny(), isAny()) } returns
                        dummyEvent
                every { roomServiceMock.getPreviousTimelineEvent(isAny(), isAny()) } returns
                        dummyEvent

                every { userServiceMock.getById(isEqual(roomId), isAny()) } returns
                        MutableStateFlow(null)
                everySuspending { userServiceMock.loadMembers(roomId, false) } returns Unit
            }
        }
        afterTest {
            lifecycleRegistry.destroy()
        }

        should("return empty list if my message has been read by no one else") {
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
            timelineMock.fullyReadEventIndex.value = 0
            timelineMock.mockGetTimelineEventsFromLast()
            roomUsers(mocker, userServiceMock, roomId) {}

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 10
            cut.timelineElementHolderViewModels.value.first()
                .shouldBeInstanceOf<TimelineElementHolderViewModel>().isReadBy() shouldBe ""
        }

        should("return list of users that have my message as their last read message") {
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
            timelineMock.fullyReadEventIndex.value = 0
            timelineMock.mockGetTimelineEventsFromLast()
            roomUsers(mocker, userServiceMock, roomId) {
                +roomUser("1", lastReadMessage = EventId("0"))
                +roomUser("2", lastReadMessage = EventId("0"))
            }

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 10
            cut.timelineElementHolderViewModels.value.first()
                .shouldBeInstanceOf<TimelineElementHolderViewModel>().isReadBy() shouldBe "read by 1, 2"
            // bug: the second time, the users were added again
            cut.timelineElementHolderViewModels.value.first()
                .shouldBeInstanceOf<TimelineElementHolderViewModel>().isReadBy() shouldBe "read by 1, 2"
        }

        should("return list of users that have read one of the last 5 messages in the timeline (that are all users in the room)") {
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
            timelineMock.fullyReadEventIndex.value = 0
            timelineMock.mockGetTimelineEventsFromLast()
            roomUsers(mocker, userServiceMock, roomId) {
                +roomUser("1", lastReadMessage = EventId("17"))
                +roomUser("2", lastReadMessage = EventId("18"))
                +roomUser("3", lastReadMessage = EventId("19"))
            }

            val cut = timelineViewModel()
            // even though there are more timeline events available, initially only show 11
            cut.timelineElementHolderViewModels waitForSize 11

            cut.timelineElementHolderViewModels.value.first()
                .shouldBeInstanceOf<TimelineElementHolderViewModel>().isReadBy() shouldBe "read by 1, 2, 3"
        }

        should("increase the number of messages to be considered to 30 until our message is searched") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
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
            roomUsers(mocker, userServiceMock, roomId) {
                +roomUser("1", lastReadMessage = EventId("7"))
                +roomUser("2", lastReadMessage = EventId("8"))
                +roomUser("3", lastReadMessage = EventId("9"))
                +roomUser("noReader", lastReadMessage = null)
            }

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 11

            cut.timelineElementHolderViewModels.value.first()
                .shouldBeInstanceOf<TimelineElementHolderViewModel>().isReadBy() shouldBe "read by 1, 2, 3"
        }

        should("stop at 11 users that read my message") {
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
            timelineMock.fullyReadEventIndex.value = 0
            timelineMock.mockGetTimelineEventsFromLast()
            roomUsers(mocker, userServiceMock, roomId) {
                (1..12).forEach {
                    +roomUser("$it", lastReadMessage = EventId("9"))
                }
            }

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 10

            cut.timelineElementHolderViewModels.value.first()
                .shouldBeInstanceOf<TimelineElementHolderViewModel>()
                .isReadBy() shouldBe "read by 1, 10, 11, 2, 3, 4, 5, 6, 7, 8, ..."
        }

        should("not consider more than 100 messages for the computation of the read by users list") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
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
            roomUsers(mocker, userServiceMock, roomId) {
                +roomUser("readOnlyFirstMessage", lastReadMessage = EventId("0"))
                (1..5).forEach {
                    +roomUser("$it", lastReadMessage = EventId("109"))
                }
            }

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 11

            cut.timelineElementHolderViewModels.value.first()
                .shouldBeInstanceOf<TimelineElementHolderViewModel>().isReadBy() shouldBe "read by 1, 2, 3, 4, 5"
        }

        should("only mark own messages as read that have at least one message of someone else following") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(me) {
                    text("Hello")
                }
                +messageEvent(alice) {
                    text("World!")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0
            timelineMock.mockGetTimelineEventsFromLast()
            roomUsers(mocker, userServiceMock, roomId) {
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
            viewModelMessage1.isReadBy() shouldBe "read by Alice"
        }

        should("only mark own messages as read that have no messages following, but another user in the room has it as her last read message") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(me) {
                    text("Hello")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0
            timelineMock.mockGetTimelineEventsFromLast()
            roomUsers(mocker, userServiceMock, roomId) {
                +roomUser(name = "Alice", lastReadMessage = EventId("0"))
            }

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 1

            val viewModelMessage1 = cut.timelineElementHolderViewModels.value[0]
            viewModelMessage1.shouldBeInstanceOf<TimelineElementHolderViewModel>()
            viewModelMessage1.isRead.first { it }
            viewModelMessage1.isReadBy() shouldBe "read by Alice"
        }

        should("not mark own messsage as read when it is the last message in the timeline and no one has read it") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
                +messageEvent(me) {
                    text("Hello")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0
            timelineMock.mockGetTimelineEventsFromLast()
            roomUsers(mocker, userServiceMock, roomId) {
                +roomUser(name = "Alice", lastReadMessage = null)
            }

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 1

            val viewModelMessage1 = cut.timelineElementHolderViewModels.value[0]
            viewModelMessage1.shouldBeInstanceOf<TimelineElementHolderViewModel>()
            viewModelMessage1.isRead.first { it.not() }
            viewModelMessage1.isReadBy() shouldBe ""
        }

        should("not mark own message as read when there are only own messages following and none of those are marked as the last read message for another user") {
            val timelineMock = timeline(mocker, roomServiceMock, roomId) {
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
            roomUsers(mocker, userServiceMock, roomId) {
                roomUser(name = "Alice", lastReadMessage = null)
            }

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 3

            val viewModelMessage3 = cut.timelineElementHolderViewModels.value[0]
            viewModelMessage3.shouldBeInstanceOf<TimelineElementHolderViewModel>()
            viewModelMessage3.isRead.first { it.not() }
            viewModelMessage3.isReadBy() shouldBe ""
            val viewModelMessage2 = cut.timelineElementHolderViewModels.value[1]
            viewModelMessage2.shouldBeInstanceOf<TimelineElementHolderViewModel>()
            viewModelMessage2.isRead.first { it.not() }
            viewModelMessage2.isReadBy() shouldBe ""
            val viewModelMessage1 = cut.timelineElementHolderViewModels.value[2]
            viewModelMessage1.shouldBeInstanceOf<TimelineElementHolderViewModel>()
            viewModelMessage1.isRead.first { it.not() }
            viewModelMessage1.isReadBy() shouldBe ""
        }
    }

    private fun timelineViewModel(): TimelineViewModelImpl {
        val di = koinApplication {
            modules(trixnityMessengerModule(), testMatrixClientModule(matrixClientMock), module {
                single<RoomHeaderViewModelFactory> {
                    object : RoomHeaderViewModelFactory {
                        override fun newRoomHeaderViewModel(
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
                        override fun newInputAreaViewModel(
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
        di.get<I18n>().setCurrentLang("en")
        return TimelineViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(lifecycleRegistry),
                di = di,
                accountName = "test",
            ),
            selectedRoomId = roomId,
            isBackButtonVisible = MutableStateFlow(false),
            onBack = mockFunction0(mocker),
            onOpenModal = mockFunction4(mocker),
            onShowSettings = mockFunction0(mocker),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TimelineMock.mockGetTimelineEventsFromLast() {
        mocker.every {
            roomServiceMock.getTimelineEvents(
                isAny(),
                isAny(),
                isAny(),
                isAny(),
            )
        } returns eventsInStore.flatMapLatest { it.reversed().asFlow() }
    }
}