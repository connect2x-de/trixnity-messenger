package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.isNot
import de.connect2x.trixnity.messenger.isRoomOf
import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.viewmodel.AccountInfo
import de.connect2x.trixnity.messenger.viewmodel.RootViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.ErrorType
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationTrigger
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationTriggerImpl
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifySuspend
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.DeviceTrustLevel
import net.folivo.trixnity.client.key.KeyService
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.DirectEventContent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent.RoomType
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.space.ChildEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds
import io.kotest.matchers.Matcher as KoMatcher

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomListViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

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

    val matrixClientMock = mock<MatrixClient>()

    val matrixClientMock2 = mock<MatrixClient>()

    val userServiceMock = mock<UserService>()

    val userServiceMock2 = mock<UserService>()

    val roomServiceMock = mock<RoomService>()

    val roomServiceMock2 = mock<RoomService>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    val roomNameMock = mock<RoomName>()

    val profileManagerMock = mock<ProfileManager>()

    val keyServiceMock = mock<KeyService>()

    val keyServiceMock2 = mock<KeyService>()

    private val onRoomSelectedMock = mock<Function2<UserId, RoomId, Unit>>()
    private val onAccountSelected = mock<Function0<Unit>>()

    lateinit var syncStateMocker: BlockingAnsweringScope<StateFlow<SyncState>>
    lateinit var roomName3Mocker: BlockingAnsweringScope<Flow<String>>

    private val roomCreateEventContent = CreateEventContent(creator = user1, type = RoomType.Room)
    private val spaceCreateEventContent = CreateEventContent(creator = user1, type = RoomType.Space)

    init {
        coroutineTestScope = true

        beforeTest {

            lifecycleRegistry = LifecycleRegistry()
            lifecycleRegistry.resume()

            resetMocks(
                matrixClientMock,
                matrixClientMock2,
                userServiceMock,
                userServiceMock2,
                roomServiceMock,
                roomServiceMock2,
                matrixClientServerApiClientMock,
                roomsApiClientMock,
                roomNameMock,
                profileManagerMock,
                keyServiceMock,
                keyServiceMock2,
                onRoomSelectedMock,
                onAccountSelected,
            )

            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock }
                        single { userServiceMock }
                        single { keyServiceMock }
                    }
                )
            }.koin
            syncStateMocker = every { matrixClientMock.syncState }
            syncStateMocker returns MutableStateFlow(SyncState.RUNNING)
            every { matrixClientMock.userId } returns user1
            every { matrixClientMock.api } returns matrixClientServerApiClientMock
            every { matrixClientServerApiClientMock.room } returns roomsApiClientMock

            every { userServiceMock.getById(eq(roomId1), eq(user1)) } returns
                    MutableStateFlow(roomUser(roomId1, user1))
            every { userServiceMock.getById(eq(roomId2), eq(user1)) } returns
                    MutableStateFlow(roomUser(roomId2, user1))
            every { userServiceMock.getById(eq(roomId1), eq(user2)) } returns user2Flow
            every { userServiceMock.getById(any(), eq(user2)) } returns
                    MutableStateFlow(roomUser(roomId2, user2))
            every { userServiceMock.getById(eq(roomId1), eq(user3)) } returns
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
            every { userServiceMock.getAll(eq(roomId5)) } returns MutableStateFlow(emptyMap())
            every { userServiceMock.userPresence } returns MutableStateFlow(mapOf())

            every {
                roomServiceMock.getState(
                    isNot(listOf(roomId5, spaceId1, spaceId2)),
                    CreateEventContent::class,
                    any()
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
                    any(),
                    eq(JoinRulesEventContent::class),
                    any()
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

            every { onRoomSelectedMock.invoke(any(), any()) } returns Unit

            every { onAccountSelected.invoke() } returns Unit

            every {
                roomNameMock.getRoomName(isRoomOf(roomId1), eq(matrixClientMock), any())
            } returns flowOf("room1")
            every {
                roomNameMock.getRoomName(eq(roomId1), eq(matrixClientMock), any())
            } returns flowOf("room1")
            every {
                roomNameMock.getRoomName(isRoomOf(roomId2), eq(matrixClientMock), any())
            } returns flowOf("room2")
            every {
                roomNameMock.getRoomName(eq(roomId2), eq(matrixClientMock), any())
            } returns flowOf("room2")
            roomName3Mocker =
                every { roomNameMock.getRoomName(isRoomOf(roomId3), eq(matrixClientMock), any()) }
            roomName3Mocker returns flowOf("room3-but-also-room2")
            every {
                roomNameMock.getRoomName(eq(roomId3), eq(matrixClientMock), any())
            } returns flowOf("room3-but-also-room2")
            every {
                roomNameMock.getRoomName(isRoomOf(roomId4), eq(matrixClientMock), any())
            } returns flowOf("room4")
            every {
                roomNameMock.getRoomName(eq(roomId4), eq(matrixClientMock), any())
            } returns flowOf("room4")
            every {
                roomNameMock.getRoomName(isRoomOf(roomId5), eq(matrixClientMock), any())
            } returns flowOf("room5")
            every {
                roomNameMock.getRoomName(eq(roomId5), eq(matrixClientMock), any())
            } returns flowOf("room5")
            every {
                roomNameMock.getRoomName(isRoomOf(spaceId1), eq(matrixClientMock), any())
            } returns
                    flowOf("space and beyond")
            every {
                roomNameMock.getRoomName(isRoomOf(spaceId2), eq(matrixClientMock), any())
            } returns flowOf("space and beyond and beyonder")

            every {
                roomServiceMock.getAllState(eq(spaceId1), ChildEventContent::class)
            } returns flowOf(mapOf())

            every { userServiceMock.getAccountData(DirectEventContent::class) } returns
                    MutableStateFlow(
                        DirectEventContent(
                            mappings = mapOf(
                                user2 to setOf(roomId1),
                            )
                        )
                    )

            every { profileManagerMock.profiles } returns MutableStateFlow(emptyMap())
            everySuspend { profileManagerMock.closeProfile() } returns Unit
        }

        should("sort rooms by last received message") {
            every {
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

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            val list = cut.elements.onEach { println(it) }.first { it.size == 5 }
            list[0].roomId shouldBe roomId2
            list[1].roomId shouldBe roomId3
            list[2].roomId shouldBe roomId5
            list[3].roomId shouldBe roomId1
            list[4].roomId shouldBe roomId4

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set 'initialSyncFinished' to 'true' when the initial sync with the matrix server is completed") {
            every { roomServiceMock.getAll() } returns MutableStateFlow(emptyMap())
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
            every { roomServiceMock.getAll() } returns MutableStateFlow(emptyMap())
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
            every { roomServiceMock.getById(roomId1) } returns flowOf(room)
            every { roomServiceMock.getAll() } returns MutableStateFlow(
                mapOf(
                    roomId1 to MutableStateFlow(Room(roomId1))
                )
            )
            var joinedRoomWasCalled = false
            everySuspend {
                roomsApiClientMock.joinRoom(
                    eq(roomId1),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } calls {
                joinedRoomWasCalled = true
                Result.success(roomId1)
            }

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()
            cut.selectRoom(roomId1)
            testCoroutineScheduler.advanceUntilIdle()

            joinedRoomWasCalled shouldBe false
            verify { onRoomSelectedMock.invoke(any(), eq(roomId1)) }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("join the room first and then open the room when the selected room is an invitation") {
            val room = Room(roomId1, membership = Membership.INVITE)
            every { roomServiceMock.getById(roomId1) } returns flowOf(room)
            every { roomServiceMock.getAll() } returns MutableStateFlow(
                mapOf(
                    roomId1 to MutableStateFlow(Room(roomId1))
                )
            )
            everySuspend {
                roomsApiClientMock.joinRoom(
                    eq(roomId1),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns Result.success(roomId1)

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()
            cut.selectRoom(roomId1)
            testCoroutineScheduler.advanceUntilIdle()

            verifySuspend {
                roomsApiClientMock.joinRoom(eq(roomId1), any(), any(), any(), any())
                onRoomSelectedMock.invoke(any(), eq(roomId1))
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("display an error message when the selected room is an invitation and the join fails") {
            val room = Room(roomId1, membership = Membership.INVITE)
            every { roomServiceMock.getById(roomId1) } returns flowOf(room)
            every { roomServiceMock.getAll() } returns MutableStateFlow(
                mapOf(
                    roomId1 to MutableStateFlow(Room(roomId1))
                )
            )
            everySuspend {
                roomsApiClientMock.joinRoom(
                    eq(roomId1),
                    any(),
                    any(),
                    any(),
                    any()
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
            every { roomServiceMock.getById(roomId1) } returns flowOf(room)
            every { roomServiceMock.getAll() } returns MutableStateFlow(
                mapOf(
                    roomId1 to MutableStateFlow(Room(roomId1))
                )
            )
            everySuspend {
                roomsApiClientMock.joinRoom(
                    eq(roomId1),
                    any(),
                    any(),
                    any(),
                    any(),
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
        should("not show search initially") {
            every { roomServiceMock.getAll() } returns MutableStateFlow(mapOf())
            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.showSearch.value shouldBe false

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("yield all rooms as search result when search term which is blank") {
            val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
            val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
            every { roomServiceMock.getAll() } returns MutableStateFlow(
                mapOf(
                    roomId1 to MutableStateFlow(room1),
                    roomId2 to MutableStateFlow(room2)
                )
            )
            every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
            every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.searchTerm.value = ""
            testCoroutineScheduler.advanceUntilIdle()
            cut.elements.value shouldHaveSize 2

            cut.searchTerm.value = "  "
            testCoroutineScheduler.runCurrent()
            cut.elements.value shouldHaveSize 2

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("contain search term in all search results") {
            val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
            val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
            val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
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

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.searchTerm.value = "2"
            testCoroutineScheduler.advanceTimeBy(500) // debounce
            testCoroutineScheduler.runCurrent()

            cut.elements.value shouldHaveSize 2
            cut.elements.value.should(
                containRoomListElementViewModelsFor(listOf(roomId2, roomId3))
            )

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("change search results when the search term changes") {
            val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
            val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
            val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
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

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.searchTerm.value = "2"
            testCoroutineScheduler.advanceTimeBy(500) // debounce
            testCoroutineScheduler.runCurrent()
            cut.searchTerm.value = "1"
            testCoroutineScheduler.advanceTimeBy(500) // debounce
            testCoroutineScheduler.runCurrent()

            cut.elements.value.should(containRoomListElementViewModelsFor(listOf(roomId1)))

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
            every { roomServiceMock.getAll() } returns roomList
            every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
            every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
            every { roomServiceMock.getById(roomId3) } returns MutableStateFlow(room3)

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.searchTerm.value = "2"
            testCoroutineScheduler.advanceTimeBy(500) // debounce
            testCoroutineScheduler.runCurrent()
            cut.elements.value.should(containRoomListElementViewModelsFor(listOf(roomId2)))

            roomList.value = mapOf(
                roomId1 to MutableStateFlow(room1),
                roomId2 to MutableStateFlow(room2),
                roomId3 to MutableStateFlow(room3),
            )
            testCoroutineScheduler.advanceUntilIdle() // no debounce, since search term stays the same
            cut.elements.value.should(
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
            every { roomServiceMock.getAll() } returns roomList
            every { roomServiceMock.getById(roomId1) } returns MutableStateFlow(room1)
            every { roomServiceMock.getById(roomId2) } returns MutableStateFlow(room2)
            every { roomServiceMock.getById(roomId3) } returns MutableStateFlow(room3)

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.searchTerm.value = "2"
            testCoroutineScheduler.advanceTimeBy(500) // debounce
            testCoroutineScheduler.runCurrent()
            cut.elements.value.should(
                containRoomListElementViewModelsFor(listOf(roomId2, roomId3))
            )

            roomList.value = mapOf(
                roomId1 to MutableStateFlow(room1),
                roomId2 to MutableStateFlow(room2),
            )
            testCoroutineScheduler.advanceUntilIdle() // no debounce, since search term stays the same
            cut.elements.value.should(containRoomListElementViewModelsFor(listOf(roomId2)))

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("remove a room from the search result when its name changes and it no longer fits the search term") {
            val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
            val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
            val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
            val room3NameFlow = MutableStateFlow("room2-other")
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

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.searchTerm.value = "2"
            testCoroutineScheduler.advanceTimeBy(500) // debounce
            testCoroutineScheduler.runCurrent()

            room3NameFlow.value = "completely different"
            testCoroutineScheduler.advanceUntilIdle() // no debounce, since search term stays the same
            cut.elements.value.should(containRoomListElementViewModelsFor(listOf(roomId2)))

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("add a newly added room to the search result when it fits the search term") {
            val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
            val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
            val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
            val room3NameFlow = MutableStateFlow("room2-other")
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
                roomNameMock.getRoomName(eq(room3), eq(matrixClientMock), any())
            } returns room3NameFlow
            roomName3Mocker returns room3NameFlow

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.searchTerm.value = "1"
            testCoroutineScheduler.advanceTimeBy(500) // debounce
            testCoroutineScheduler.runCurrent()
            cut.elements.value.should(containRoomListElementViewModelsFor(listOf(roomId1)))

            room3NameFlow.value = "I am number 1"
            testCoroutineScheduler.advanceUntilIdle() // no debounce, since search term stays the same
            cut.elements.value.should(
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

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.elements.value shouldHaveSize 3

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("consider all spaces") {
            val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
            val space1 = Room(spaceId1, createEventContent = spaceCreateEventContent)
            val space2 = Room(spaceId2, createEventContent = spaceCreateEventContent)
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
            every { roomServiceMock.getState(spaceId1, CreateEventContent::class, "") } returns
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
            every { roomServiceMock.getState(spaceId2, CreateEventContent::class, "") } returns
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
            every { roomServiceMock.getState(spaceId1, CreateEventContent::class, "") } returns
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
            every { roomServiceMock.getState(spaceId2, CreateEventContent::class, "") } returns
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
                roomServiceMock.getAllState(eq(spaceId2), ChildEventContent::class)
            } returns
                    flowOf(
                        mapOf(
                            roomId2.full to flowOf(spaceChildEvent(spaceId2, roomId2)),
                            roomId3.full to flowOf(spaceChildEvent(spaceId2, roomId3)),
                        )
                    )

            every { userServiceMock.getAll(spaceId1) } returns MutableStateFlow(emptyMap())
            every { userServiceMock.getAll(spaceId2) } returns MutableStateFlow(emptyMap())

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.activeSpace.value shouldBe null
            cut.elements.value shouldHaveSize 4

            cut.activeSpace.value = spaceId1
            cut.elements.first { it.isEmpty() }

            cut.activeSpace.value = spaceId2
            cut.elements.first { it.size == 2 }

            cut.activeSpace.value = null
            cut.elements.first { it.size == 4 }

            cut.activeSpace.value = spaceId2
            cut.elements.first { it.size == 2 }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("also show direct rooms with people that are members of the selected space") {
            val room1 = Room(roomId1, createEventContent = roomCreateEventContent, isDirect = true)
            val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
            val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
            val room4 = Room(roomId4, createEventContent = roomCreateEventContent)
            val space2 = Room(spaceId2, createEventContent = spaceCreateEventContent)
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
            every { roomServiceMock.getState(spaceId2, CreateEventContent::class, "") } returns
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
                roomServiceMock.getAllState(eq(spaceId2), ChildEventContent::class)
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

            val cut = roomListViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()
            cut.activeSpace.value = space2.roomId
            cut.elements.first {
                println("... $it")
                it.size == 3
            }
            cut.elements.value[0].roomId shouldBe roomId1

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("only show rooms, spaces and direct chats of selected account") {
            val roomId21 = RoomId("room21", "localhost") // direct room
            val roomId22 = RoomId("room22", "localhost") // group
            val roomId23 = RoomId("room23", "localhost") // group
            val spaceId21 = RoomId("space21", "localhost") // space with room23
            every { matrixClientMock2.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock2 }
                        single { userServiceMock2 }
                    }
                )
            }.koin
            every { matrixClientMock2.userId } returns user2
            every { matrixClientMock2.api } returns matrixClientServerApiClientMock
            every { matrixClientMock2.syncState } returns MutableStateFlow(SyncState.RUNNING)

            every { userServiceMock2.getById(eq(roomId21), eq(user1)) } returns
                    MutableStateFlow(roomUser(roomId21, user1))
            every { userServiceMock2.getById(eq(roomId22), eq(user1)) } returns
                    MutableStateFlow(roomUser(roomId22, user1))
            every { userServiceMock2.getById(eq(roomId21), eq(user2)) } returns user2Flow
            every { userServiceMock2.getById(any(), eq(user2)) } returns
                    MutableStateFlow(roomUser(roomId22, user2))
            every { userServiceMock2.getById(eq(roomId21), eq(user3)) } returns
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
                roomServiceMock2.getState(
                    isNot(listOf(spaceId2, spaceId21)),
                    CreateEventContent::class,
                    any()
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
                roomServiceMock2.getState(
                    any(),
                    JoinRulesEventContent::class,
                    any()
                )
            } returns flowOf(
                StateEvent(
                    content = JoinRulesEventContent(JoinRulesEventContent.JoinRule.Invite),
                    id = EventId(""),
                    sender = user1,
                    roomId = roomId1,
                    originTimestamp = 0L,
                    stateKey = ""
                )
            )

            every {
                roomNameMock.getRoomName(any<Room>(), any(), any())
            } returns flowOf("room")
            every {
                roomNameMock.getRoomName(any<RoomId>(), any(), any())
            } returns flowOf("room")

            every { userServiceMock2.getAccountData(DirectEventContent::class) } returns
                    MutableStateFlow(
                        DirectEventContent(
                            mappings = mapOf(
                                user2 to setOf(roomId21),
                            )
                        )
                    )

            val room1 = Room(roomId1, createEventContent = roomCreateEventContent, isDirect = true)
            val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
            val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
            val room4 = Room(roomId4, createEventContent = roomCreateEventContent)
            val room21 = Room(roomId21, createEventContent = roomCreateEventContent, isDirect = true)
            val room22 = Room(roomId22, createEventContent = roomCreateEventContent)
            val room23 = Room(roomId23, createEventContent = roomCreateEventContent)
            val space2 = Room(spaceId2, createEventContent = spaceCreateEventContent)
            val space21 = Room(spaceId21, createEventContent = spaceCreateEventContent)
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

            every { roomServiceMock.getState(spaceId2, CreateEventContent::class, "") } returns
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
            every { roomServiceMock2.getState(spaceId21, CreateEventContent::class, "") } returns
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
                roomServiceMock.getAllState(eq(spaceId2), ChildEventContent::class)
            } returns
                    flowOf(
                        mapOf(
                            roomId2.full to flowOf(spaceChildEvent(spaceId2, roomId2)),
                            roomId3.full to flowOf(spaceChildEvent(spaceId2, roomId3)),
                        )
                    )
            every {
                roomServiceMock2.getAllState(eq(spaceId2), ChildEventContent::class)
            } returns flowOf(mapOf())
            every {
                roomServiceMock2.getAllState(eq(spaceId21), ChildEventContent::class)
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

            val cut = roomListViewModel(
                coroutineContext,
                mapOf(
                    user1 to matrixClientMock,
                    user2 to matrixClientMock2,
                )
            )
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            // all rooms, spaces, etc. are visible
            cut.elements.first {
                println("(1) ... ${it.map { it.roomId }}")
                it.size == 7
            }
            cut.activeSpace.value = spaceId2
            cut.elements.first {
                println("spaces ... ${it.map { it.roomId }}")
                it.size == 4 // includes direct room (room1 for test, room21 for test2)
            }
            cut.activeSpace.value = null

            cut.accountViewModel.selectActiveAccount(user2)
            testCoroutineScheduler.advanceUntilIdle()
            // only rooms, spaces, etc. of account 'test2' are visible
            cut.elements.first {
                println("(2) ... ${it.map { it.roomId }}")
                it.size == 3
            }
            cut.spaces.first {
                println("spaces... ${it.map { it.roomId }}")
                it.size == 1
            }
            cut.activeSpace.value = spaceId21
            testCoroutineScheduler.advanceUntilIdle()
            cut.elements.first {
                println("(3) ... ${it.map { it.roomId }}")
                it.size == 1 // only room23 is in space21
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }


        should("trigger account verification") {
            val verificationTriggers = MutableStateFlow<List<UserId>>(listOf())
            val trigger = SelfVerificationTriggerImpl()
            val cut = roomListViewModel(
                coroutineContext,
                selfVerificationTrigger = trigger,
            )
            val subscriberJob = subscribe(cut)
            launch {
                trigger.onInvoke.collect {
                    verificationTriggers.value += it
                }
            }
            testCoroutineScheduler.advanceUntilIdle()
            verificationTriggers.first().size shouldBe 0

            cut.verifyAccount(UserId("Timmy"))
            testCoroutineScheduler.advanceUntilIdle()
            eventually(1.seconds) {
                verificationTriggers.first().size shouldBe 1
                verificationTriggers.first()[0].localpart shouldBe "Timmy"
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("get unverified accounts") {
            val cut = roomListViewModel(
                coroutineContext,
                mapOf(
                    user1 to matrixClientMock,
                    user2 to matrixClientMock2,
                )
            )
            val subscriberJob = subscribe(cut)
            val syncState = MutableStateFlow(SyncState.STOPPED)

            every { matrixClientMock2.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock2 }
                        single { userServiceMock2 }
                        single { keyServiceMock2 }
                    }
                )
            }.koin

            every { keyServiceMock.getTrustLevel(any<UserId>(), any()) } returns
                    flowOf(DeviceTrustLevel.CrossSigned(true))
            every { keyServiceMock2.getTrustLevel(any<UserId>(), any()) } returns
                    flowOf(DeviceTrustLevel.CrossSigned(false))
            every { matrixClientMock.userId } returns user1
            every { matrixClientMock2.userId } returns user2
            every { matrixClientMock.deviceId } returns "device1"
            every { matrixClientMock2.deviceId } returns "device2"
            every { matrixClientMock.syncState } returns syncState
            every { matrixClientMock2.syncState } returns syncState
            every { roomServiceMock.getAll() } returns MutableStateFlow(mapOf())
            every { roomServiceMock2.getAll() } returns MutableStateFlow(mapOf())
            every { userServiceMock2.getAccountData(DirectEventContent::class) } returns
                    MutableStateFlow(DirectEventContent(mappings = mapOf()))

            launch { cut.unverifiedAccounts.collect() }
            testCoroutineScheduler.advanceUntilIdle()
            cut.unverifiedAccounts.first() shouldBe listOf(
                user2,
            )

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }

    private fun CoroutineScope.subscribe(cut: RoomListViewModel) = launch {
        launch { cut.selectedRoomId.collect() }
        launch { cut.error.collect() }
        launch { cut.errorType.collect() }
        launch { cut.elements.collect() }
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
        selfVerificationTrigger: SelfVerificationTrigger? = null,
    ): RoomListViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        val koin = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(matrixClients) + module {
                    single { roomNameMock }
                    single { profileManagerMock }
                    if (selfVerificationTrigger != null) single { selfVerificationTrigger }
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
                                    override val isSingleAccount: StateFlow<Boolean> = MutableStateFlow(false)
                                    override val accounts: StateFlow<List<AccountInfo>> =
                                        MutableStateFlow(listOf())

                                    override fun selectActiveAccount(userId: UserId?) {
                                        GlobalScope.launch {
                                            get<MatrixMessengerSettingsHolder>().update<MatrixMessengerSettingsBase>() {
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
            onCreateNewRoom = mock(),
            onUserSettingsSelected = mock(),
            onOpenAppInfo = mock(),
            onOpenAccountsOverview = mock(),
            onSendLogs = mock(),
            onAccountSelected = onAccountSelected
        )
    }

    private fun containRoomListElementViewModelsFor(roomIds: List<RoomId>) =
        KoMatcher<List<RoomListElement>> { list ->
            MatcherResult(
                roomIds.all { roomId ->
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
            content = ChildEventContent(via = setOf()),
            id = EventId(""),
            sender = user1,
            roomId = spaceId,
            originTimestamp = 0L,
            stateKey = containedId.full,
        )
}
