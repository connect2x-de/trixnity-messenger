package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.isNot
import de.connect2x.trixnity.messenger.isRoomOf
import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.viewmodel.AccountInfo
import de.connect2x.trixnity.messenger.viewmodel.RootViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verify
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.KeyService
import net.folivo.trixnity.client.notification.NotificationService
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
import net.folivo.trixnity.core.model.events.m.MarkedUnreadEventContent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent.RoomType
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.space.ChildEventContent
import net.folivo.trixnity.crypto.key.DeviceTrustLevel
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import io.kotest.matchers.Matcher as KoMatcher

class RoomListViewModelTest {

    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry()

    private val roomId1 = RoomId("!room1")
    private val roomId2 = RoomId("!room2")
    private val roomId3 = RoomId("!room3")
    private val roomId4 = RoomId("!room4")
    private val roomId5 = RoomId("!room5")
    private val spaceId1 = RoomId("!space1")
    private val spaceId2 = RoomId("!space2")

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
    val notificationService = mock<NotificationService>()

    private val onRoomSelectedMock = mock<Function2<UserId, RoomId, Unit>>()
    private val onAccountSelected = mock<Function0<Unit>>()

    var syncStateMocker: BlockingAnsweringScope<StateFlow<SyncState>>
    var roomName3Mocker: BlockingAnsweringScope<Flow<String>>

    private val roomCreateEventContent = CreateEventContent(type = RoomType.Room)
    private val spaceCreateEventContent = CreateEventContent(type = RoomType.Space)

    init {
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
            notificationService,
        )

        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                    single { keyServiceMock }
                    single { notificationService }
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

        every { userServiceMock.getPresence(any()) } returns flowOf(null)

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
        every { roomServiceMock.usersTyping } returns MutableStateFlow(mapOf())
        every { roomServiceMock.getAccountData(any(), eq(MarkedUnreadEventContent::class), any()) } returns flowOf(null)
        every {
            roomServiceMock2.getAccountData(
                any(),
                eq(MarkedUnreadEventContent::class),
                any()
            )
        } returns flowOf(null)

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
        every { notificationService.getCount(any()) } returns flowOf(0)
    }

    @Test
    fun `sort rooms by last received message`() = runTest {
        every {
            roomServiceMock.getState(roomId5, CreateEventContent::class, "")
        } returns
                flowOf(
                    StateEvent(
                        CreateEventContent(),
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

        val cut = roomListViewModel()
        subscribe(cut)

        val list = cut.elements.onEach { println(it) }.first { it.size == 5 }
        list[0].roomId shouldBe roomId2
        list[1].roomId shouldBe roomId3
        list[2].roomId shouldBe roomId5
        list[3].roomId shouldBe roomId1
        list[4].roomId shouldBe roomId4
    }

    @Test
    fun `set 'initialSyncFinished' to 'true' when the initial sync with the matrix server is completed`() = runTest {
        every { roomServiceMock.getAll() } returns MutableStateFlow(emptyMap())
        val syncState = MutableStateFlow(SyncState.STARTED)
        syncStateMocker returns syncState

        val cut = roomListViewModel()
        subscribe(cut)

        syncState.value = SyncState.INITIAL_SYNC
        delay(100)
        cut.initialSyncFinished.value shouldBe false

        syncState.value = SyncState.RUNNING
        delay(100)
        cut.initialSyncFinished.value shouldBe true
    }

    @Test
    fun `leave 'initialSyncFinished as 'false' when the client reconnects to the matrix server`() = runTest {
        every { roomServiceMock.getAll() } returns MutableStateFlow(emptyMap())
        val syncState = MutableStateFlow(SyncState.RUNNING)
        syncStateMocker returns syncState

        val cut = roomListViewModel()
        subscribe(cut)

        delay(10)
        cut.initialSyncFinished.value shouldBe true

        syncState.value = SyncState.TIMEOUT
        delay(10)
        cut.initialSyncFinished.value shouldBe false

        syncState.value = SyncState.RUNNING
        delay(10)
        cut.initialSyncFinished.value shouldBe false
    }

    @Test
    fun `open a normal room on selection`() = runTest {
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

        val cut = roomListViewModel()
        subscribe(cut)

        cut.selectRoom(roomId1)
        delay(10)

        joinedRoomWasCalled shouldBe false
        verify { onRoomSelectedMock.invoke(any(), eq(roomId1)) }
    }

    @Test
    fun `do nothing when selecting invited room`() = runTest {
        val room = Room(roomId1, membership = Membership.INVITE)
        every { roomServiceMock.getById(roomId1) } returns flowOf(room)
        every { roomServiceMock.getAll() } returns MutableStateFlow(
            mapOf(
                roomId1 to MutableStateFlow(Room(roomId1))
            )
        )
        val syncState = MutableStateFlow(SyncState.ERROR)
        syncStateMocker returns syncState

        val cut = roomListViewModel()
        subscribe(cut)

        cut.selectRoom(roomId1)
        delay(10)

        cut.error.value shouldBe null
        cut.selectedRoomId.value shouldBe RoomId("!roomId") // default when initialized in test case
    }

    @Test
    fun `not show search initially`() = runTest {
        every { roomServiceMock.getAll() } returns MutableStateFlow(mapOf())
        val cut = roomListViewModel()
        subscribe(cut)

        cut.showSearch.value shouldBe false
    }

    @Test
    fun `yield all rooms as search result when search term which is blank`() = runTest {
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
        val cut = roomListViewModel()
        subscribe(cut)

        cut.searchTerm.update("")
        delay(10)
        cut.elements.value shouldHaveSize 2

        cut.searchTerm.update("  ")
        delay(10)
        cut.elements.value shouldHaveSize 2
    }

    @Test
    fun `contain search term in all search results`() = runTest {
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

        val cut = roomListViewModel()
        subscribe(cut)

        cut.searchTerm.update("2")
        delay(301) // 300 ms debounce

        cut.elements.value shouldHaveSize 2
        cut.elements.value.should(
            containRoomListElementViewModelsFor(listOf(roomId2, roomId3))
        )
    }

    @Test
    fun `change search results when the search term changes`() = runTest {
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

        val cut = roomListViewModel()
        subscribe(cut)

        cut.searchTerm.update("2")
        delay(301) // 300 ms debounce
        cut.searchTerm.update("1")
        delay(301) // 300 ms debounce

        cut.elements.value.should(containRoomListElementViewModelsFor(listOf(roomId1)))
    }

    @Test
    fun `show a newly added room that fits the ongoing search term`() = runTest {
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

        val cut = roomListViewModel()
        subscribe(cut)

        cut.searchTerm.update("2")
        delay(301) // 300 ms debounce
        cut.elements.value.should(containRoomListElementViewModelsFor(listOf(roomId2)))

        roomList.value = mapOf(
            roomId1 to MutableStateFlow(room1),
            roomId2 to MutableStateFlow(room2),
            roomId3 to MutableStateFlow(room3),
        )
        delay(10) // no debounce, since search term stays the same
        cut.elements.value.should(
            containRoomListElementViewModelsFor(listOf(roomId2, roomId3))
        )
    }

    @Test
    fun `remove room from search result when it is removed`() = runTest {
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

        val cut = roomListViewModel()
        subscribe(cut)

        cut.searchTerm.update("2")
        delay(301) // 300 ms debounce
        cut.elements.value.should(
            containRoomListElementViewModelsFor(listOf(roomId2, roomId3))
        )

        roomList.value = mapOf(
            roomId1 to MutableStateFlow(room1),
            roomId2 to MutableStateFlow(room2),
        )
        delay(10000) // no debounce, since search term stays the same
        cut.elements.value.should(containRoomListElementViewModelsFor(listOf(roomId2)))
    }

    @Test
    fun `remove a room from the search result when its name changes and it no longer fits the search term`() = runTest {
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

        val cut = roomListViewModel()
        subscribe(cut)

        cut.searchTerm.update("2")
        delay(301) // 300 ms debounce

        room3NameFlow.value = "completely different"
        delay(1000) // no debounce, since search term stays the same
        cut.elements.value.should(containRoomListElementViewModelsFor(listOf(roomId2)))
    }

    @Test
    fun `add a newly added room to the search result when it fits the search term`() = runTest {
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

        val cut = roomListViewModel()
        subscribe(cut)

        cut.searchTerm.update("1")
        delay(301) // 300 ms debounce
        cut.elements.value.should(containRoomListElementViewModelsFor(listOf(roomId1)))

        room3NameFlow.value = "I am number 1"
        delay(10.seconds) // no debounce, since search term stays the same
        cut.elements.value.should(
            containRoomListElementViewModelsFor(listOf(roomId1, roomId3))
        )
    }

    @Test
    fun `show rooms without whitespace when adding a whitespace to the end of search term`() = runTest {
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

        val cut = roomListViewModel()
        subscribe(cut)

        cut.searchTerm.update("2")
        delay(301) // 300 ms debounce
        cut.searchTerm.update("2 ")
        delay(301) // 300 ms debounce

        cut.elements.value.should(containRoomListElementViewModelsFor(listOf(roomId2)))
    }

    @Test
    fun `not show spaces in room list`() = runTest {
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

        val cut = roomListViewModel()
        subscribe(cut)
        delay(10)

        cut.elements.value shouldHaveSize 3
    }

    @Test
    fun `only show rooms and direct chats of selected account`() = runTest {
        val roomId21 = RoomId("!room21") // direct room
        val roomId22 = RoomId("!room22") // group
        val roomId23 = RoomId("!room23") // group
        val spaceId21 = RoomId("!space21") // space with room23
        every { matrixClientMock2.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock2 }
                    single { userServiceMock2 }
                    single { notificationService }
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
        every { userServiceMock2.getPresence(any()) } returns flowOf(null)

        every {
            roomServiceMock2.getState(
                isNot(listOf(spaceId2, spaceId21)),
                CreateEventContent::class,
                any()
            )
        } returns flowOf(
            StateEvent(
                content = CreateEventContent(),
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

        every { roomServiceMock2.usersTyping } returns MutableStateFlow(mapOf())

        val cut = roomListViewModel(
            mapOf(
                user1 to matrixClientMock,
                user2 to matrixClientMock2,
            )
        )
        subscribe(cut)

        // all rooms, spaces, etc. are visible
        cut.elements.first {
            println("(1) ... ${it.map { it.roomId }}")
            it.size == 7
        }

        cut.accountViewModel.selectActiveAccount(user2)
        // only rooms, spaces, etc. of account 'test2' are visible
        cut.elements.first {
            println("(2) ... ${it.map { it.roomId }}")
            it.size == 3
        }
    }

    @Test
    fun `trigger account verification`() = runTest {
        val verificationTriggers = MutableStateFlow<List<UserId>>(listOf())
        val cut = roomListViewModel(onVerificationStarted = { userId -> verificationTriggers.value += userId })
        subscribe(cut)

        verificationTriggers.first().size shouldBe 0

        cut.verifyAccount(UserId("Timmy"))

        eventually(1.seconds) {
            verificationTriggers.first().size shouldBe 1
            verificationTriggers.first()[0].localpart shouldBe "Timmy"
        }
    }

    @Test
    fun `get unverified accounts`() = runTest {
        val cut = roomListViewModel(
            mapOf(
                user1 to matrixClientMock,
                user2 to matrixClientMock2,
            )
        )
        subscribe(cut)
        val syncState = MutableStateFlow(SyncState.STOPPED)

        every { matrixClientMock2.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock2 }
                    single { userServiceMock2 }
                    single { keyServiceMock2 }
                    single { notificationService }
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

        backgroundScope.launch { cut.unverifiedAccounts.collect() }
        delay(10)
        cut.unverifiedAccounts.first() shouldBe listOf(
            user2,
        )
    }

    @Test
    fun `update search results empty information when no rooms exist`() = runTest {
        val cut = roomListViewModel(matrixClients = mapOf(user1 to matrixClientMock))
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                    single { keyServiceMock }
                    single { notificationService }
                }
            )
        }.koin
        every { roomServiceMock.getAll() } returns flowOf(mapOf())
        subscribe(cut)
        delay(10)
        cut.elements.value.size shouldBe 0
        cut.searchResultsEmpty.value shouldBe false

        cut.showSearch.value = true
        cut.searchTerm.update("KeinErgebnis")
        delay(600)
        cut.searchResultsEmpty.value shouldBe false
    }

    @Test
    fun `update search results empty information when rooms were found`() = runTest {
        val cut = roomListViewModel(matrixClients = mapOf(user1 to matrixClientMock))
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                    single { keyServiceMock }
                    single { notificationService }
                }
            )
        }.koin
        every { roomServiceMock.getAll() } returns flowOf(mapOf(roomId1 to flowOf(Room(roomId1))))
        every { roomServiceMock.getById(roomId1) } returns flowOf()
        subscribe(cut)
        delay(10)
        cut.elements.value.size shouldBe 1
        cut.searchResultsEmpty.value shouldBe false

        cut.showSearch.value = true
        cut.searchTerm.update("room1")
        delay(600)
        cut.searchResultsEmpty.value shouldBe false
    }

    @Test
    fun `update search results empty information when rooms exist but none fulfill the search criteria`() = runTest {
        val cut = roomListViewModel(matrixClients = mapOf(user1 to matrixClientMock))
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                    single { keyServiceMock }
                    single { notificationService }
                }
            )
        }.koin
        every { roomServiceMock.getAll() } returns flowOf(mapOf(roomId1 to flowOf(Room(roomId1))))
        every { roomServiceMock.getById(roomId1) } returns flowOf(Room(roomId1))
        subscribe(cut)
        delay(10)
        cut.elements.value.size shouldBe 1
        cut.searchResultsEmpty.value shouldBe false

        cut.showSearch.value = true
        cut.searchTerm.update("KeinErgebnis")
        delay(600)
        cut.searchResultsEmpty.value shouldBe true
    }

    private fun TestScope.subscribe(cut: RoomListViewModel) = backgroundScope.launch {
        launch { cut.selectedRoomId.collect(::println) }
        launch { cut.error.collect(::println) }
        launch { cut.errorType.collect(::println) }
        launch { cut.elements.collect(::println) }
        launch { cut.initialSyncFinished.collect(::println) }
        launch { cut.showSearch.collect(::println) }
        launch { cut.searchTerm.collect(::println) }
        launch { cut.searchResultsEmpty.collect(::println) }
    }

    private fun TestScope.roomListViewModel(
        matrixClients: Map<UserId, MatrixClient> = mapOf(user1 to matrixClientMock),
        onVerificationStarted: ((UserId) -> Unit)? = null
    ): RoomListViewModelImpl {
        val koin = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(matrixClients) + module {
                    single { roomNameMock }
                    single { profileManagerMock }
                    single<AccountViewModelFactory> {
                        object : AccountViewModelFactory {
                            override fun create(
                                viewModelContext: ViewModelContext,
                                onAccountSelected: (UserId?) -> Unit,
                                onUserSettingsSelected: () -> Unit,
                                onShowAppInfo: () -> Unit,
                                onShowProfile: () -> Unit
                            ): AccountViewModel {
                                return object : AccountViewModel {
                                    override val activeAccount: StateFlow<UserId?> = MutableStateFlow(null)
                                    override val isSingleAccount: StateFlow<Boolean> = MutableStateFlow(false)
                                    override val accounts: StateFlow<List<AccountInfo>> =
                                        MutableStateFlow(listOf())

                                    override fun selectActiveAccount(userId: UserId?) {
                                        backgroundScope.launch {
                                            get<MatrixMessengerSettingsHolder>().update<MatrixMessengerSettingsBase> {
                                                it.copy(selectedAccount = userId)
                                            }
                                        }
                                        onAccountSelected(userId) // needed to influence RoomListViewModel
                                    }

                                    override fun openUserSettings() {}
                                    override fun openUserProfile() {}
                                    override fun openAppInfo() {}
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
                coroutineContext = backgroundScope.coroutineContext
            ),
            selectedRoomId = MutableStateFlow(RoomId("!roomId")),
            onRoomSelected = onRoomSelectedMock,
            onCreateNewRoom = mock(),
            onUserSettingsSelected = mock(),
            onUserProfileSelected = mock(),
            onOpenAppInfo = mock(),
            onOpenAccountsOverview = mock(),
            onSendLogs = mock(),
            onAccountSelected = onAccountSelected,
            onStartVerification = onVerificationStarted ?: mock(),
            onCloseRoom = mock()
        )
    }

    private fun containRoomListElementViewModelsFor(roomIds: List<RoomId>) =
        KoMatcher<List<RoomListElementViewModel>> { list ->
            MatcherResult(
                roomIds.all { roomId ->
                    list.any { element -> element.roomId == roomId }
                },
                {
                    "RoomListElementViewModel with ids [${
                        roomIds.filterNot { roomId -> list.any { element -> element.roomId == roomId } }
                            .joinToString { it.full }
                    }] not found"
                },
                {
                    "RoomListElementViewModel with ids [${
                        roomIds.filterNot { roomId -> list.any { element -> element.roomId == roomId } }
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
