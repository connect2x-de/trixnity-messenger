package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import de.connect2x.trixnity.messenger.GetAccountNames
import de.connect2x.trixnity.messenger.NamedMatrixClient
import de.connect2x.trixnity.messenger.NamedMatrixClients
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.viewmodel.*
import de.connect2x.trixnity.messenger.viewmodel.util.ErrorType
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import isNot
import isRoomOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomsApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.m.DirectEventContent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent.RoomType
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.space.ChildEventContent
import org.kodein.mock.*
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import io.kotest.matchers.Matcher as KoMatcher

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomListViewModelMultiAccountTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val mocker = Mocker()

    private lateinit var lifecycleRegistry: LifecycleRegistry

    private val roomId1 = RoomId("room1", "localhost")
    private val roomId2 = RoomId("room2", "localhost")
    private val roomId3 = RoomId("room3", "localhost")
    private val roomId4 = RoomId("room4", "localhost")
    private val roomId5 = RoomId("room5", "localhost")
    private val spaceId1 = RoomId("space1", "localhost")
    private val spaceId2 = RoomId("space2", "localhost")

    private val me1 = UserId("me1", "server")
    private val me2 = UserId("me2", "server")
    private val me3 = UserId("me3", "server")
    private val user2 = UserId("user2", "server")
    private val user3 = UserId("user3", "server")

    private val user2Flow = MutableStateFlow(roomUser(roomId1, user2))

    @Mock
    lateinit var matrixClientMock1: MatrixClient

    @Mock
    lateinit var matrixClientMock2: MatrixClient

    @Mock
    lateinit var matrixClientMock3: MatrixClient

    @Mock
    lateinit var userServiceMock1: UserService

    @Mock
    lateinit var userServiceMock2: UserService

    @Mock
    lateinit var userServiceMock3: UserService

    @Mock
    lateinit var roomServiceMock1: RoomService

    @Mock
    lateinit var roomServiceMock2: RoomService

    @Mock
    lateinit var roomServiceMock3: RoomService

    @Mock
    lateinit var matrixClientServerApiClientMock: MatrixClientServerApiClient

    @Mock
    lateinit var roomsApiClientMock: RoomsApiClient

    @Mock
    lateinit var roomNameMock: RoomName

    private val onRoomSelectedMock = mockFunction2<Unit, String, RoomId>(mocker)

    private lateinit var syncStateMocker1: Mocker.Every<StateFlow<SyncState>>
    private lateinit var syncStateMocker2: Mocker.Every<StateFlow<SyncState>>
    private lateinit var syncStateMocker3: Mocker.Every<StateFlow<SyncState>>
    private lateinit var roomName3Mocker: Mocker.Every<Flow<RoomNameElement>>

    private lateinit var di: Koin
    private lateinit var namedMatrixClients: MutableStateFlow<List<NamedMatrixClient>>

    init {
        Dispatchers.setMain(testMainDispatcher)
        coroutineTestScope = true

        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            lifecycleRegistry = LifecycleRegistry()
            lifecycleRegistry.resume()

            // MatrixClient1: room1, room2, space1
            // MatrixClient2: room3, room4, space2
            // MatrixClient3: room5
            with(mocker) {
                every { matrixClientMock1.di } returns koinApplication {
                    modules(
                        module {
                            single { roomServiceMock1 }
                            single { userServiceMock1 }
                        }
                    )
                }.koin
                every { matrixClientMock2.di } returns koinApplication {
                    modules(
                        module {
                            single { roomServiceMock2 }
                            single { userServiceMock2 }
                        }
                    )
                }.koin
                every { matrixClientMock3.di } returns koinApplication {
                    modules(
                        module {
                            single { roomServiceMock3 }
                            single { userServiceMock3 }
                        }
                    )
                }.koin
                syncStateMocker1 = every { matrixClientMock1.syncState }
                syncStateMocker1 returns MutableStateFlow(SyncState.RUNNING)
                syncStateMocker2 = every { matrixClientMock2.syncState }
                syncStateMocker2 returns MutableStateFlow(SyncState.RUNNING)
                syncStateMocker3 = every { matrixClientMock3.syncState }
                syncStateMocker3 returns MutableStateFlow(SyncState.RUNNING)
                every { matrixClientMock1.userId } returns me1
                every { matrixClientMock2.userId } returns me2
                every { matrixClientMock3.userId } returns me3
                every { matrixClientMock1.room } returns roomServiceMock1
                every { matrixClientMock2.room } returns roomServiceMock2
                every { matrixClientMock3.room } returns roomServiceMock3
                every { matrixClientMock1.user } returns userServiceMock1
                every { matrixClientMock2.user } returns userServiceMock2
                every { matrixClientMock3.user } returns userServiceMock3
                every { matrixClientMock1.api } returns matrixClientServerApiClientMock
                every { matrixClientMock2.api } returns matrixClientServerApiClientMock
                every { matrixClientMock3.api } returns matrixClientServerApiClientMock
                every { matrixClientServerApiClientMock.rooms } returns roomsApiClientMock

                every { userServiceMock1.getById(isEqual(roomId1), isEqual(me1)) } returns
                        MutableStateFlow(roomUser(roomId1, me1))
                every { userServiceMock1.getById(isEqual(roomId2), isEqual(me1)) } returns
                        MutableStateFlow(roomUser(roomId2, me1))
                every { userServiceMock1.getById(isEqual(roomId1), isEqual(user2)) } returns user2Flow
                every { userServiceMock1.getById(isEqual(roomId1), isEqual(user3)) } returns
                        MutableStateFlow(roomUser(roomId1, user3))

                every { userServiceMock1.getById(isAny(), isEqual(user2)) } returns
                        MutableStateFlow(roomUser(roomId2, user2))
                every { userServiceMock2.getById(isAny(), isEqual(user2)) } returns
                        MutableStateFlow(roomUser(roomId2, user2))
                every { userServiceMock3.getById(isAny(), isEqual(user2)) } returns
                        MutableStateFlow(roomUser(roomId2, user2))

                every { userServiceMock1.getAll(isEqual(roomId1)) } returns
                        MutableStateFlow(
                            mapOf(
                                me1 to flowOf(roomUser(roomId1, me1)),
                                user2 to flowOf(roomUser(roomId1, user2))
                            )
                        )
                every { userServiceMock1.getAll(isEqual(roomId2)) } returns
                        MutableStateFlow(
                            mapOf(
                                me1 to flowOf(roomUser(roomId2, me1)),
                                user2 to flowOf(roomUser(roomId2, user2))
                            )
                        )
                every { userServiceMock2.getAll(roomId3) } returns
                        MutableStateFlow(
                            mapOf(
                                me2 to flowOf(roomUser(roomId3, me2)),
                                user3 to flowOf(roomUser(roomId3, user3))
                            )
                        )
                every { userServiceMock2.getAll(isEqual(roomId4)) } returns
                        MutableStateFlow(mapOf(me2 to flowOf(roomUser(roomId4, me2))))
                every { userServiceMock3.getAll(isEqual(roomId5)) } returns MutableStateFlow(emptyMap())
                every { userServiceMock1.userPresence } returns MutableStateFlow(mapOf())
                every { userServiceMock2.userPresence } returns MutableStateFlow(mapOf())
                every { userServiceMock3.userPresence } returns MutableStateFlow(mapOf())

                every {
                    roomServiceMock1.getState<CreateEventContent>(
                        isNot(listOf(roomId5, spaceId1, spaceId2)),
                        isAny(),
                        isAny()
                    )
                } returns flowOf(
                    Event.StateEvent(
                        content = CreateEventContent(creator = me1),
                        id = EventId(""),
                        sender = me1,
                        roomId = roomId1,
                        originTimestamp = 0L,
                        stateKey = ""
                    )
                )
                every {
                    roomServiceMock2.getState<CreateEventContent>(
                        isNot(listOf(roomId5, spaceId1, spaceId2)),
                        isAny(),
                        isAny()
                    )
                } returns flowOf(
                    Event.StateEvent(
                        content = CreateEventContent(creator = me2),
                        id = EventId(""),
                        sender = me2,
                        roomId = roomId1,
                        originTimestamp = 0L,
                        stateKey = ""
                    )
                )
                every {
                    roomServiceMock3.getState<CreateEventContent>(
                        isNot(listOf(roomId5, spaceId1, spaceId2)),
                        isAny(),
                        isAny()
                    )
                } returns flowOf(
                    Event.StateEvent(
                        content = CreateEventContent(creator = me3),
                        id = EventId(""),
                        sender = me3,
                        roomId = roomId1,
                        originTimestamp = 0L,
                        stateKey = ""
                    )
                )

                every { onRoomSelectedMock.invoke(isAny(), isAny()) } returns Unit

                every {
                    roomNameMock.getRoomNameElement(isRoomOf(roomId1), isEqual(matrixClientMock1))
                } returns flowOf(RoomNameElement("room1"))
                every {
                    roomNameMock.getRoomNameElement(roomId1, matrixClientMock1)
                } returns flowOf(RoomNameElement("room1"))
                every {
                    roomNameMock.getRoomNameElement(isRoomOf(roomId2), isEqual(matrixClientMock1))
                } returns flowOf(RoomNameElement("room2"))
                every {
                    roomNameMock.getRoomNameElement(roomId2, matrixClientMock1)
                } returns flowOf(RoomNameElement("room2"))
                roomName3Mocker =
                    every { roomNameMock.getRoomNameElement(isRoomOf(roomId3), isEqual(matrixClientMock2)) }
                roomName3Mocker returns flowOf(RoomNameElement("room3-but-also-room2"))
                every {
                    roomNameMock.getRoomNameElement(roomId3, matrixClientMock2)
                } returns flowOf(RoomNameElement("room3-but-also-room2"))
                every {
                    roomNameMock.getRoomNameElement(isRoomOf(roomId4), isEqual(matrixClientMock2))
                } returns flowOf(RoomNameElement("room4"))
                every {
                    roomNameMock.getRoomNameElement(roomId4, matrixClientMock2)
                } returns flowOf(RoomNameElement("room4"))
                every {
                    roomNameMock.getRoomNameElement(isRoomOf(roomId5), isEqual(matrixClientMock3))
                } returns flowOf(RoomNameElement("room5"))
                every {
                    roomNameMock.getRoomNameElement(roomId5, matrixClientMock3)
                } returns flowOf(RoomNameElement("room5"))
                every {
                    roomNameMock.getRoomNameElement(isRoomOf(spaceId1), isEqual(matrixClientMock1))
                } returns
                        flowOf(RoomNameElement("space and beyond"))
                every {
                    roomNameMock.getRoomNameElement(isRoomOf(spaceId2), isEqual(matrixClientMock2))
                } returns flowOf(RoomNameElement("space and beyond and beyonder"))
                every { roomServiceMock1.getLastTimelineEvent(isAny(), isAny()) } returns flowOf(null)
                every { roomServiceMock2.getLastTimelineEvent(isAny(), isAny()) } returns flowOf(null)
                every { roomServiceMock3.getLastTimelineEvent(isAny(), isAny()) } returns flowOf(null)

                every {
                    roomServiceMock1.getAllState<ChildEventContent>(isEqual(spaceId1), isAny())
                } returns flowOf(mapOf())

                every { userServiceMock1.getAccountData<DirectEventContent>() } returns
                        MutableStateFlow(
                            DirectEventContent(
                                mappings = mapOf(
                                    user2 to setOf(roomId1),
                                )
                            )
                        )
                every { userServiceMock2.getAccountData<DirectEventContent>() } returns
                        MutableStateFlow(
                            DirectEventContent(
                                mappings = emptyMap()
                            )
                        )
                every { userServiceMock3.getAccountData<DirectEventContent>() } returns
                        MutableStateFlow(
                            DirectEventContent(
                                mappings = emptyMap()
                            )
                        )
            }
        }

        should("sort rooms by last received message, even if the rooms are of different matrix accounts") {
            mocker.every {
                roomServiceMock3.getState(roomId5, CreateEventContent::class, "")
            } returns
                    flowOf(
                        Event.StateEvent(
                            CreateEventContent(user2),
                            EventId("\$event-a"),
                            user2,
                            roomId5,
                            Instant.parse("2021-11-04T17:00:00Z").toEpochMilliseconds(),
                            stateKey = ""
                        )
                    )
            val eventId1 = EventId("1")
            mocker.every {
                roomServiceMock1.getTimelineEvent(
                    isEqual(roomId1),
                    isEqual(eventId1),
                    isAny(),
                )
            } returns MutableStateFlow(
                timelineEvent(eventId1, Instant.parse("2021-11-03T14:00:00Z"))
            )
            val room1 = Room(roomId1, lastRelevantEventId = eventId1)
            val eventId2 = EventId("2")
            mocker.every {
                roomServiceMock1.getTimelineEvent(
                    isEqual(roomId2),
                    isEqual(eventId2),
                    isAny(),
                )
            } returns MutableStateFlow(
                timelineEvent(eventId2, Instant.parse("2021-11-04T19:00:00Z"))
            )
            val room2 = Room(roomId2, lastRelevantEventId = eventId2)
            val eventId3 = EventId("3")
            mocker.every {
                roomServiceMock2.getTimelineEvent(
                    isEqual(roomId3),
                    isEqual(eventId3),
                    isAny(),
                )
            } returns MutableStateFlow(
                timelineEvent(eventId3, Instant.parse("2021-11-04T18:00:00Z"))
            )
            val room3 = Room(roomId3, lastRelevantEventId = eventId3)
            val room4 = Room(roomId4)
            val room5 = Room(roomId5) // with invite
            with(mocker) {
                every { roomServiceMock1.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        roomId2 to MutableStateFlow(room2),
                    )
                )
                every { roomServiceMock2.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId3 to MutableStateFlow(room3),
                        roomId4 to MutableStateFlow(room4),
                    )
                )
                every { roomServiceMock3.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId5 to MutableStateFlow(room5),
                    )
                )
                every { roomServiceMock1.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock1.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock2.getById(roomId3) } returns MutableStateFlow(room3)
                every { roomServiceMock2.getById(roomId4) } returns MutableStateFlow(room4)
                every { roomServiceMock3.getById(roomId5) } returns MutableStateFlow(room5)
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            val list = cut.sortedRoomListElementViewModels.onEach { println(it) }.first { it.size == 5 }
            list[0].first shouldBe roomId2
            list[1].first shouldBe roomId3
            list[2].first shouldBe roomId5
            list[3].first shouldBe roomId1
            list[4].first shouldBe roomId4

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set 'initialSyncFinished' to 'true' when the initial sync with the matrix server is completed for all accounts") {
            mocker.every { roomServiceMock1.getAll() } returns MutableStateFlow(emptyMap())
            mocker.every { roomServiceMock2.getAll() } returns MutableStateFlow(emptyMap())
            mocker.every { roomServiceMock3.getAll() } returns MutableStateFlow(emptyMap())
            val syncState1 = MutableStateFlow(SyncState.STARTED)
            val syncState2 = MutableStateFlow(SyncState.STARTED)
            val syncState3 = MutableStateFlow(SyncState.STARTED)
            syncStateMocker1 returns syncState1
            syncStateMocker2 returns syncState2
            syncStateMocker3 returns syncState3

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            syncState1.value = SyncState.INITIAL_SYNC
            syncState2.value = SyncState.INITIAL_SYNC
            syncState3.value = SyncState.INITIAL_SYNC
            testCoroutineScheduler.advanceUntilIdle()
            cut.initialSyncFinished.value shouldBe false

            syncState1.value = SyncState.RUNNING
            testCoroutineScheduler.advanceUntilIdle()
            cut.initialSyncFinished.value shouldBe false

            syncState2.value = SyncState.RUNNING
            testCoroutineScheduler.advanceUntilIdle()
            cut.initialSyncFinished.value shouldBe false

            syncState3.value = SyncState.RUNNING
            testCoroutineScheduler.advanceUntilIdle()
            cut.initialSyncFinished.value shouldBe true

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("display info message when trying to join a room while the client is not connected to the server") {
            val room = Room(roomId1, membership = Membership.INVITE)
            mocker.every { roomServiceMock1.getById(roomId1) } returns flowOf(room)
            mocker.every { roomServiceMock1.getAll() } returns MutableStateFlow(
                mapOf(
                    roomId1 to MutableStateFlow(Room(roomId1))
                )
            )
            mocker.every { roomServiceMock2.getAll() } returns MutableStateFlow(emptyMap())
            mocker.every { roomServiceMock3.getAll() } returns MutableStateFlow(emptyMap())
            mocker.everySuspending {
                roomsApiClientMock.joinRoom(
                    isEqual(roomId1),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                )
            } returns Result.success(roomId1)
            val syncState = MutableStateFlow(SyncState.ERROR)
            syncStateMocker1 returns syncState

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            testCoroutineScheduler.advanceUntilIdle()
            cut.selectRoom(roomId1)
            testCoroutineScheduler.advanceUntilIdle()

            cut.error.value shouldNotBe null
            cut.errorType.value shouldBe ErrorType.JUST_DISMISS

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("yield all rooms as search result when search term which is blank") {
            val room1 = Room(roomId1)
            val room2 = Room(roomId2)
            val room3 = Room(roomId3)
            val room4 = Room(roomId4)
            with(mocker) {
                every { roomServiceMock1.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        roomId2 to MutableStateFlow(room2),
                    )
                )
                every { roomServiceMock2.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId3 to MutableStateFlow(room3),
                        roomId4 to MutableStateFlow(room4),
                    )
                )
                mocker.every { roomServiceMock3.getAll() } returns MutableStateFlow(emptyMap())
                every { roomServiceMock1.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock1.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock2.getById(roomId3) } returns MutableStateFlow(room3)
                every { roomServiceMock2.getById(roomId4) } returns MutableStateFlow(room4)
            }
            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.searchTerm.value = ""
            testCoroutineScheduler.advanceUntilIdle()
            cut.sortedRoomListElementViewModels.value shouldHaveSize 4

            cut.searchTerm.value = "  "
            testCoroutineScheduler.runCurrent()
            cut.sortedRoomListElementViewModels.value shouldHaveSize 4

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("contain search term in all search results") {
            val room1 = Room(roomId1)
            val room2 = Room(roomId2)
            val room3 = Room(roomId3)
            val room4 = Room(roomId4)
            with(mocker) {
                every { roomServiceMock1.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        roomId2 to MutableStateFlow(room2),
                    )
                )
                every { roomServiceMock2.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId3 to MutableStateFlow(room3),
                        roomId4 to MutableStateFlow(room4),
                    )
                )
                mocker.every { roomServiceMock3.getAll() } returns MutableStateFlow(emptyMap())

                every { roomServiceMock1.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock1.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock2.getById(roomId3) } returns MutableStateFlow(room3)
                every { roomServiceMock2.getById(roomId4) } returns MutableStateFlow(room4)
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.searchTerm.value = "2"
            testCoroutineScheduler.advanceTimeBy(500) // debounce
            testCoroutineScheduler.runCurrent()

            cut.sortedRoomListElementViewModels.value shouldHaveSize 2
            cut.sortedRoomListElementViewModels.value.should(
                containRoomListElementViewModelsFor(listOf(roomId2, roomId3))
            )

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("add a newly added room to the search result when it fits the search term") {
            val room1 = Room(roomId1)
            val room2 = Room(roomId2)
            val room3 = Room(roomId3)
            val room3NameFlow = MutableStateFlow(RoomNameElement("room2-other"))
            with(mocker) {
                every { roomServiceMock1.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        roomId2 to MutableStateFlow(room2),
                    )
                )
                every { roomServiceMock2.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId3 to MutableStateFlow(room3),
                    )
                )
                mocker.every { roomServiceMock3.getAll() } returns MutableStateFlow(emptyMap())
                every { roomServiceMock1.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock1.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock2.getById(roomId3) } returns MutableStateFlow(room3)
                every {
                    roomNameMock.getRoomNameElement(isEqual(room3), isEqual(matrixClientMock2))
                } returns room3NameFlow
                roomName3Mocker returns room3NameFlow
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.searchTerm.value = "1"
            testCoroutineScheduler.advanceTimeBy(500) // debounce
            testCoroutineScheduler.runCurrent()
            cut.sortedRoomListElementViewModels.value.should(containRoomListElementViewModelsFor(listOf(roomId1)))

            room3NameFlow.value = RoomNameElement("I am number 1")
            testCoroutineScheduler.advanceUntilIdle() // no debounce, since search term stays the same
            cut.sortedRoomListElementViewModels.value.should(
                containRoomListElementViewModelsFor(listOf(roomId1, roomId3))
            )

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("not show spaces in room list") {
            val room1 = Room(roomId1)
            val room2 = Room(roomId2)
            val space = Room(spaceId1)
            val room3 = Room(roomId3)
            with(mocker) {
                every { roomServiceMock1.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        roomId2 to MutableStateFlow(room2),
                        spaceId1 to MutableStateFlow(space),
                    )
                )
                every { roomServiceMock2.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId3 to MutableStateFlow(room3),
                    )
                )
                mocker.every { roomServiceMock3.getAll() } returns MutableStateFlow(emptyMap())
                every { roomServiceMock1.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock1.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock1.getById(spaceId1) } returns MutableStateFlow(space)
                every { roomServiceMock2.getById(roomId3) } returns MutableStateFlow(room3)
                every { roomServiceMock1.getState<CreateEventContent>(spaceId1, "") } returns
                        flowOf(
                            Event.StateEvent(
                                CreateEventContent(
                                    creator = me1,
                                    federate = false,
                                    roomVersion = "",
                                    type = RoomType.Space,
                                ),
                                EventId(""), UserId(""), RoomId(""), 0L, stateKey = ""
                            )
                        )
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.sortedRoomListElementViewModels.value shouldHaveSize 3

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("consider all spaces") {
            val room1 = Room(roomId1)
            val room3 = Room(roomId3)
            val space1 = Room(spaceId1)
            val space2 = Room(spaceId2)
            with(mocker) {
                every { roomServiceMock1.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        spaceId1 to MutableStateFlow(space1),
                    )
                )
                every { roomServiceMock2.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId3 to MutableStateFlow(room3),
                        spaceId2 to MutableStateFlow(space2),
                    )
                )
                mocker.every { roomServiceMock3.getAll() } returns MutableStateFlow(emptyMap())
                every { roomServiceMock1.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock1.getById(spaceId1) } returns MutableStateFlow(space1)
                every { roomServiceMock2.getById(roomId3) } returns MutableStateFlow(room3)
                every { roomServiceMock2.getById(spaceId2) } returns MutableStateFlow(space2)
                every { roomServiceMock1.getState<CreateEventContent>(spaceId1, "") } returns
                        flowOf(
                            Event.StateEvent(
                                CreateEventContent(
                                    creator = me1,
                                    federate = false,
                                    roomVersion = "",
                                    type = RoomType.Space,
                                ),
                                EventId(""), UserId(""), RoomId(""), 0L, stateKey = ""
                            )
                        )
                every { roomServiceMock2.getState<CreateEventContent>(spaceId2, "") } returns
                        flowOf(
                            Event.StateEvent(
                                CreateEventContent(
                                    creator = me2,
                                    federate = false,
                                    roomVersion = "",
                                    type = RoomType.Space,
                                ),
                                EventId(""), UserId(""), RoomId(""), 0L, stateKey = ""
                            )
                        )
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.spaces.value shouldHaveSize 2
            cut.spaces.value[0].name shouldBe "space and beyond"
            cut.spaces.value[1].name shouldBe "space and beyond and beyonder"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("filter rooms by the space that is currently selected") {
            val room1 = Room(roomId1)
            val room2 = Room(roomId2)
            val room3 = Room(roomId3)
            val room4 = Room(roomId4)
            val space1 = Room(spaceId1)
            val space2 = Room(spaceId2)
            with(mocker) {
                every { roomServiceMock1.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        roomId2 to MutableStateFlow(room2),
                        spaceId1 to MutableStateFlow(space1),
                    )
                )
                every { roomServiceMock2.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId3 to MutableStateFlow(room3),
                        roomId4 to MutableStateFlow(room4),
                        spaceId2 to MutableStateFlow(space2),
                    )
                )
                mocker.every { roomServiceMock3.getAll() } returns MutableStateFlow(emptyMap())
                every { roomServiceMock1.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock1.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock1.getById(spaceId1) } returns MutableStateFlow(space1)
                every { roomServiceMock2.getById(roomId3) } returns MutableStateFlow(room3)
                every { roomServiceMock2.getById(roomId4) } returns MutableStateFlow(room4)
                every { roomServiceMock2.getById(spaceId2) } returns MutableStateFlow(space2)
                every { roomServiceMock1.getState<CreateEventContent>(spaceId1, "") } returns
                        flowOf(
                            Event.StateEvent(
                                CreateEventContent(
                                    creator = me1,
                                    federate = false,
                                    roomVersion = "",
                                    type = RoomType.Space,
                                ),
                                EventId(""), UserId(""), RoomId(""), 0L, stateKey = ""
                            )
                        )
                every { roomServiceMock2.getState<CreateEventContent>(spaceId2, "") } returns
                        flowOf(
                            Event.StateEvent(
                                CreateEventContent(
                                    creator = me2,
                                    federate = false,
                                    roomVersion = "",
                                    type = RoomType.Space,
                                ),
                                EventId(""), UserId(""), RoomId(""), 0L, stateKey = ""
                            )
                        )

                every {
                    roomServiceMock1.getAllState<ChildEventContent>(isEqual(spaceId1), isAny())
                } returns
                        flowOf(
                            mapOf(
                                roomId1.full to flowOf(spaceChildEvent(spaceId1, roomId1)),
                                roomId2.full to flowOf(spaceChildEvent(spaceId1, roomId2)),
                            )
                        )
                every {
                    roomServiceMock2.getAllState<ChildEventContent>(isEqual(spaceId2), isAny())
                } returns
                        flowOf(
                            mapOf(
                                roomId3.full to flowOf(spaceChildEvent(spaceId2, roomId3)),
                                roomId4.full to flowOf(spaceChildEvent(spaceId2, roomId4)),
                            )
                        )
                every { roomServiceMock1.getAllState<ChildEventContent>(isEqual(spaceId2), isAny()) } returns
                        flowOf(emptyMap())
                every { roomServiceMock2.getAllState<ChildEventContent>(isEqual(spaceId1), isAny()) } returns
                        flowOf(emptyMap())
                every { roomServiceMock3.getAllState<ChildEventContent>(isAny(), isAny()) } returns
                        flowOf(emptyMap())

                every { userServiceMock1.getAll(isEqual(spaceId1)) } returns MutableStateFlow(emptyMap())
                every { userServiceMock1.getAll(isEqual(spaceId2)) } returns MutableStateFlow(emptyMap())
                every { userServiceMock2.getAll(isEqual(spaceId1)) } returns MutableStateFlow(emptyMap())
                every { userServiceMock2.getAll(isEqual(spaceId2)) } returns MutableStateFlow(emptyMap())
                every { userServiceMock3.getAll(isEqual(spaceId1)) } returns MutableStateFlow(emptyMap())
                every { userServiceMock3.getAll(isEqual(spaceId2)) } returns MutableStateFlow(emptyMap())
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.activeSpace.value shouldBe null
            cut.sortedRoomListElementViewModels.value shouldHaveSize 4

            cut.activeSpace.value = spaceId1
            cut.sortedRoomListElementViewModels.first { it.isEmpty() }

            cut.activeSpace.value = spaceId2
            cut.sortedRoomListElementViewModels.first { it.size == 2 }

            cut.activeSpace.value = null
            cut.sortedRoomListElementViewModels.first { it.size == 4 }

            cut.activeSpace.value = spaceId2
            cut.sortedRoomListElementViewModels.first { it.size == 2 }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("react to changes of accounts") {
            val room1 = Room(roomId1)
            val room2 = Room(roomId2)
            val room3 = Room(roomId3)
            with(mocker) {
                every { roomServiceMock1.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        roomId2 to MutableStateFlow(room2),
                    )
                )
                every { roomServiceMock2.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId3 to MutableStateFlow(room3),
                    )
                )
                mocker.every { roomServiceMock3.getAll() } returns MutableStateFlow(emptyMap())
                every { roomServiceMock1.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock1.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock2.getById(roomId3) } returns MutableStateFlow(room3)
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()
            cut.sortedRoomListElementViewModels.value shouldHaveSize 3

            namedMatrixClients.value =
                listOf(
                    NamedMatrixClient(
                        accountName = "test1",
                        MutableStateFlow(matrixClientMock1),
                    ),
                )
            testCoroutineScheduler.advanceUntilIdle()
            cut.sortedRoomListElementViewModels.value shouldHaveSize 2

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }

    private fun CoroutineScope.subscribe(cut: RoomListViewModel) = launch {
        launch { cut.selectedRoomId.collect() }
        launch { cut.error.collect() }
        launch { cut.errorType.collect() }
        launch { cut.sortedRoomListElementViewModels.collect() }
        launch { cut.syncStateError.collect() }
        launch { cut.initialSyncFinished.collect() }
        launch { cut.showSearch.collect() }
        launch { cut.searchTerm.collect() }
        launch { cut.spaces.collect() }
        launch { cut.activeSpace.collect() }
        launch { cut.showSpaces.collect() }
    }

    private fun roomListViewModel(coroutineContext: CoroutineContext): RoomListViewModelImpl {
        namedMatrixClients = MutableStateFlow(
            listOf(
                NamedMatrixClient(
                    accountName = "test1",
                    MutableStateFlow(matrixClientMock1),
                ),
                NamedMatrixClient(
                    accountName = "test2",
                    MutableStateFlow(matrixClientMock2),
                ),
                NamedMatrixClient(
                    accountName = "test3",
                    MutableStateFlow(matrixClientMock3),
                )
            )
        )
        di = koinApplication {
            modules(trixnityMessengerModule(), module {
                single {
                    NamedMatrixClients(
                        namedMatrixClients
                    )
                }
                single {
                    object : GetAccountNames {
                        override suspend fun invoke(): List<String> {
                            return listOf("test1", "test2", "test3")
                        }
                    }
                }
                single { roomNameMock }
                single<AccountViewModelFactory> {
                    object : AccountViewModelFactory {
                        override fun newAccountViewModel(
                            viewModelContext: ViewModelContext,
                            onAccountSelected: (String?) -> Unit,
                            onUserSettingsSelected: () -> Unit,
                            onShowAppInfo: () -> Unit
                        ): AccountViewModel {
                            return object : AccountViewModel {
                                override val activeAccount: StateFlow<Account?> = MutableStateFlow(null)
                                override val allAccounts: StateFlow<List<Account>> = MutableStateFlow(listOf())

                                override fun selectActiveAccount(accountName: String?) {
                                }

                                override fun userSettings() {
                                }

                                override fun appInfo() {
                                }
                            }
                        }
                    }
                }
            })
        }.koin
        di.createScope<RootViewModelImpl>()
        return RoomListViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(lifecycleRegistry),
                di = di,
                accountName = "test1",
                coroutineContext = coroutineContext
            ),
            selectedRoomId = MutableStateFlow(RoomId("roomId", "localhost")),
            onRoomSelected = onRoomSelectedMock,
            onCreateNewRoom = mockFunction1(mocker),
            onUserSettingsSelected = mockFunction0(mocker),
            onOpenAppInfo = mockFunction0(mocker),
            onOpenAccountsOverview = mockFunction0(mocker),
            onSendLogs = mockFunction0(mocker),
        )
    }

    private fun containRoomListElementViewModelsFor(roomIds: List<RoomId>) =
        KoMatcher<List<Pair<RoomId, RoomListElementViewModel>>> { list ->
            MatcherResult(roomIds.all { roomId ->
                list.any { (_, vm) -> vm.roomId == roomId }
            },
                {
                    "RoomListElementViewModel with ids [${
                        roomIds.filterNot { roomId -> list.any { (_, vm) -> vm.roomId == roomId } }
                            .joinToString { it.full }
                    }] not found"
                },
                {
                    "RoomListElementViewModel with ids [${
                        roomIds.filterNot { roomId -> list.any { (_, vm) -> vm.roomId == roomId } }
                            .joinToString { it.full }
                    }] not found"
                })
        }

    private fun timelineEvent(eventId: EventId, sentAt: Instant) = TimelineEvent(
        event = Event.MessageEvent(
            content = RoomMessageEventContent.TextMessageEventContent(""),
            id = eventId,
            sender = user2,
            roomId = roomId1,
            originTimestamp = sentAt.toEpochMilliseconds(),
            unsigned = null
        ),
        content = null,
        roomId = roomId1,
        eventId = eventId,
        previousEventId = null,
        nextEventId = null,
        gap = null,
    )

    private fun roomUser(roomId: RoomId, userId: UserId) = RoomUser(
        roomId,
        userId,
        "user1",
        memberEvent(roomId, userId)
    )

    private fun memberEvent(roomId: RoomId, sender: UserId) = Event.StateEvent(
        content = MemberEventContent(membership = Membership.JOIN),
        id = EventId("1"),
        sender = sender,
        roomId = roomId,
        originTimestamp = 0L,
        stateKey = "",
    )

    private fun spaceChildEvent(spaceId: RoomId, containedId: RoomId) =
        Event.StateEvent(
            content = ChildEventContent(),
            id = EventId(""),
            sender = me2,
            roomId = spaceId,
            originTimestamp = 0L,
            stateKey = containedId.full,
        )
}
