package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ChangePowerLevelViewModel.Role
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.util.createTestMatrixMessengerSettingsHolder
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.KeyService
import net.folivo.trixnity.client.key.UserTrustLevel
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.clientserverapi.client.UserApiClient
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.IgnoredUserListEventContent
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.PresenceEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test

class MemberListElementViewModelTest {
    private val me = UserId("user1", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    private val roomId = RoomId("room", "localhost")

    private val memberElementAlice = MemberListElementViewModel.MemberElement(null, "Alice", alice.full, "A")

    private val roomUserAlice = RoomUser(
        roomId, alice, "Alice", StateEvent(
            MemberEventContent(membership = Membership.JOIN), EventId(""), alice, roomId, 0, stateKey = ""
        )
    )

    private val roomUserBob = RoomUser(
        roomId, bob, "Bob", StateEvent(
            MemberEventContent(membership = Membership.JOIN), EventId(""), bob, roomId, 0, stateKey = ""
        )
    )

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val userServiceMock = mock<UserService>()

    val keyServiceMock = mock<KeyService>()

    val matrixClientServerApiMock = mock<MatrixClientServerApiClient>()

    val usersApiClientMock = mock<UserApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    val i18n = object : I18n(
        DefaultLanguages,
        createTestMatrixMessengerSettingsHolder(),
        GetSystemLang { "en" },
        TimeZone.of("CET"),
    ) {}

    init {
        resetMocks(
            matrixClientMock,
            roomServiceMock,
            userServiceMock,
            keyServiceMock,
            matrixClientServerApiMock,
            usersApiClientMock,
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
        every { matrixClientMock.syncState } returns MutableStateFlow(SyncState.STARTED)
        every { matrixClientMock.api } returns matrixClientServerApiMock

        every { matrixClientServerApiMock.room } returns roomsApiClientMock

        every { matrixClientMock.userId } returns me

        every { roomServiceMock.getById(eq(roomId)) } returns MutableStateFlow(
            Room(isDirect = true, roomId = roomId)
        )

        every { userServiceMock.getById(eq(roomId), eq(roomUserAlice.userId)) } returns flowOf(roomUserAlice)
        every { userServiceMock.getById(eq(roomId), eq(roomUserBob.userId)) } returns flowOf(roomUserBob)
        every { userServiceMock.canKickUser(eq(roomId), any()) } returns MutableStateFlow(true)
        every { userServiceMock.canBanUser(eq(roomId), any()) } returns MutableStateFlow(true)
        every { userServiceMock.canUnbanUser(eq(roomId), any()) } returns MutableStateFlow(true)
        every { userServiceMock.getPowerLevel(eq(roomId), eq(alice)) } returns MutableStateFlow(50)
        every {
            userServiceMock.canSetPowerLevelToMax(eq(roomId), any())
        } returns MutableStateFlow(100)
        every { userServiceMock.getAccountData(IgnoredUserListEventContent::class) } returns flowOf(
            IgnoredUserListEventContent(emptyMap())
        )

        every { keyServiceMock.getTrustLevel(any()) } returns flowOf(UserTrustLevel.Blocked)

        every { userServiceMock.userPresence } returns MutableStateFlow(
            mapOf(me to PresenceEventContent(Presence.OFFLINE))
        )
    }


    @Test
    fun `initially do not create MemberElement before subscription`() = runTest {

        every { userServiceMock.getPowerLevel(eq(roomId), any()) } returns MutableStateFlow(50)

        val cut = memberListElementViewModel(roomUserAlice)

        delay(200)

        cut.member.value shouldBe null
    }

    @Test
    fun `Create MemberElement after subscription`() = runTest {

        every { userServiceMock.getPowerLevel(eq(roomId), any()) } returns MutableStateFlow(50)

        val cut = memberListElementViewModel(roomUserAlice)

        backgroundScope.launch { cut.member.collect() }

        delay(200)

        cut.member.value shouldBe memberElementAlice
    }

    fun setupRoleComputationForTheMemberList() {
        every {
            userServiceMock.getPowerLevel(eq(roomId), eq(alice))
        } returns MutableStateFlow(50)

        every {
            userServiceMock.getPowerLevel(eq(roomId), eq(me))
        } returns MutableStateFlow(50)
    }

    @Test
    fun `role computation for the member list ~ Member is admin ~ return the role admin`() = runTest {
        setupRoleComputationForTheMemberList()
        every {
            userServiceMock.getPowerLevel(roomId, bob)
        } returns MutableStateFlow(100)
        val cut = memberListElementViewModel(roomUserBob)
        cut.role.first { it != Role.USER } shouldBe Role.ADMIN
    }

    @Test
    fun `role computation for the member list ~ Member is admin ~ show role name in view`() = runTest {
        setupRoleComputationForTheMemberList()
        every {
            userServiceMock.getPowerLevel(roomId, bob)
        } returns MutableStateFlow(100)
        val cut = memberListElementViewModel(roomUserBob)
        cut.showRole.first { it } shouldBe true
    }

    @Test
    fun `role computation for the member list ~ Member is moderator ~ return the role moderator`() = runTest {
        setupRoleComputationForTheMemberList()
        every {
            userServiceMock.getPowerLevel(roomId, bob)
        } returns MutableStateFlow(50)
        val cut = memberListElementViewModel(roomUserBob)
        cut.role.first { it != Role.USER } shouldBe Role.MODERATOR
    }

    @Test
    fun `role computation for the member list ~ Member is moderator ~ show role name in view`() = runTest {
        setupRoleComputationForTheMemberList()
        every {
            userServiceMock.getPowerLevel(roomId, bob)
        } returns MutableStateFlow(50)
        val cut = memberListElementViewModel(roomUserBob)
        delay(100)
        cut.showRole.first { it } shouldBe true
    }


    @Test
    fun `role computation for the member list ~ Member is a normal user ~ return the role user`() = runTest {
        setupRoleComputationForTheMemberList()
        every {
            userServiceMock.getPowerLevel(roomId, bob)
        } returns MutableStateFlow(0)
        val cut = memberListElementViewModel(roomUserBob)
        cut.role.value shouldBe Role.USER
    }

    @Test
    fun `role computation for the member list ~ Member is a normal user ~ do not show role name in view`() = runTest {
        setupRoleComputationForTheMemberList()
        every {
            userServiceMock.getPowerLevel(roomId, bob)
        } returns MutableStateFlow(0)
        val cut = memberListElementViewModel(roomUserBob)
        delay(50)
        cut.showRole.value shouldBe false
    }


    private fun TestScope.memberListElementViewModel(
        roomUser: RoomUser
    ): MemberListElementViewModelImpl {
        return MemberListElementViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("test", "server") to matrixClientMock)
                        ),
                    )
                }.koin,
                userId = UserId("test", "server"),
            ), roomUser, selectedRoomId = roomId, onOpenUserProfile = mock()
        )
    }
}
