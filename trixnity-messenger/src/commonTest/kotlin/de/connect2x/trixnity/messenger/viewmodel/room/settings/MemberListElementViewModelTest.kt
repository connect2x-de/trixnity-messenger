package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ChangePowerLevelViewModel.Role
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
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
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.key.KeyService
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.store.UserPresence
import de.connect2x.trixnity.client.user.PowerLevel
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.RoomApiClient
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.clientserverapi.client.UserApiClient
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import de.connect2x.trixnity.core.model.events.m.IgnoredUserListEventContent
import de.connect2x.trixnity.core.model.events.m.Presence
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.crypto.key.UserTrustLevel
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Clock

@Suppress("NonAsciiCharacters")
class MemberListElementViewModelTest {
    private val me = UserId("user1", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    private val roomId = RoomId("!room")

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

        every { roomServiceMock.getById(roomId) } returns MutableStateFlow(
            Room(isDirect = true, roomId = roomId)
        )

        every { userServiceMock.getById(roomId, roomUserAlice.userId) } returns flowOf(roomUserAlice)
        every { userServiceMock.getById(roomId, roomUserBob.userId) } returns flowOf(roomUserBob)
        every { userServiceMock.canKickUser(roomId, any()) } returns MutableStateFlow(true)
        every { userServiceMock.canBanUser(roomId, any()) } returns MutableStateFlow(true)
        every { userServiceMock.canUnbanUser(roomId, any()) } returns MutableStateFlow(true)
        every { userServiceMock.getPowerLevel(roomId, alice) } returns MutableStateFlow(PowerLevel.User(50))
        every {
            userServiceMock.canSetPowerLevelToMax(roomId, any())
        } returns MutableStateFlow(PowerLevel.User(100))
        every { userServiceMock.getAccountData(IgnoredUserListEventContent::class) } returns flowOf(
            IgnoredUserListEventContent(emptyMap())
        )

        every { keyServiceMock.getTrustLevel(any()) } returns flowOf(UserTrustLevel.Blocked)

        every { userServiceMock.getPresence(any()) } returns flowOf(
            UserPresence(Presence.OFFLINE, Clock.System.now())
        )
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `initially do not create MemberElement before subscription`() = runTest {

        every { userServiceMock.getPowerLevel(roomId, any()) } returns MutableStateFlow(PowerLevel.User(50))

        val cut = memberListElementViewModel(roomUserAlice)

        delay(200)

        cut.member.value shouldBe null
    }

    @Test
    fun `Create MemberElement after subscription`() = runTest {

        every { userServiceMock.getPowerLevel(roomId, any()) } returns MutableStateFlow(PowerLevel.User(50))

        val cut = memberListElementViewModel(roomUserAlice)

        backgroundScope.launch { cut.member.collect() }

        delay(200)

        cut.member.value shouldBe memberElementAlice
    }

    fun setupRoleComputationForTheMemberList() {
        every {
            userServiceMock.getPowerLevel(roomId, alice)
        } returns MutableStateFlow(PowerLevel.User(50))

        every {
            userServiceMock.getPowerLevel(roomId, me)
        } returns MutableStateFlow(PowerLevel.User(50))
    }

    @Test
    fun `role computation for the member list » Member is admin » return the role admin`() = runTest {
        setupRoleComputationForTheMemberList()
        every {
            userServiceMock.getPowerLevel(roomId, bob)
        } returns MutableStateFlow(PowerLevel.User(100))
        val cut = memberListElementViewModel(roomUserBob)
        cut.role.first { it != Role.USER } shouldBe Role.ADMIN
    }

    @Test
    fun `role computation for the member list » Member is admin » show role name in view`() = runTest {
        setupRoleComputationForTheMemberList()
        every {
            userServiceMock.getPowerLevel(roomId, bob)
        } returns MutableStateFlow(PowerLevel.User(100))
        val cut = memberListElementViewModel(roomUserBob)
        cut.showRole.first { it } shouldBe true
    }

    @Test
    fun `role computation for the member list » Member is moderator » return the role moderator`() = runTest {
        setupRoleComputationForTheMemberList()
        every {
            userServiceMock.getPowerLevel(roomId, bob)
        } returns MutableStateFlow(PowerLevel.User(50))
        val cut = memberListElementViewModel(roomUserBob)
        cut.role.first { it != Role.USER } shouldBe Role.MODERATOR
    }

    @Test
    fun `role computation for the member list » Member is moderator » show role name in view`() = runTest {
        setupRoleComputationForTheMemberList()
        every {
            userServiceMock.getPowerLevel(roomId, bob)
        } returns MutableStateFlow(PowerLevel.User(50))
        val cut = memberListElementViewModel(roomUserBob)
        delay(100)
        cut.showRole.first { it } shouldBe true
    }


    @Test
    fun `role computation for the member list » Member is a normal user » return the role user`() = runTest {
        setupRoleComputationForTheMemberList()
        every {
            userServiceMock.getPowerLevel(roomId, bob)
        } returns MutableStateFlow(PowerLevel.User(0))
        val cut = memberListElementViewModel(roomUserBob)
        cut.role.value shouldBe Role.USER
    }

    @Test
    fun `role computation for the member list » Member is a normal user » do not show role name in view`() = runTest {
        setupRoleComputationForTheMemberList()
        every {
            userServiceMock.getPowerLevel(roomId, bob)
        } returns MutableStateFlow(PowerLevel.User(0))
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
