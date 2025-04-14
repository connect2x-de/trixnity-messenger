package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.KeyService
import net.folivo.trixnity.client.key.UserTrustLevel
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.membership
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.IgnoredUserListEventContent
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.PresenceEventContent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class MemberListViewModelTest {
    private val roomId = RoomId("room", "localhost")

    private val me = UserId("user1", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    private val roomUserMeFlow = MutableStateFlow(
        RoomUser(
            roomId, me, "User1", StateEvent(
                MemberEventContent(membership = Membership.JOIN), EventId(""), me, roomId, 0L, stateKey = ""
            )
        )
    )

    private val roomUserAliceFlow = MutableStateFlow(
        RoomUser(
            roomId, alice, "Alice", StateEvent(
                MemberEventContent(membership = Membership.JOIN), EventId(""), alice, roomId, 0L, stateKey = ""
            )
        )
    )

    private val roomUserBobFlow = MutableStateFlow(
        RoomUser(
            roomId, bob, "Bob", StateEvent(
                MemberEventContent(membership = Membership.JOIN), EventId(""), bob, roomId, 0L, stateKey = ""
            )
        )
    )

    private val roomUserMapFlow = MutableStateFlow(mapOf<UserId, MutableStateFlow<RoomUser>>())

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val userServiceMock = mock<UserService>()

    val keyServiceMock = mock<KeyService>()

    val matrixClientServerApiMock = mock<MatrixClientServerApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    init {
        roomUserMapFlow.value = emptyMap()
        roomUserMapFlow.value = mapOf(
            me to roomUserMeFlow,
            alice to roomUserAliceFlow,
            bob to roomUserBobFlow,
        )

        resetMocks(
            matrixClientMock,
            roomsApiClientMock,
            userServiceMock,
            keyServiceMock,
            matrixClientServerApiMock,
            roomsApiClientMock
        )
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                    single { keyServiceMock }
                })
        }.koin

        every { matrixClientMock.syncState } returns MutableStateFlow(SyncState.RUNNING)

        every { matrixClientMock.api } returns matrixClientServerApiMock

        every { matrixClientServerApiMock.room } returns roomsApiClientMock

        every { matrixClientMock.userId } returns me

        every { roomServiceMock.getById(eq(roomId)) } returns MutableStateFlow(
            Room(isDirect = true, roomId = roomId)
        )

        every { userServiceMock.getAll(eq(roomId)) } returns roomUserMapFlow
        every { userServiceMock.canKickUser(eq(roomId), any()) } returns MutableStateFlow(true)
        every { userServiceMock.canBanUser(eq(roomId), any()) } returns MutableStateFlow(true)
        every { userServiceMock.canUnbanUser(eq(roomId), any()) } returns MutableStateFlow(true)
        every { userServiceMock.getPowerLevel(eq(roomId), any()) } returns flowOf(50)
        every { userServiceMock.getPowerLevel(any(), any(), any()) } returns 50

        every { userServiceMock.getById(eq(roomId), eq(me)) } returns roomUserMeFlow
        every { userServiceMock.getById(eq(roomId), eq(alice)) } returns roomUserAliceFlow
        every { userServiceMock.getById(eq(roomId), eq(bob)) } returns roomUserBobFlow
        every { userServiceMock.canSetPowerLevelToMax(eq(roomId), any()) } returns MutableStateFlow(1)
        every { userServiceMock.getAccountData(IgnoredUserListEventContent::class) } returns flowOf(
            IgnoredUserListEventContent(emptyMap())
        )

        everySuspend { roomsApiClientMock.banUser(eq(roomId), any(), any(), any()) } calls {
            val userId = (it.args[1] as UserId)
            val roomUserFlow = userServiceMock.getById(roomId, userId) as MutableStateFlow<RoomUser?>
            setMemberEventContentOf(
                roomUserFlow, MemberEventContent(
                    membership = Membership.BAN, reason = it.args[2] as String
                )
            )
            Result.success(Unit)
        }
        everySuspend { roomsApiClientMock.unbanUser(eq(roomId), any(), any(), any()) } calls {
            val userId = (it.args[1] as UserId)
            roomUserMapFlow.value -= userId
            Result.success(Unit)
        }

        every { keyServiceMock.getTrustLevel(any()) } returns flowOf(UserTrustLevel.Blocked)
        every { userServiceMock.userPresence } returns MutableStateFlow(
            mapOf(me to PresenceEventContent(Presence.OFFLINE))
        )

        setupAliceBobMe()
    }

    fun setupAliceBobMe() {
        val powerLevelsEventContent = PowerLevelsEventContent(users = mapOf(alice to 100, bob to 50, me to 1))
        val createEventContent = CreateEventContent(creator = bob)

        val powerLevelEvent = StateEvent(
            powerLevelsEventContent,
            EventId("I'm an EventId"),
            sender = bob,
            originTimestamp = 123,
            roomId = roomId,
            stateKey = ""
        )
        val createEvent = StateEvent(
            createEventContent,
            EventId("I'm an EventId too"),
            sender = bob,
            originTimestamp = 122,
            roomId = roomId,
            stateKey = ""
        )

        every {
            roomServiceMock.getState(
                roomId, PowerLevelsEventContent::class, ""
            )
        } returns MutableStateFlow(powerLevelEvent)

        every {
            roomServiceMock.getState(
                roomId, CreateEventContent::class, ""
            )
        } returns MutableStateFlow(createEvent)

        every {
            userServiceMock.getPowerLevel(
                alice,
                bob,
                powerLevelsEventContent = powerLevelsEventContent,
            )
        } returns 100

        every {
            userServiceMock.getPowerLevel(
                bob,
                bob,
                powerLevelsEventContent = powerLevelsEventContent,
            )
        } returns 50

        every {
            userServiceMock.getPowerLevel(
                me,
                bob,
                powerLevelsEventContent = powerLevelsEventContent,
            )
        } returns 1
    }

    @Test
    fun `create List of sorted MemberListElementViewModels after initiation and subscription`() = runTest {
        val cut = memberListViewModel()

        eventually(2.seconds) {
            cut.elements.value.size shouldBe 3
        }

        eventually(2.seconds) {
            cut.elements.value should containSortedMemberListElementViewModelsFor(
                listOf(alice, bob, me)
            )
        }
    }

    @Test
    fun `Calculate membership amounts in a Room with 3 joined Members`() = runTest {
        val (roomAlice, roomBob, roomMe) = setMembershipsAndGetRoomUsers(
            alices = Membership.JOIN,
            bobs = Membership.JOIN,
            mine = Membership.JOIN,
        )

        delay(2.seconds)

        requireNotNull(roomAlice.value).membership shouldBe Membership.JOIN
        requireNotNull(roomBob.value).membership shouldBe Membership.JOIN
        requireNotNull(roomMe.value).membership shouldBe Membership.JOIN

        val cut = memberListViewModel()

        delay(2.seconds)

        cut.elements.value.size shouldBe 3

        cut.membershipCounts.value[Membership.JOIN] shouldBe 3
        cut.membershipCounts.value[Membership.BAN] shouldBe 0
        cut.membershipCounts.value[Membership.INVITE] shouldBe 0
        cut.membershipCounts.value[Membership.KNOCK] shouldBe 0
        cut.membershipCounts.value[Membership.LEAVE] shouldBe 0
    }

    @Test
    fun `Calculate membership amounts in a Room containing 1 banned and 2 joined Members`() = runTest {
        val (roomAlice, roomBob, roomMe) = setMembershipsAndGetRoomUsers(
            alices = Membership.JOIN,
            bobs = Membership.JOIN,
            mine = Membership.BAN,
        )
        eventually(2.seconds) {
            requireNotNull(roomAlice.value).membership shouldBe Membership.JOIN
            requireNotNull(roomBob.value).membership shouldBe Membership.JOIN
            requireNotNull(roomMe.value).membership shouldBe Membership.BAN
        }

        val cut = memberListViewModel()

        eventually(2.seconds) {
            cut.membershipCounts.value[Membership.JOIN] shouldBe 2
            cut.membershipCounts.value[Membership.BAN] shouldBe 1
            cut.membershipCounts.value[Membership.INVITE] shouldBe 0
            cut.membershipCounts.value[Membership.KNOCK] shouldBe 0
            cut.membershipCounts.value[Membership.LEAVE] shouldBe 0
        }
    }

    @Test
    fun `Calculate membership amounts in a Room containing 1 left and 2 knocking Members`() = runTest {
        val (roomAlice, roomBob, roomMe) = setMembershipsAndGetRoomUsers(
            alices = Membership.KNOCK,
            bobs = Membership.KNOCK,
            mine = Membership.LEAVE,
        )

        eventually(2.seconds) {
            requireNotNull(roomAlice.value).membership shouldBe Membership.KNOCK
            requireNotNull(roomBob.value).membership shouldBe Membership.KNOCK
            requireNotNull(roomMe.value).membership shouldBe Membership.LEAVE
        }

        val cut = memberListViewModel()

        eventually(2.seconds) {
            cut.membershipCounts.value[Membership.JOIN] shouldBe 0
            cut.membershipCounts.value[Membership.BAN] shouldBe 0
            cut.membershipCounts.value[Membership.INVITE] shouldBe 0
            cut.membershipCounts.value[Membership.KNOCK] shouldBe 2
            cut.membershipCounts.value[Membership.LEAVE] shouldBe 1
        }
    }

    @Test
    fun `Calculate membership amounts in a Room containing 1 joined and 2 invited Members`() = runTest {
        val (roomAlice, roomBob, roomMe) = setMembershipsAndGetRoomUsers(
            alices = Membership.INVITE,
            bobs = Membership.INVITE,
            mine = Membership.JOIN,
        )

        delay(2.seconds)

        requireNotNull(roomAlice.value).membership shouldBe Membership.INVITE
        requireNotNull(roomBob.value).membership shouldBe Membership.INVITE
        requireNotNull(roomMe.value).membership shouldBe Membership.JOIN

        val cut = memberListViewModel()

        delay(2.seconds)

        cut.membershipCounts.value[Membership.JOIN] shouldBe 1
        cut.membershipCounts.value[Membership.BAN] shouldBe 0
        cut.membershipCounts.value[Membership.INVITE] shouldBe 2
        cut.membershipCounts.value[Membership.KNOCK] shouldBe 0
        cut.membershipCounts.value[Membership.LEAVE] shouldBe 0
    }

    private fun setMembershipsAndGetRoomUsers(
        alices: Membership, bobs: Membership, mine: Membership
    ): Triple<StateFlow<RoomUser?>, StateFlow<RoomUser?>, StateFlow<RoomUser?>> {
        val roomAlice = userServiceMock.getById(roomId, alice) as MutableStateFlow<RoomUser?>
        setMemberEventContentOf(roomAlice, MemberEventContent(membership = alices))

        val roomBob = userServiceMock.getById(roomId, bob) as MutableStateFlow<RoomUser?>
        setMemberEventContentOf(roomBob, MemberEventContent(membership = bobs))

        val roomMe = userServiceMock.getById(roomId, me) as MutableStateFlow<RoomUser?>
        setMemberEventContentOf(roomMe, MemberEventContent(membership = mine))

        return Triple(roomAlice, roomBob, roomMe)
    }

    private fun setMemberEventContentOf(roomUser: MutableStateFlow<RoomUser?>, eventContent: MemberEventContent) {
        roomUser.value = requireNotNull(roomUser.value).copy(
            event = StateEvent(
                eventContent, EventId(""), requireNotNull(roomUser.value).userId, roomId, 0, stateKey = ""
            )
        )
    }

    private fun containSortedMemberListElementViewModelsFor(userIds: List<UserId>) =
        Matcher<List<MemberListElementViewModel>> { resultList ->
            MatcherResult(userIds.foldIndexed(true) { index, acc, userId ->
                val vm = resultList.getOrElse(index) { null }
                acc && (vm?.memberUserId == userId)
            }, {
                "Expecting: $userIds\nbut was:   " + resultList.fold(listOf<UserId>()) { acc, vm ->
                    acc + vm.memberUserId
                }
            }, {
                "Expecting: $userIds\nbut was:   " + resultList.fold(listOf<UserId>()) { acc, vm ->
                    acc + vm.memberUserId
                }
            })
        }


    private fun TestScope.memberListViewModel(): MemberListViewModelImpl {
        return MemberListViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("test", "server") to matrixClientMock)
                        ),
                    )
                }.koin,
                userId = UserId("test", "server"),
            ), selectedRoomId = roomId, onOpenUserProfile = mock(), error = MutableStateFlow("")
        ).also {
            backgroundScope.launch {
                it.elements.collect { }
            }

            backgroundScope.launch {
                it.membershipCounts.collect { }
            }
        }
    }
}
