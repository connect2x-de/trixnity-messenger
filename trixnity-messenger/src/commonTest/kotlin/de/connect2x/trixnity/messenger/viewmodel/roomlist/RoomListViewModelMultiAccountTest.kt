package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import de.connect2x.trixnity.messenger.continually
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.isNot
import de.connect2x.trixnity.messenger.isRoomOf
import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.AccountInfo
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.RootViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
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
import net.folivo.trixnity.client.notification.NotificationService
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.UserPresence
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
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent.RoomType
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.space.ChildEventContent
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import io.kotest.matchers.Matcher as KoMatcher

class RoomListViewModelMultiAccountTest {

    private var lifecycleRegistry = LifecycleRegistry()

    private val roomId1 = RoomId("!room1")
    private val roomId2 = RoomId("!room2")
    private val roomId3 = RoomId("!room3")
    private val roomId4 = RoomId("!room4")
    private val roomId5 = RoomId("!room5")
    private val spaceId1 = RoomId("!space1")
    private val spaceId2 = RoomId("!space2")

    private val me1 = UserId("test1", "server")
    private val me2 = UserId("test2", "server")
    private val me3 = UserId("test3", "server")
    private val user2 = UserId("user2", "server")
    private val user3 = UserId("user3", "server")

    private val user2Flow = MutableStateFlow(roomUser(roomId1, user2))

    val matrixClientMock1 = mock<MatrixClient>()

    val matrixClientMock2 = mock<MatrixClient>()

    val matrixClientMock3 = mock<MatrixClient>()

    val userServiceMock1 = mock<UserService>()

    val userServiceMock2 = mock<UserService>()

    val userServiceMock3 = mock<UserService>()

    val roomServiceMock1 = mock<RoomService>()

    val roomServiceMock2 = mock<RoomService>()

    val roomServiceMock3 = mock<RoomService>()
    val notificationService = mock<NotificationService>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    private val roomNameMock = mock<RoomName>()

    private val profileManagerMock = mock<ProfileManager>()

    private val onRoomSelectedMock = mock<Function2<UserId, RoomId, Unit>>()

    private var syncStateMocker1: BlockingAnsweringScope<StateFlow<SyncState>>
    private var syncStateMocker2: BlockingAnsweringScope<StateFlow<SyncState>>
    private var syncStateMocker3: BlockingAnsweringScope<StateFlow<SyncState>>
    private var roomName3Mocker: BlockingAnsweringScope<Flow<String>>

    private lateinit var di: Koin
    private lateinit var matrixClients: MutableStateFlow<Map<UserId, MatrixClient>>

    private val roomCreateEventContent = CreateEventContent(type = RoomType.Room)
    private val spaceCreateEventContent = CreateEventContent(type = RoomType.Space)

    init {
        lifecycleRegistry.resume()

        resetMocks(
            matrixClientMock1,
            matrixClientMock2,
            matrixClientMock3,
            userServiceMock1,
            userServiceMock2,
            userServiceMock3,
            roomServiceMock1,
            roomServiceMock2,
            roomServiceMock3,
            matrixClientServerApiClientMock,
            roomsApiClientMock,
            roomNameMock,
            profileManagerMock,
            onRoomSelectedMock,
            notificationService,
        )

        // MatrixClient1: room1, room2, space1
        // MatrixClient2: room3, room4, space2
        // MatrixClient3: room5
        every { matrixClientMock1.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock1 }
                    single { userServiceMock1 }
                    single { notificationService }
                })
        }.koin
        every { matrixClientMock2.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock2 }
                    single { userServiceMock2 }
                    single { notificationService }
                })
        }.koin
        every { matrixClientMock3.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock3 }
                    single { userServiceMock3 }
                    single { notificationService }
                })
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
        every { matrixClientMock1.api } returns matrixClientServerApiClientMock
        every { matrixClientMock2.api } returns matrixClientServerApiClientMock
        every { matrixClientMock3.api } returns matrixClientServerApiClientMock
        every { matrixClientServerApiClientMock.room } returns roomsApiClientMock

        every { userServiceMock1.getById(eq(roomId1), eq(me1)) } returns MutableStateFlow(roomUser(roomId1, me1))
        every { userServiceMock1.getById(eq(roomId2), eq(me1)) } returns MutableStateFlow(roomUser(roomId2, me1))
        every { userServiceMock1.getById(eq(roomId1), eq(user2)) } returns user2Flow
        every { userServiceMock1.getById(eq(roomId1), eq(user3)) } returns MutableStateFlow(roomUser(roomId1, user3))

        every { userServiceMock1.getById(any(), eq(user2)) } returns MutableStateFlow(roomUser(roomId2, user2))
        every { userServiceMock2.getById(any(), eq(user2)) } returns MutableStateFlow(roomUser(roomId2, user2))
        every { userServiceMock3.getById(any(), eq(user2)) } returns MutableStateFlow(roomUser(roomId2, user2))

        every { userServiceMock1.getAll(eq(roomId1)) } returns MutableStateFlow(
            mapOf(
                me1 to flowOf(roomUser(roomId1, me1)), user2 to flowOf(roomUser(roomId1, user2))
            )
        )
        every { userServiceMock1.getAll(eq(roomId2)) } returns MutableStateFlow(
            mapOf(
                me1 to flowOf(roomUser(roomId2, me1)), user2 to flowOf(roomUser(roomId2, user2))
            )
        )
        every { userServiceMock2.getAll(roomId3) } returns MutableStateFlow(
            mapOf(
                me2 to flowOf(roomUser(roomId3, me2)), user3 to flowOf(roomUser(roomId3, user3))
            )
        )
        every { userServiceMock2.getAll(eq(roomId4)) } returns MutableStateFlow(
            mapOf(
                me2 to flowOf(
                    roomUser(
                        roomId4,
                        me2
                    )
                )
            )
        )
        every { userServiceMock3.getAll(eq(roomId5)) } returns MutableStateFlow(emptyMap())

        every { userServiceMock1.getPresence(any()) } returns flowOf(
            UserPresence(Presence.OFFLINE, Clock.System.now())
        )
        every { userServiceMock2.getPresence(any()) } returns flowOf(
            UserPresence(Presence.OFFLINE, Clock.System.now())
        )
        every { userServiceMock3.getPresence(any()) } returns flowOf(
            UserPresence(Presence.OFFLINE, Clock.System.now())
        )

        every {
            roomServiceMock1.getState(
                isNot(listOf(roomId5, spaceId1, spaceId2)), CreateEventContent::class, any()
            )
        } returns flowOf(
            StateEvent(
                content = CreateEventContent(),
                id = EventId(""),
                sender = me1,
                roomId = roomId1,
                originTimestamp = 0L,
                stateKey = ""
            )
        )
        every {
            roomServiceMock2.getState(
                isNot(listOf(roomId5, spaceId1, spaceId2)), CreateEventContent::class, any()
            )
        } returns flowOf(
            StateEvent(
                content = CreateEventContent(),
                id = EventId(""),
                sender = me2,
                roomId = roomId1,
                originTimestamp = 0L,
                stateKey = ""
            )
        )
        every {
            roomServiceMock3.getState(
                isNot(listOf(roomId5, spaceId1, spaceId2)), CreateEventContent::class, any()
            )
        } returns flowOf(
            StateEvent(
                content = CreateEventContent(),
                id = EventId(""),
                sender = me3,
                roomId = roomId1,
                originTimestamp = 0L,
                stateKey = ""
            )
        )
        every {
            roomServiceMock1.getAccountData(
                any(),
                eq(MarkedUnreadEventContent::class),
                any()
            )
        } returns flowOf(null)
        every {
            roomServiceMock2.getAccountData(
                any(),
                eq(MarkedUnreadEventContent::class),
                any()
            )
        } returns flowOf(null)
        every {
            roomServiceMock3.getAccountData(
                any(),
                eq(MarkedUnreadEventContent::class),
                any()
            )
        } returns flowOf(null)

        every { onRoomSelectedMock.invoke(any(), any()) } returns Unit

        every {
            roomNameMock.getRoomName(isRoomOf(roomId1), eq(matrixClientMock1), any())
        } returns flowOf("room1")
        every {
            roomNameMock.getRoomName(eq(roomId1), eq(matrixClientMock1), any())
        } returns flowOf("room1")
        every {
            roomNameMock.getRoomName(isRoomOf(roomId2), eq(matrixClientMock1), any())
        } returns flowOf("room2")
        every {
            roomNameMock.getRoomName(eq(roomId2), eq(matrixClientMock1), any())
        } returns flowOf("room2")
        roomName3Mocker = every { roomNameMock.getRoomName(isRoomOf(roomId3), eq(matrixClientMock2), any()) }
        roomName3Mocker returns flowOf("room3-but-also-room2")
        every {
            roomNameMock.getRoomName(eq(roomId3), eq(matrixClientMock2), any())
        } returns flowOf("room3-but-also-room2")
        every {
            roomNameMock.getRoomName(isRoomOf(roomId4), eq(matrixClientMock2), any())
        } returns flowOf("room4")
        every {
            roomNameMock.getRoomName(eq(roomId4), eq(matrixClientMock2), any())
        } returns flowOf("room4")
        every {
            roomNameMock.getRoomName(isRoomOf(roomId5), eq(matrixClientMock3), any())
        } returns flowOf("room5")
        every {
            roomNameMock.getRoomName(eq(roomId5), eq(matrixClientMock3), any())
        } returns flowOf("room5")
        every {
            roomNameMock.getRoomName(isRoomOf(spaceId1), eq(matrixClientMock1), any())
        } returns flowOf("space and beyond")
        every {
            roomNameMock.getRoomName(isRoomOf(spaceId2), eq(matrixClientMock2), any())
        } returns flowOf("space and beyond and beyonder")
        every { roomServiceMock1.getLastTimelineEvent(any(), any()) } returns flowOf(null)
        every { roomServiceMock2.getLastTimelineEvent(any(), any()) } returns flowOf(null)
        every { roomServiceMock3.getLastTimelineEvent(any(), any()) } returns flowOf(null)

        every { roomServiceMock1.usersTyping } returns MutableStateFlow(mapOf())
        every { roomServiceMock2.usersTyping } returns MutableStateFlow(mapOf())
        every { roomServiceMock3.usersTyping } returns MutableStateFlow(mapOf())

        every {
            roomServiceMock1.getAllState(spaceId1, ChildEventContent::class)
        } returns flowOf(mapOf())

        every { userServiceMock1.getAccountData(DirectEventContent::class) } returns MutableStateFlow(
            DirectEventContent(
                mappings = mapOf(
                    user2 to setOf(roomId1),
                )
            )
        )
        every { userServiceMock2.getAccountData(DirectEventContent::class) } returns MutableStateFlow(
            DirectEventContent(
                mappings = emptyMap()
            )
        )
        every { userServiceMock3.getAccountData(DirectEventContent::class) } returns MutableStateFlow(
            DirectEventContent(
                mappings = emptyMap()
            )
        )
        every {
            roomServiceMock1.getState(
                any(), JoinRulesEventContent::class, any()
            )
        } returns MutableStateFlow(
            StateEvent(
                content = JoinRulesEventContent(
                    joinRule = JoinRulesEventContent.JoinRule.Private
                ),
                EventId("1"),
                user2,
                roomId1,
                0L,
                stateKey = "",
            )
        )
        every {
            roomServiceMock2.getState(
                any(), JoinRulesEventContent::class, any()
            )
        } returns MutableStateFlow(
            StateEvent(
                content = JoinRulesEventContent(
                    joinRule = JoinRulesEventContent.JoinRule.Private
                ),
                EventId("1"),
                user2,
                roomId1,
                0L,
                stateKey = "",
            )
        )
        every {
            roomServiceMock2.getState(
                any(), JoinRulesEventContent::class, any()
            )
        } returns MutableStateFlow(
            StateEvent(
                content = JoinRulesEventContent(
                    joinRule = JoinRulesEventContent.JoinRule.Private
                ),
                EventId("1"),
                user2,
                roomId1,
                0L,
                stateKey = "",
            )
        )
        every {
            roomServiceMock3.getState(
                any(), JoinRulesEventContent::class, any()
            )
        } returns MutableStateFlow(
            StateEvent(
                content = JoinRulesEventContent(
                    joinRule = JoinRulesEventContent.JoinRule.Private
                ),
                EventId("1"),
                user2,
                roomId1,
                0L,
                stateKey = "",
            )
        )

        every { profileManagerMock.profiles } returns MutableStateFlow(emptyMap())
        everySuspend { profileManagerMock.closeProfile() } returns Unit
        every { notificationService.getCount(any()) } returns flowOf(0)
    }

    @Test
    fun `sort rooms by last received message even if the rooms are of different matrix accounts`() = runTest {
        every {
            roomServiceMock3.getState(roomId5, CreateEventContent::class, "")
        } returns flowOf(
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
        val room5 = Room(roomId5, createEventContent = roomCreateEventContent) // with invite
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
    fun `set 'initialSyncFinished' to 'true' when the initial sync with the matrix server is completed for all accounts`() =
        runTest {
            every { roomServiceMock1.getAll() } returns MutableStateFlow(emptyMap())
            every { roomServiceMock2.getAll() } returns MutableStateFlow(emptyMap())
            every { roomServiceMock3.getAll() } returns MutableStateFlow(emptyMap())
            val syncState1 = MutableStateFlow(SyncState.STARTED)
            val syncState2 = MutableStateFlow(SyncState.STARTED)
            val syncState3 = MutableStateFlow(SyncState.STARTED)
            syncStateMocker1 returns syncState1
            syncStateMocker2 returns syncState2
            syncStateMocker3 returns syncState3

            val cut = roomListViewModel()
            subscribe(cut)

            syncState1.value = SyncState.INITIAL_SYNC
            syncState2.value = SyncState.INITIAL_SYNC
            syncState3.value = SyncState.INITIAL_SYNC
            delay(10)
            cut.initialSyncFinished.value shouldBe false

            syncState1.value = SyncState.RUNNING
            delay(10)
            cut.initialSyncFinished.value shouldBe false

            syncState2.value = SyncState.RUNNING
            delay(10)
            cut.initialSyncFinished.value shouldBe false

            syncState3.value = SyncState.RUNNING
            delay(10)
            cut.initialSyncFinished.value shouldBe true
        }

    @Test
    fun `display only users with a sync error`() = runTest {
        every { roomServiceMock1.getAll() } returns MutableStateFlow(emptyMap())
        every { roomServiceMock2.getAll() } returns MutableStateFlow(emptyMap())
        every { roomServiceMock3.getAll() } returns MutableStateFlow(emptyMap())
        val syncState1 = MutableStateFlow(SyncState.STARTED)
        val syncState2 = MutableStateFlow(SyncState.STARTED)
        val syncState3 = MutableStateFlow(SyncState.STARTED)
        syncStateMocker1 returns syncState1
        syncStateMocker2 returns syncState2
        syncStateMocker3 returns syncState3

        val cut = roomListViewModel()
        delay(10)
        subscribe(cut)
        delay(10)

        syncState1.value = SyncState.INITIAL_SYNC
        syncState2.value = SyncState.INITIAL_SYNC
        syncState3.value = SyncState.INITIAL_SYNC

        continually(10.seconds) {
            cut.syncStates.value.failedFor shouldBe setOf()
            cut.syncStates.value.failedForAll shouldBe false
        }

        syncState1.value = SyncState.ERROR

        delay(4.seconds) // 3 seconds debounce
        cut.syncStates.value.failedFor shouldBe setOf(me1)
        cut.syncStates.value.failedForAll shouldBe false

        syncState2.value = SyncState.ERROR

        delay(4.seconds) // 3 seconds debounce
        cut.syncStates.value.failedFor shouldBe setOf(me1, me2)
        cut.syncStates.value.joinFailedToString() shouldBe "${me1.full}, ${me2.full}"
        cut.syncStates.value.failedForAll shouldBe false

        syncState3.value = SyncState.ERROR

        delay(4.seconds) // 3 seconds debounce
        cut.syncStates.value.failedFor shouldBe setOf(me1, me2, me3)
        cut.syncStates.value.operationalFor shouldBe setOf()
        cut.syncStates.value.failedForAll shouldBe true
    }

    @Test
    fun `do nothing when selecting invited room`() = runTest {
        val room = Room(roomId1, createEventContent = roomCreateEventContent, membership = Membership.INVITE)
        every { roomServiceMock1.getById(roomId1) } returns flowOf(room)
        every { roomServiceMock1.getAll() } returns MutableStateFlow(
            mapOf(
                roomId1 to MutableStateFlow(Room(roomId1))
            )
        )
        every { roomServiceMock2.getAll() } returns MutableStateFlow(emptyMap())
        every { roomServiceMock3.getAll() } returns MutableStateFlow(emptyMap())
        val syncState = MutableStateFlow(SyncState.ERROR)
        syncStateMocker1 returns syncState

        val cut = roomListViewModel()
        subscribe(cut)

        cut.selectRoom(roomId1)
        delay(10)

        cut.error.value shouldBe null
        cut.selectedRoomId.value shouldBe RoomId("!roomId") // default when initialized in test case
    }

    @Test
    fun `yield all rooms as search result when search term which is blank`() = runTest {
        val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
        val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
        val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
        val room4 = Room(roomId4, createEventContent = roomCreateEventContent)
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
        every { roomServiceMock3.getAll() } returns MutableStateFlow(emptyMap())
        every { roomServiceMock1.getById(roomId1) } returns MutableStateFlow(room1)
        every { roomServiceMock1.getById(roomId2) } returns MutableStateFlow(room2)
        every { roomServiceMock2.getById(roomId3) } returns MutableStateFlow(room3)
        every { roomServiceMock2.getById(roomId4) } returns MutableStateFlow(room4)
        val cut = roomListViewModel()
        subscribe(cut)

        cut.searchTerm.update("")
        delay(10)
        cut.elements.value shouldHaveSize 4

        cut.searchTerm.update("  ")
        delay(10)
        cut.elements.value shouldHaveSize 4
    }

    @Test
    fun `contain search term in all search results`() = runTest {
        val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
        val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
        val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
        val room4 = Room(roomId4, createEventContent = roomCreateEventContent)
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
        every { roomServiceMock3.getAll() } returns MutableStateFlow(emptyMap())

        every { roomServiceMock1.getById(roomId1) } returns MutableStateFlow(room1)
        every { roomServiceMock1.getById(roomId2) } returns MutableStateFlow(room2)
        every { roomServiceMock2.getById(roomId3) } returns MutableStateFlow(room3)
        every { roomServiceMock2.getById(roomId4) } returns MutableStateFlow(room4)

        val cut = roomListViewModel()
        subscribe(cut)

        cut.searchTerm.update("2")
        delay(301) // debounce

        cut.elements.value shouldHaveSize 2
        cut.elements.value.should(
            containRoomListElementViewModelsFor(listOf(roomId2, roomId3))
        )
    }

    @Test
    fun `add a newly added room to the search result when it fits the search term`() = runTest {
        val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
        val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
        val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
        val room3NameFlow = MutableStateFlow("room2-other")
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
        every { roomServiceMock3.getAll() } returns MutableStateFlow(emptyMap())
        every { roomServiceMock1.getById(roomId1) } returns MutableStateFlow(room1)
        every { roomServiceMock1.getById(roomId2) } returns MutableStateFlow(room2)
        every { roomServiceMock2.getById(roomId3) } returns MutableStateFlow(room3)
        every {
            roomNameMock.getRoomName(eq(room3), eq(matrixClientMock2), any())
        } returns room3NameFlow
        roomName3Mocker returns room3NameFlow

        val cut = roomListViewModel()
        subscribe(cut)

        cut.searchTerm.update("1")
        delay(301) // debounce
        cut.elements.value.should(containRoomListElementViewModelsFor(listOf(roomId1)))

        room3NameFlow.value = "I am number 1"
        delay(10.seconds) // no debounce, since search term stays the same
        cut.elements.value.should(
            containRoomListElementViewModelsFor(listOf(roomId1, roomId3))
        )
    }

    @Test
    fun `not show spaces in room list`() = runTest {
        val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
        val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
        val space = Room(spaceId1, createEventContent = spaceCreateEventContent)
        val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
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
        every { roomServiceMock3.getAll() } returns MutableStateFlow(emptyMap())
        every { roomServiceMock1.getById(roomId1) } returns MutableStateFlow(room1)
        every { roomServiceMock1.getById(roomId2) } returns MutableStateFlow(room2)
        every { roomServiceMock1.getById(spaceId1) } returns MutableStateFlow(space)
        every { roomServiceMock2.getById(roomId3) } returns MutableStateFlow(room3)
        every { roomServiceMock1.getState(spaceId1, CreateEventContent::class, "") } returns flowOf(
            StateEvent(
                CreateEventContent(
                    federate = false,
                    roomVersion = "",
                    type = RoomType.Space,
                ), EventId(""), UserId(""), RoomId(""), 0L, stateKey = ""
            )
        )

        val cut = roomListViewModel()
        subscribe(cut)
        delay(10)
        cut.elements.value shouldHaveSize 3
    }

    @Test
    fun `react to changes of accounts`() = runTest {
        val room1 = Room(roomId1, createEventContent = roomCreateEventContent)
        val room2 = Room(roomId2, createEventContent = roomCreateEventContent)
        val room3 = Room(roomId3, createEventContent = roomCreateEventContent)
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
        every { roomServiceMock3.getAll() } returns MutableStateFlow(emptyMap())
        every { roomServiceMock1.getById(roomId1) } returns MutableStateFlow(room1)
        every { roomServiceMock1.getById(roomId2) } returns MutableStateFlow(room2)
        every { roomServiceMock2.getById(roomId3) } returns MutableStateFlow(room3)

        val cut = roomListViewModel()
        subscribe(cut)
        delay(10)
        cut.elements.value shouldHaveSize 3

        matrixClients.value = mapOf(
            UserId("test1", "server") to matrixClientMock1
        )
        delay(10)
        cut.elements.value shouldHaveSize 2
    }

    private fun TestScope.subscribe(cut: RoomListViewModel) = backgroundScope.launch {
        launch { cut.selectedRoomId.collect() }
        launch { cut.error.collect() }
        launch { cut.errorType.collect() }
        launch { cut.elements.collect() }
        launch { cut.syncStates.collect() }
        launch { cut.initialSyncFinished.collect() }
        launch { cut.showSearch.collect() }
        launch { cut.searchTerm.collect() }
    }

    private fun TestScope.roomListViewModel(): RoomListViewModelImpl {
        matrixClients = MutableStateFlow(
            mapOf(
                UserId("test1", "server") to matrixClientMock1,
                UserId("test2", "server") to matrixClientMock2,
                UserId("test3", "server") to matrixClientMock3,
            )
        )
        di = koinApplication {
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
                                onShowAccounts: () -> Unit
                            ): AccountViewModel = object : AccountViewModel {
                                override val activeAccount: StateFlow<UserId?> = MutableStateFlow(null)
                                override val isSingleAccount: StateFlow<Boolean> = MutableStateFlow(false)
                                override val accounts: StateFlow<List<AccountInfo>> = MutableStateFlow(listOf())

                                override fun selectActiveAccount(userId: UserId?) {}
                                override fun openUserSettings() {}
                                override fun openUserAccounts() {}
                                override fun openAppInfo() {}
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
                userId = UserId("test1", "server"),
                coroutineContext = backgroundScope.coroutineContext
            ),
            selectedRoomId = MutableStateFlow(RoomId("!roomId")),
            onRoomSelected = onRoomSelectedMock,
            onCreateNewRoom = mock(),
            onShowAccounts = mock(),
            onUserSettingsSelected = mock(),
            onOpenAppInfo = mock(),
            onSendLogs = mock(),
            onAccountSelected = mock(),
            onStartVerification = mock(),
            onCloseRoom = mock(),
        )
    }

    private fun containRoomListElementViewModelsFor(roomIds: List<RoomId>) =
        KoMatcher<List<RoomListElementViewModel>> { list ->
            MatcherResult(roomIds.all { roomId ->
                list.any { element -> element.roomId == roomId }
            }, {
                "RoomListElementViewModel with ids [${
                    roomIds.filterNot { roomId -> list.any { element -> element.roomId == roomId } }
                        .joinToString { it.full }
                }] not found"
            }, {
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
        memberEvent(roomId, userId),
    )

    private fun memberEvent(roomId: RoomId, sender: UserId) = StateEvent(
        content = MemberEventContent(membership = Membership.JOIN),
        id = EventId("1"),
        sender = sender,
        roomId = roomId,
        originTimestamp = 0L,
        stateKey = "",
    )
}
