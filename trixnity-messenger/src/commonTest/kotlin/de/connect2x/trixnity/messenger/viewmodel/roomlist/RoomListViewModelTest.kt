package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.*
import de.connect2x.trixnity.messenger.viewmodel.util.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import isNot
import isRoomOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.*
import net.folivo.trixnity.core.model.events.m.DirectEventContent
import net.folivo.trixnity.core.model.events.m.room.*
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent.RoomType
import net.folivo.trixnity.core.model.events.m.space.ChildEventContent
import org.kodein.mock.*
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import io.kotest.matchers.Matcher as KoMatcher

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomListViewModelTest : ShouldSpec() {
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

    private val user1 = UserId("user1", "server")
    private val user2 = UserId("user2", "server")
    private val user3 = UserId("user3", "server")

    private val user2Flow = MutableStateFlow(roomUser(roomId1, user2))

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var matrixClientMock2: MatrixClient

    @Mock
    lateinit var userServiceMock: UserService

    @Mock
    lateinit var userServiceMock2: UserService

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var roomServiceMock2: RoomService

    @Mock
    lateinit var matrixClientServerApiClientMock: MatrixClientServerApiClient

    @Mock
    lateinit var roomsApiClientMock: RoomApiClient

    @Mock
    lateinit var roomNameMock: RoomName

    private val onRoomSelectedMock = mockFunction2<Unit, UserId, RoomId>(mocker)

    lateinit var syncStateMocker: Mocker.Every<StateFlow<SyncState>>
    lateinit var roomName3Mocker: Mocker.Every<Flow<String>>

    private val roomCreateEventContent = CreateEventContent(creator = user1, type = RoomType.Room)
    private val spaceCreateEventContent = CreateEventContent(creator = user1, type = RoomType.Space)

    init {
        coroutineTestScope = true

        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            lifecycleRegistry = LifecycleRegistry()
            lifecycleRegistry.resume()

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                            single { userServiceMock }
                        }
                    )
                }.koin
                syncStateMocker = every { matrixClientMock.syncState }
                syncStateMocker returns MutableStateFlow(SyncState.RUNNING)
                every { matrixClientMock.userId } returns user1
                every { matrixClientMock.room } returns roomServiceMock
                every { matrixClientMock.user } returns userServiceMock
                every { matrixClientMock.api } returns matrixClientServerApiClientMock
                every { matrixClientServerApiClientMock.room } returns roomsApiClientMock

                every { userServiceMock.getById(isEqual(roomId1), isEqual(user1)) } returns
                        MutableStateFlow(roomUser(roomId1, user1))
                every { userServiceMock.getById(isEqual(roomId2), isEqual(user1)) } returns
                        MutableStateFlow(roomUser(roomId2, user1))
                every { userServiceMock.getById(isEqual(roomId1), isEqual(user2)) } returns user2Flow
                every { userServiceMock.getById(isAny(), isEqual(user2)) } returns
                        MutableStateFlow(roomUser(roomId2, user2))
                every { userServiceMock.getById(isEqual(roomId1), isEqual(user3)) } returns
                        MutableStateFlow(roomUser(roomId1, user3))

                every { userServiceMock.getAll(roomId1) } returns
                        MutableStateFlow(
                            mapOf(
                                user1 to flowOf(roomUser(roomId1, user1)),
                                user2 to flowOf(roomUser(roomId1, user2))
                            )
                        )
                every { userServiceMock.getAll(roomId2) } returns
                        MutableStateFlow(
                            mapOf(
                                user1 to flowOf(roomUser(roomId2, user1)),
                                user2 to flowOf(roomUser(roomId2, user2))
                            )
                        )
                every { userServiceMock.getAll(roomId3) } returns
                        MutableStateFlow(
                            mapOf(
                                user1 to flowOf(roomUser(roomId3, user1)),
                                user3 to flowOf(roomUser(roomId3, user3))
                            )
                        )
                every { userServiceMock.getAll(roomId4) } returns
                        MutableStateFlow(mapOf(user1 to flowOf(roomUser(roomId4, user1))))
                every { userServiceMock.getAll(isEqual(roomId5)) } returns MutableStateFlow(emptyMap())
                every { userServiceMock.userPresence } returns MutableStateFlow(mapOf())

                every {
                    roomServiceMock.getState<CreateEventContent>(
                        isNot(listOf(roomId5, spaceId1, spaceId2)),
                        isAny(),
                        isAny()
                    )
                } returns flowOf(
                    StateEvent(
                        content = roomCreateEventContent,
                        id = EventId(""),
                        sender = user1,
                        roomId = roomId1,
                        originTimestamp = 0L,
                        stateKey = ""
                    )
                )
                every {
                    roomServiceMock.getState(
                        isAny(),
                        isEqual(JoinRulesEventContent::class),
                        isAny()
                    )
                } returns MutableStateFlow(
                    StateEvent(
                        content = JoinRulesEventContent(
                            joinRule = JoinRulesEventContent.JoinRule.Private
                        ),
                        EventId("1"),
                        user1,
                        roomId1,
                        0L,
                        stateKey = "",
                    )
                )

                every { onRoomSelectedMock.invoke(isAny(), isAny()) } returns Unit

                every {
                    roomNameMock.getRoomName(isRoomOf(roomId1), isEqual(matrixClientMock))
                } returns flowOf("room1")
                every {
                    roomNameMock.getRoomName(roomId1, matrixClientMock)
                } returns flowOf("room1")
                every {
                    roomNameMock.getRoomName(isRoomOf(roomId2), isEqual(matrixClientMock))
                } returns flowOf("room2")
                every {
                    roomNameMock.getRoomName(roomId2, matrixClientMock)
                } returns flowOf("room2")
                roomName3Mocker =
                    every { roomNameMock.getRoomName(isRoomOf(roomId3), isEqual(matrixClientMock)) }
                roomName3Mocker returns flowOf("room3-but-also-room2")
                every {
                    roomNameMock.getRoomName(roomId3, matrixClientMock)
                } returns flowOf("room3-but-also-room2")
                every {
                    roomNameMock.getRoomName(isRoomOf(roomId4), isEqual(matrixClientMock))
                } returns flowOf("room4")
                every {
                    roomNameMock.getRoomName(roomId4, matrixClientMock)
                } returns flowOf("room4")
                every {
                    roomNameMock.getRoomName(isRoomOf(roomId5), isEqual(matrixClientMock))
                } returns flowOf("room5")
                every {
                    roomNameMock.getRoomName(roomId5, matrixClientMock)
                } returns flowOf("room5")
                every {
                    roomNameMock.getRoomName(isRoomOf(spaceId1), isEqual(matrixClientMock))
                } returns
                        flowOf("space and beyond")
                every {
                    roomNameMock.getRoomName(isRoomOf(spaceId2), isEqual(matrixClientMock))
                } returns flowOf("space and beyond and beyonder")
                every { roomServiceMock.getLastTimelineEvent(isAny(), isAny()) } returns flowOf(null)

                every {
                    roomServiceMock.getAllState<ChildEventContent>(isEqual(spaceId1), isAny())
                } returns flowOf(mapOf())

                every { userServiceMock.getAccountData<DirectEventContent>() } returns
                        MutableStateFlow(
                            DirectEventContent(
                                mappings = mapOf(
                                    user2 to setOf(roomId1),
                                )
                            )
                        )
            }
        }

        should("sort rooms by last received message") {
            mocker.every {
                roomServiceMock.getState(roomId5, CreateEventContent::class, "")
            } returns
                    flowOf(
                        StateEvent(
                            CreateEventContent(user2),
                            EventId("\$event-a"),
                            user2,
                            roomId5,
                            Instant.parse("2021-11-04T17:00:00Z").toEpochMilliseconds(),
                            stateKey = ""
                        )
                    )
            val eventId1 = EventId("1")
            val room1 = Room(
                roomId1,
                createEventContent = roomCreateEventContent,
                lastRelevantEventId = eventId1,
                lastRelevantEventTimestamp = Instant.parse("2021-11-03T14:00:00Z")
            )
            val eventId2 = EventId("2")
            val room2 = Room(
                roomId2,
                createEventContent = roomCreateEventContent,
                lastRelevantEventId = eventId2,
                lastRelevantEventTimestamp = Instant.parse("2021-11-04T19:00:00Z")
            )
            val eventId3 = EventId("3")
            val room3 = Room(
                roomId3,
                createEventContent = roomCreateEventContent,
                lastRelevantEventId = eventId3,
                lastRelevantEventTimestamp = Instant.parse("2021-11-04T18:00:00Z")
            )
            val room4 = Room(roomId4, createEventContent = roomCreateEventContent)
            val room5 = Room(
                roomId5,
                createEventContent = roomCreateEventContent,
                lastRelevantEventTimestamp = null
            ) // with invite
            with(mocker) {
                every { roomServiceMock.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        roomId2 to MutableStateFlow(room2),
                        roomId3 to MutableStateFlow(room3),
                        roomId4 to MutableStateFlow(room4),
                        roomId5 to MutableStateFlow(room5),
                    )
                )
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock.getById(roomId3) } returns MutableStateFlow(room3)
                every { roomServiceMock.getById(roomId4) } returns MutableStateFlow(room4)
                every { roomServiceMock.getById(roomId5) } returns MutableStateFlow(room5)
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            val list = cut.sortedRoomListElementViewModels.onEach { println(it) }.first { it.size == 5 }
            list[0].roomId shouldBe roomId2
            list[1].roomId shouldBe roomId3
            list[2].roomId shouldBe roomId5
            list[3].roomId shouldBe roomId1
            list[4].roomId shouldBe roomId4

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set 'initialSyncFinished' to 'true' when the initial sync with the matrix server is completed") {
            mocker.every { roomServiceMock.getAll() } returns MutableStateFlow(emptyMap())
            val syncState = MutableStateFlow(SyncState.STARTED)
            syncStateMocker returns syncState

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            syncState.value = SyncState.INITIAL_SYNC
            testCoroutineScheduler.advanceUntilIdle()
            cut.initialSyncFinished.value shouldBe false
            syncState.value = SyncState.RUNNING
            testCoroutineScheduler.advanceUntilIdle()
            cut.initialSyncFinished.value shouldBe true

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("leave 'initialSyncFinished as 'false' when the client reconnects to the matrix server") {
            mocker.every { roomServiceMock.getAll() } returns MutableStateFlow(emptyMap())
            val syncState = MutableStateFlow(SyncState.RUNNING)
            syncStateMocker returns syncState

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()
            cut.initialSyncFinished.value shouldBe true

            syncState.value = SyncState.TIMEOUT
            testCoroutineScheduler.advanceUntilIdle()
            cut.initialSyncFinished.value shouldBe false

            syncState.value = SyncState.RUNNING
            testCoroutineScheduler.advanceUntilIdle()
            cut.initialSyncFinished.value shouldBe false

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("open a normal room on selection") {
            val room = Room(roomId1, createEventContent = roomCreateEventContent)
            mocker.every { roomServiceMock.getById(roomId1) } returns flowOf(room)
            mocker.every { roomServiceMock.getAll() } returns MutableStateFlow(
                mapOf(
                    roomId1 to MutableStateFlow(Room(roomId1))
                )
            )
            var joinedRoomWasCalled = false
            mocker.everySuspending {
                roomsApiClientMock.joinRoom(
                    isEqual(roomId1),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny()
                )
            } runs {
                joinedRoomWasCalled = true
                Result.success(roomId1)
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()
            cut.selectRoom(roomId1)
            testCoroutineScheduler.advanceUntilIdle()

            joinedRoomWasCalled shouldBe false
            mocker.verify(exhaustive = false) { onRoomSelectedMock.invoke(isAny(), isEqual(roomId1)) }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("join the room first and then open the room when the selected room is an invitation") {
            val room = Room(roomId1, membership = Membership.INVITE)
            mocker.every { roomServiceMock.getById(roomId1) } returns flowOf(room)
            mocker.every { roomServiceMock.getAll() } returns MutableStateFlow(
                mapOf(
                    roomId1 to MutableStateFlow(Room(roomId1))
                )
            )
            mocker.everySuspending {
                roomsApiClientMock.joinRoom(
                    isEqual(roomId1),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny()
                )
            } returns Result.success(roomId1)

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()
            cut.selectRoom(roomId1)
            testCoroutineScheduler.advanceUntilIdle()

            mocker.verifyWithSuspend(exhaustive = false) {
                roomsApiClientMock.joinRoom(isEqual(roomId1), isAny(), isAny(), isAny(), isAny())
                onRoomSelectedMock.invoke(isAny(), isEqual(roomId1))
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("display an error message when the selected room is an invitation and the join fails") {
            val room = Room(roomId1, membership = Membership.INVITE)
            mocker.every { roomServiceMock.getById(roomId1) } returns flowOf(room)
            mocker.every { roomServiceMock.getAll() } returns MutableStateFlow(
                mapOf(
                    roomId1 to MutableStateFlow(Room(roomId1))
                )
            )
            mocker.everySuspending {
                roomsApiClientMock.joinRoom(
                    isEqual(roomId1),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny()
                )
            } returns Result.failure(RuntimeException("Oh no!"))

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()
            cut.selectRoom(roomId1)
            testCoroutineScheduler.advanceUntilIdle()

            cut.error.value shouldNotBe null
            cut.errorType.value shouldBe ErrorType.WITH_ACTION

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("display info message when trying to join a room while the client is not connected to the server") {
            val room = Room(roomId1, membership = Membership.INVITE)
            mocker.every { roomServiceMock.getById(roomId1) } returns flowOf(room)
            mocker.every { roomServiceMock.getAll() } returns MutableStateFlow(
                mapOf(
                    roomId1 to MutableStateFlow(Room(roomId1))
                )
            )
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
            syncStateMocker returns syncState

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

        should("open a normal room on redirect") {
            val room = Room(roomId1, membership = Membership.JOIN)
            mocker.every { roomServiceMock.getById(roomId1) } returns flowOf(room)
            mocker.every { roomServiceMock.getAll() } returns MutableStateFlow(
                mapOf(
                    roomId1 to MutableStateFlow(Room(roomId1))
                )
            )
            var joinedRoomWasCalled = false
            mocker.everySuspending {
                roomsApiClientMock.joinRoom(
                    isEqual(roomId1),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny()
                )
            } runs {
                joinedRoomWasCalled = true
                Result.success(roomId1)
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()
            cut.redirectRoom(roomId1)
            testCoroutineScheduler.advanceUntilIdle()

            joinedRoomWasCalled shouldBe false
            mocker.verify(exhaustive = false) { onRoomSelectedMock.invoke(isAny(), isEqual(roomId1)) }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("pass invalid room") {
            val room = Room(roomId1, membership = Membership.JOIN)
            mocker.every { roomServiceMock.getById(roomId1) } returns flowOf(room)
            mocker.every { roomServiceMock.getAll() } returns MutableStateFlow(
                mapOf(
                    roomId1 to MutableStateFlow(Room(roomId1))
                )
            )
            var joinedRoomWasCalled = false
            mocker.everySuspending {
                roomsApiClientMock.joinRoom(
                    isEqual(roomId1),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny()
                )
            } runs {
                joinedRoomWasCalled = true
                Result.success(roomId1)
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()
            cut.redirectRoom(roomId1)
            cut.redirectRoom(roomId3)
            testCoroutineScheduler.advanceUntilIdle()

            joinedRoomWasCalled shouldBe false
            mocker.verify(exhaustive = false) { onRoomSelectedMock.invoke(isAny(), isEqual(roomId1)) }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("log when trying to join a unjoined room") {
            val room = Room(roomId1, membership = Membership.LEAVE)
            mocker.every { roomServiceMock.getById(roomId1) } returns flowOf(room)
            mocker.every { roomServiceMock.getAll() } returns MutableStateFlow(
                mapOf(
                    roomId1 to MutableStateFlow(Room(roomId1))
                )
            )

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()
            cut.redirectRoom(roomId1)
            testCoroutineScheduler.advanceUntilIdle()

            shouldThrow<MockerVerificationAssertionError> {
                mocker.verify(exhaustive = false) { onRoomSelectedMock.invoke(isAny(), isNotEqual(roomId1)) }
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("not show search initially") {
            mocker.every { roomServiceMock.getAll() } returns MutableStateFlow(mapOf())
            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.showSearch.value shouldBe false

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("yield all rooms as search result when search term which is blank") {
            val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
            val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
            with(mocker) {
                every { roomServiceMock.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        roomId2 to MutableStateFlow(room2)
                    )
                )
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
            }
            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.searchTerm.value = ""
            testCoroutineScheduler.advanceUntilIdle()
            cut.sortedRoomListElementViewModels.value shouldHaveSize 2

            cut.searchTerm.value = "  "
            testCoroutineScheduler.runCurrent()
            cut.sortedRoomListElementViewModels.value shouldHaveSize 2

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("contain search term in all search results") {
            val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
            val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
            val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
            with(mocker) {
                every { roomServiceMock.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        roomId2 to MutableStateFlow(room2),
                        roomId3 to MutableStateFlow(room3)
                    )
                )
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock.getById(roomId3) } returns MutableStateFlow(room3)
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

        should("change search results when the search term changes") {
            val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
            val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
            val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
            with(mocker) {
                every { roomServiceMock.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        roomId2 to MutableStateFlow(room2),
                        roomId3 to MutableStateFlow(room3)
                    )
                )
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock.getById(roomId3) } returns MutableStateFlow(room3)
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.searchTerm.value = "2"
            testCoroutineScheduler.advanceTimeBy(500) // debounce
            testCoroutineScheduler.runCurrent()
            cut.searchTerm.value = "1"
            testCoroutineScheduler.advanceTimeBy(500) // debounce
            testCoroutineScheduler.runCurrent()

            cut.sortedRoomListElementViewModels.value.should(containRoomListElementViewModelsFor(listOf(roomId1)))

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("show a newly added room that fits the ongoing search term") {
            val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
            val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
            val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
            val roomList = MutableStateFlow(
                mapOf(
                    roomId1 to MutableStateFlow(room1),
                    roomId2 to MutableStateFlow(room2),
                )
            )
            with(mocker) {
                every { roomServiceMock.getAll() } returns roomList
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock.getById(roomId3) } returns MutableStateFlow(room3)
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.searchTerm.value = "2"
            testCoroutineScheduler.advanceTimeBy(500) // debounce
            testCoroutineScheduler.runCurrent()
            cut.sortedRoomListElementViewModels.value.should(containRoomListElementViewModelsFor(listOf(roomId2)))

            roomList.value = mapOf(
                roomId1 to MutableStateFlow(room1),
                roomId2 to MutableStateFlow(room2),
                roomId3 to MutableStateFlow(room3),
            )
            testCoroutineScheduler.advanceUntilIdle() // no debounce, since search term stays the same
            cut.sortedRoomListElementViewModels.value.should(
                containRoomListElementViewModelsFor(listOf(roomId2, roomId3))
            )

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("remove room from search result when it is removed") {
            val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
            val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
            val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
            val roomList = MutableStateFlow(
                mapOf(
                    roomId1 to MutableStateFlow(room1),
                    roomId2 to MutableStateFlow(room2),
                    roomId3 to MutableStateFlow(room3),
                )
            )
            with(mocker) {
                every { roomServiceMock.getAll() } returns roomList
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock.getById(roomId3) } returns MutableStateFlow(room3)
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.searchTerm.value = "2"
            testCoroutineScheduler.advanceTimeBy(500) // debounce
            testCoroutineScheduler.runCurrent()
            cut.sortedRoomListElementViewModels.value.should(
                containRoomListElementViewModelsFor(listOf(roomId2, roomId3))
            )

            roomList.value = mapOf(
                roomId1 to MutableStateFlow(room1),
                roomId2 to MutableStateFlow(room2),
            )
            testCoroutineScheduler.advanceUntilIdle() // no debounce, since search term stays the same
            cut.sortedRoomListElementViewModels.value.should(containRoomListElementViewModelsFor(listOf(roomId2)))

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("remove a room from the search result when its name changes and it no longer fits the search term") {
            val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
            val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
            val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
            val room3NameFlow = MutableStateFlow("room2-other")
            with(mocker) {
                every { roomServiceMock.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        roomId2 to MutableStateFlow(room2),
                        roomId3 to MutableStateFlow(room3),
                    )
                )
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock.getById(roomId3) } returns MutableStateFlow(room3)
                roomName3Mocker returns room3NameFlow
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.searchTerm.value = "2"
            testCoroutineScheduler.advanceTimeBy(500) // debounce
            testCoroutineScheduler.runCurrent()

            room3NameFlow.value = "completely different"
            testCoroutineScheduler.advanceUntilIdle() // no debounce, since search term stays the same
            cut.sortedRoomListElementViewModels.value.should(containRoomListElementViewModelsFor(listOf(roomId2)))

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("add a newly added room to the search result when it fits the search term") {
            val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
            val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
            val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
            val room3NameFlow = MutableStateFlow("room2-other")
            with(mocker) {
                every { roomServiceMock.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        roomId2 to MutableStateFlow(room2),
                        roomId3 to MutableStateFlow(room3),
                    )
                )
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock.getById(roomId3) } returns MutableStateFlow(room3)
                every {
                    roomNameMock.getRoomName(isEqual(room3), isEqual(matrixClientMock))
                } returns room3NameFlow
                roomName3Mocker returns room3NameFlow
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.searchTerm.value = "1"
            testCoroutineScheduler.advanceTimeBy(500) // debounce
            testCoroutineScheduler.runCurrent()
            cut.sortedRoomListElementViewModels.value.should(containRoomListElementViewModelsFor(listOf(roomId1)))

            room3NameFlow.value = "I am number 1"
            testCoroutineScheduler.advanceUntilIdle() // no debounce, since search term stays the same
            cut.sortedRoomListElementViewModels.value.should(
                containRoomListElementViewModelsFor(listOf(roomId1, roomId3))
            )

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("not show spaces in room list") {
            val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
            val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
            val space = Room(spaceId1, createEventContent = spaceCreateEventContent)
            val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
            with(mocker) {
                every { roomServiceMock.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        roomId2 to MutableStateFlow(room2),
                        spaceId1 to MutableStateFlow(space),
                        roomId3 to MutableStateFlow(room3),
                    )
                )
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock.getById(spaceId1) } returns MutableStateFlow(space)
                every { roomServiceMock.getById(roomId3) } returns MutableStateFlow(room3)
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.sortedRoomListElementViewModels.value shouldHaveSize 3

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("consider all spaces") {
            val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
            val space1 = Room(spaceId1, createEventContent = spaceCreateEventContent)
            val space2 = Room(spaceId2, createEventContent = spaceCreateEventContent)
            with(mocker) {
                every { roomServiceMock.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        spaceId1 to MutableStateFlow(space1),
                        spaceId2 to MutableStateFlow(space2),
                    )
                )
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock.getById(spaceId1) } returns MutableStateFlow(space1)
                every { roomServiceMock.getById(spaceId2) } returns MutableStateFlow(space2)
                every { roomServiceMock.getState<CreateEventContent>(spaceId1, "") } returns
                        flowOf(
                            StateEvent(
                                CreateEventContent(
                                    creator = user1,
                                    federate = false,
                                    roomVersion = "",
                                    type = RoomType.Space,
                                ),
                                EventId(""), UserId(""), RoomId(""), 0L, stateKey = ""
                            )
                        )
                every { roomServiceMock.getState<CreateEventContent>(spaceId2, "") } returns
                        flowOf(
                            StateEvent(
                                CreateEventContent(
                                    creator = user1,
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
            val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
            val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
            val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
            val room4 = Room(roomId4, createEventContent = roomCreateEventContent)
            val space1 = Room(spaceId1, createEventContent = spaceCreateEventContent)
            val space2 = Room(spaceId2, createEventContent = spaceCreateEventContent)
            with(mocker) {
                every { roomServiceMock.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        roomId2 to MutableStateFlow(room2),
                        roomId3 to MutableStateFlow(room3),
                        roomId4 to MutableStateFlow(room4),
                        spaceId1 to MutableStateFlow(space1),
                        spaceId2 to MutableStateFlow(space2),
                    )
                )
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock.getById(roomId3) } returns MutableStateFlow(room3)
                every { roomServiceMock.getById(roomId4) } returns MutableStateFlow(room4)
                every { roomServiceMock.getById(spaceId1) } returns MutableStateFlow(space1)
                every { roomServiceMock.getById(spaceId2) } returns MutableStateFlow(space2)
                every { roomServiceMock.getState<CreateEventContent>(spaceId1, "") } returns
                        flowOf(
                            StateEvent(
                                CreateEventContent(
                                    creator = user1,
                                    federate = false,
                                    roomVersion = "",
                                    type = RoomType.Space,
                                ),
                                EventId(""), UserId(""), RoomId(""), 0L, stateKey = ""
                            )
                        )
                every { roomServiceMock.getState<CreateEventContent>(spaceId2, "") } returns
                        flowOf(
                            StateEvent(
                                CreateEventContent(
                                    creator = user1,
                                    federate = false,
                                    roomVersion = "",
                                    type = RoomType.Space,
                                ),
                                EventId(""), UserId(""), RoomId(""), 0L, stateKey = ""
                            )
                        )

                every {
                    roomServiceMock.getAllState<ChildEventContent>(isEqual(spaceId2), isAny())
                } returns
                        flowOf(
                            mapOf(
                                roomId2.full to flowOf(spaceChildEvent(spaceId2, roomId2)),
                                roomId3.full to flowOf(spaceChildEvent(spaceId2, roomId3)),
                            )
                        )

                every { userServiceMock.getAll(spaceId1) } returns MutableStateFlow(emptyMap())
                every { userServiceMock.getAll(spaceId2) } returns MutableStateFlow(emptyMap())
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

        should("also show direct rooms with people that are members of the selected space") {
            val room1 = Room(roomId1, createEventContent = roomCreateEventContent, isDirect = true)
            val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
            val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
            val room4 = Room(roomId4, createEventContent = roomCreateEventContent)
            val space2 = Room(spaceId2, createEventContent = spaceCreateEventContent)
            with(mocker) {
                every { roomServiceMock.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        roomId2 to MutableStateFlow(room2),
                        roomId3 to MutableStateFlow(room3),
                        roomId4 to MutableStateFlow(room4),
                        spaceId2 to MutableStateFlow(space2),
                    )
                )
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock.getById(roomId3) } returns MutableStateFlow(room3)
                every { roomServiceMock.getById(roomId4) } returns MutableStateFlow(room4)
                every { roomServiceMock.getById(spaceId2) } returns MutableStateFlow(space2)
                every { roomServiceMock.getState<CreateEventContent>(spaceId2, "") } returns
                        flowOf(
                            StateEvent(
                                CreateEventContent(
                                    creator = user1,
                                    federate = false,
                                    roomVersion = "",
                                    type = RoomType.Space,
                                ),
                                EventId(""), UserId(""), RoomId(""), 0L, stateKey = ""
                            )
                        )

                every {
                    roomServiceMock.getAllState<ChildEventContent>(isEqual(spaceId2), isAny())
                } returns
                        flowOf(
                            mapOf(
                                roomId2.full to flowOf(spaceChildEvent(spaceId2, roomId2)),
                                roomId3.full to flowOf(spaceChildEvent(spaceId2, roomId3)),
                            )
                        )

                every { userServiceMock.getAll(spaceId2) } returns
                        MutableStateFlow(
                            mapOf(
                                user1 to flowOf(roomUser(spaceId2, user1)),
                                user2 to flowOf(roomUser(spaceId2, user2)),
                                user3 to flowOf(roomUser(spaceId2, user3))
                            )
                        )
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()
            cut.activeSpace.value = space2.roomId
            cut.sortedRoomListElementViewModels.first {
                println("... $it")
                it.size == 3
            }
            cut.sortedRoomListElementViewModels.value[0].roomId shouldBe roomId1

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("only show rooms, spaces and direct chats of selected account") {
            val roomId21 = RoomId("room21", "localhost") // direct room
            val roomId22 = RoomId("room22", "localhost") // group
            val roomId23 = RoomId("room23", "localhost") // group
            val spaceId21 = RoomId("space21", "localhost") // space with room23
            with(mocker) {
                every { matrixClientMock2.di } returns koinApplication {
                    modules(
                        module {
                            single { roomServiceMock2 }
                            single { userServiceMock2 }
                        }
                    )
                }.koin
                every { matrixClientMock2.userId } returns user2
                every { matrixClientMock2.room } returns roomServiceMock2
                every { matrixClientMock2.user } returns userServiceMock2
                every { matrixClientMock2.api } returns matrixClientServerApiClientMock
                every { matrixClientMock2.syncState } returns MutableStateFlow(SyncState.RUNNING)

                every { userServiceMock2.getById(isEqual(roomId21), isEqual(user1)) } returns
                        MutableStateFlow(roomUser(roomId21, user1))
                every { userServiceMock2.getById(isEqual(roomId22), isEqual(user1)) } returns
                        MutableStateFlow(roomUser(roomId22, user1))
                every { userServiceMock2.getById(isEqual(roomId21), isEqual(user2)) } returns user2Flow
                every { userServiceMock2.getById(isAny(), isEqual(user2)) } returns
                        MutableStateFlow(roomUser(roomId22, user2))
                every { userServiceMock2.getById(isEqual(roomId21), isEqual(user3)) } returns
                        MutableStateFlow(roomUser(roomId21, user3))

                every { userServiceMock2.getAll(roomId21) } returns
                        MutableStateFlow(
                            mapOf(
                                user1 to flowOf(roomUser(roomId21, user1)),
                                user2 to flowOf(roomUser(roomId21, user2))
                            )
                        )
                every { userServiceMock2.getAll(roomId22) } returns
                        MutableStateFlow(
                            mapOf(
                                user1 to flowOf(roomUser(roomId22, user1)),
                                user2 to flowOf(roomUser(roomId22, user2))
                            )
                        )
                every { userServiceMock2.getAll(roomId23) } returns
                        MutableStateFlow(
                            mapOf(
                                user1 to flowOf(roomUser(roomId23, user1)),
                                user3 to flowOf(roomUser(roomId23, user3))
                            )
                        )
                every { userServiceMock2.userPresence } returns MutableStateFlow(mapOf())

                every {
                    roomServiceMock2.getState<CreateEventContent>(
                        isNot(listOf(spaceId2, spaceId21)),
                        isAny(),
                        isAny()
                    )
                } returns flowOf(
                    StateEvent(
                        content = CreateEventContent(creator = user1),
                        id = EventId(""),
                        sender = user1,
                        roomId = roomId1,
                        originTimestamp = 0L,
                        stateKey = ""
                    )
                )

                every {
                    roomNameMock.getRoomName(isAny<Room>(), isAny())
                } returns flowOf("room")
                every {
                    roomNameMock.getRoomName(isAny<RoomId>(), isAny())
                } returns flowOf("room")
                every { roomServiceMock2.getLastTimelineEvent(isAny(), isAny()) } returns flowOf(null)

                every { userServiceMock2.getAccountData<DirectEventContent>() } returns
                        MutableStateFlow(
                            DirectEventContent(
                                mappings = mapOf(
                                    user2 to setOf(roomId21),
                                )
                            )
                        )
            }

            val room1 = Room(roomId1, createEventContent = roomCreateEventContent, isDirect = true)
            val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
            val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
            val room4 = Room(roomId4, createEventContent = roomCreateEventContent)
            val room21 = Room(roomId21, createEventContent = roomCreateEventContent, isDirect = true)
            val room22 = Room(roomId22, createEventContent = roomCreateEventContent)
            val room23 = Room(roomId23, createEventContent = roomCreateEventContent)
            val space2 = Room(spaceId2, createEventContent = spaceCreateEventContent)
            val space21 = Room(spaceId21, createEventContent = spaceCreateEventContent)
            with(mocker) {
                every { roomServiceMock.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId1 to MutableStateFlow(room1),
                        roomId2 to MutableStateFlow(room2),
                        roomId3 to MutableStateFlow(room3),
                        roomId4 to MutableStateFlow(room4),
                        spaceId2 to MutableStateFlow(space2),
                    )
                )
                every { roomServiceMock2.getAll() } returns MutableStateFlow(
                    mapOf(
                        roomId21 to MutableStateFlow(room21),
                        roomId22 to MutableStateFlow(room22),
                        roomId23 to MutableStateFlow(room23),
                        spaceId21 to MutableStateFlow(space21),
                    )
                )
                every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
                every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
                every { roomServiceMock.getById(roomId3) } returns MutableStateFlow(room3)
                every { roomServiceMock.getById(roomId4) } returns MutableStateFlow(room4)
                every { roomServiceMock.getById(spaceId2) } returns MutableStateFlow(space2)
                every { roomServiceMock2.getById(roomId21) } returns MutableStateFlow(room21)
                every { roomServiceMock2.getById(roomId22) } returns MutableStateFlow(room22)
                every { roomServiceMock2.getById(roomId23) } returns MutableStateFlow(room23)
                every { roomServiceMock2.getById(spaceId21) } returns MutableStateFlow(space21)

                every { roomServiceMock.getState<CreateEventContent>(spaceId2, "") } returns
                        flowOf(
                            StateEvent(
                                CreateEventContent(
                                    creator = user1,
                                    federate = false,
                                    roomVersion = "",
                                    type = RoomType.Space,
                                ),
                                EventId(""), UserId(""), RoomId(""), 0L, stateKey = ""
                            )
                        )
                every { roomServiceMock2.getState<CreateEventContent>(spaceId21, "") } returns
                        flowOf(
                            StateEvent(
                                CreateEventContent(
                                    creator = user1,
                                    federate = false,
                                    roomVersion = "",
                                    type = RoomType.Space,
                                ),
                                EventId(""), UserId(""), RoomId(""), 0L, stateKey = ""
                            )
                        )

                every {
                    roomServiceMock.getAllState<ChildEventContent>(isEqual(spaceId2), isAny())
                } returns
                        flowOf(
                            mapOf(
                                roomId2.full to flowOf(spaceChildEvent(spaceId2, roomId2)),
                                roomId3.full to flowOf(spaceChildEvent(spaceId2, roomId3)),
                            )
                        )
                every {
                    roomServiceMock2.getAllState<ChildEventContent>(isEqual(spaceId2), isAny())
                } returns flowOf(mapOf())
                every {
                    roomServiceMock2.getAllState<ChildEventContent>(isEqual(spaceId21), isAny())
                } returns
                        flowOf(
                            mapOf(
                                roomId23.full to flowOf(spaceChildEvent(spaceId21, roomId23)),
                            )
                        )

                every { userServiceMock.getAll(spaceId2) } returns
                        MutableStateFlow(
                            mapOf(
                                user1 to flowOf(roomUser(spaceId2, user1)),
                                user2 to flowOf(roomUser(spaceId2, user2)),
                                user3 to flowOf(roomUser(spaceId2, user3)),
                            )
                        )
                every { userServiceMock2.getAll(spaceId2) } returns MutableStateFlow(mapOf())
                every { userServiceMock2.getAll(spaceId21) } returns MutableStateFlow(mapOf())
            }

            val cut = roomListViewModel(
                coroutineContext,
                mapOf(
                    user1 to matrixClientMock,
                    user2 to matrixClientMock2
                )
            )
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            // all rooms, spaces, etc. are visible
            cut.sortedRoomListElementViewModels.first {
                println("(1) ... ${it.map { it.roomId }}")
                it.size == 7
            }
            cut.activeSpace.value = spaceId2
            cut.sortedRoomListElementViewModels.first {
                println("spaces ... ${it.map { it.roomId }}")
                it.size == 4 // includes direct room (room1 for test, room21 for test2)
            }
            cut.activeSpace.value = null

            cut.accountViewModel.selectActiveAccount(user2)
            testCoroutineScheduler.advanceUntilIdle()
            // only rooms, spaces, etc. of account 'test2' are visible
            cut.sortedRoomListElementViewModels.first {
                println("(2) ... ${it.map { it.roomId }}")
                it.size == 3
            }
            cut.spaces.first {
                println("spaces... ${it.map { it.roomId }}")
                it.size == 1
            }
            cut.activeSpace.value = spaceId21
            testCoroutineScheduler.advanceUntilIdle()
            cut.sortedRoomListElementViewModels.first {
                println("(3) ... ${it.map { it.roomId }}")
                it.size == 1 // only room23 is in space21
            }

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

    private suspend fun roomListViewModel(
        coroutineContext: CoroutineContext,
        matrixClients: Map<UserId, MatrixClient> = mapOf(user1 to matrixClientMock),
    ): RoomListViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        val koin = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(matrixClients) + module {
                    single { roomNameMock }
                    single<AccountViewModelFactory> {
                        object : AccountViewModelFactory {
                            override fun create(
                                viewModelContext: ViewModelContext,
                                onAccountSelected: (UserId?) -> Unit,
                                onUserSettingsSelected: () -> Unit,
                                onShowAppInfo: () -> Unit
                            ): AccountViewModel {
                                return object : AccountViewModel {
                                    override val activeAccount: StateFlow<UserId?> = MutableStateFlow(null)
                                    override val accounts: StateFlow<List<AccountInfo>> =
                                        MutableStateFlow(listOf())

                                    override fun selectActiveAccount(userId: UserId?) {
                                        GlobalScope.launch {
                                            get<MatrixMessengerSettingsHolder>().update {
                                                it.copy(selectedAccount = userId)
                                            }
                                        }
                                        onAccountSelected(userId) // needed to influence RoomListViewModel
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
        koin.createScope<RootViewModelImpl>()
        return RoomListViewModelImpl(
            viewModelContext = ViewModelContextImpl(
                componentContext = DefaultComponentContext(lifecycleRegistry),
                di = koin,
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
        KoMatcher<List<RoomListElement>> { list ->
            MatcherResult(roomIds.all { roomId ->
                list.any { element -> element.viewModel.roomId == roomId }
            },
                {
                    "RoomListElementViewModel with ids [${
                        roomIds.filterNot { roomId -> list.any { element -> element.viewModel.roomId == roomId } }
                            .joinToString { it.full }
                    }] not found"
                },
                {
                    "RoomListElementViewModel with ids [${
                        roomIds.filterNot { roomId -> list.any { element -> element.viewModel.roomId == roomId } }
                            .joinToString { it.full }
                    }] not found"
                })
        }

    private fun roomUser(roomId: RoomId, userId: UserId) = RoomUser(
        roomId,
        userId,
        "user1",
        memberEvent(roomId, userId)
    )

    private fun memberEvent(roomId: RoomId, sender: UserId) = StateEvent(
        content = MemberEventContent(membership = Membership.JOIN),
        id = EventId("1"),
        sender = sender,
        roomId = roomId,
        originTimestamp = 0L,
        stateKey = "",
    )

    private fun spaceChildEvent(spaceId: RoomId, containedId: RoomId) =
        StateEvent(
            content = ChildEventContent(),
            id = EventId(""),
            sender = user1,
            roomId = spaceId,
            originTimestamp = 0L,
            stateKey = containedId.full,
        )
}
