package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.eqNull
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ChangePowerLevelViewModel.Role
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifyNoMoreCalls
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.key.KeyService
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.UserPresence
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.client.verification.ActiveUserVerification
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.client.verification.VerificationService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.clientserverapi.client.UserApiClient
import net.folivo.trixnity.clientserverapi.model.users.GetProfile
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.DirectEventContent
import net.folivo.trixnity.core.model.events.m.IgnoredUserListEventContent
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.crypto.key.UserTrustLevel
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

@Suppress("NonAsciiCharacters")
class UserProfileViewModelTest {
    private val me = UserId("user1", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")
    private val carol = UserId("carol", "localhost")

    private val roomId = RoomId("room", "localhost")

    private val memberElementAlice = UserInfoElement(alice, "Alice", "A", null)

    private val roomUserAlice = RoomUser(
        roomId, alice, "Alice", StateEvent(
            MemberEventContent(membership = Membership.JOIN), EventId(""), alice, roomId, 0, stateKey = ""
        )
    )

    private val roomUserAliceFlow = MutableStateFlow(roomUserAlice)

    private val roomUserBob = RoomUser(
        roomId, bob, "Bob", StateEvent(
            MemberEventContent(membership = Membership.JOIN), EventId(""), bob, roomId, 0, stateKey = ""
        )
    )

    private val roomUserBobFlow = MutableStateFlow(roomUserBob)

    private val roomUserMapFlow = MutableStateFlow(mapOf<UserId, MutableStateFlow<RoomUser>>())

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()
    val keyServiceMock = mock<KeyService>()
    val verificationServiceMock = mock<VerificationService>()
    val matrixClientServerApiMock = mock<MatrixClientServerApiClient>()
    val usersApiClientMock = mock<UserApiClient>()
    val roomsApiClientMock = mock<RoomApiClient>()
    val onOpenRoomMock = mock<(UserId, RoomId) -> Unit>()
    val onCloseSettingsMock = mock<() -> Unit>()

    val activeVerificationMock = mock<ActiveUserVerification>()

    val i18n = object : I18n(
        DefaultLanguages,
        createTestMatrixMessengerSettingsHolder(),
        GetSystemLang { "en" },
        TimeZone.of("CET"),
    ) {}

    private val syncStateMocker: BlockingAnsweringScope<StateFlow<SyncState>>

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

        roomUserMapFlow.value = mapOf(alice to roomUserAliceFlow, bob to roomUserBobFlow)

        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                    single { keyServiceMock }
                    single { verificationServiceMock }
                })
        }.koin
        syncStateMocker = every { matrixClientMock.syncState }
        syncStateMocker returns MutableStateFlow(SyncState.STARTED)
        every { matrixClientMock.api } returns matrixClientServerApiMock

        every { matrixClientServerApiMock.room } returns roomsApiClientMock
        every { matrixClientServerApiMock.user } returns usersApiClientMock

        every { matrixClientMock.userId } returns me

        every { roomServiceMock.getById(eq(roomId)) } returns MutableStateFlow(
            Room(isDirect = true, roomId = roomId)
        )
        every { userServiceMock.getAll(eq(roomId)) } returns roomUserMapFlow
        every { userServiceMock.getById(eq(roomId), eq(alice)) } returns roomUserAliceFlow
        every { userServiceMock.getById(eq(roomId), eq(bob)) } returns roomUserBobFlow
        every { userServiceMock.getById(eq(roomId), eq(carol)) } returns flowOf(null)
        every { userServiceMock.canInviteUser(eq(roomId), any()) } returns MutableStateFlow(true)
        every { userServiceMock.canKickUser(eq(roomId), any()) } returns MutableStateFlow(true)
        every { userServiceMock.canBanUser(eq(roomId), any()) } returns MutableStateFlow(true)
        every { userServiceMock.canUnbanUser(eq(roomId), any()) } returns MutableStateFlow(true)
        every { userServiceMock.getPowerLevel(eq(roomId), eq(alice)) } returns MutableStateFlow(50)
        every { userServiceMock.getAccountData<DirectEventContent>(any()) } returns MutableStateFlow(
            DirectEventContent(
                mapOf()
            )
        )
        every {
            userServiceMock.canSetPowerLevelToMax(eq(roomId), any())
        } returns MutableStateFlow(100)
        every { userServiceMock.getAccountData(IgnoredUserListEventContent::class) } returns flowOf(
            IgnoredUserListEventContent(emptyMap())
        )

        everySuspend { roomsApiClientMock.banUser(eq(roomId), any(), any(), any()) } calls {
            val userId = (it.args[1] as UserId)
            roomUserMapFlow.value -= userId
            Result.success(Unit)
        }

        everySuspend { roomsApiClientMock.unbanUser(eq(roomId), any(), any(), any()) } calls {
            val userId = (it.args[1] as UserId)
            val roomUserFlow = userServiceMock.getById(roomId, userId) as MutableStateFlow<RoomUser?>
            setMemberEventContentOf(
                roomUserFlow, MemberEventContent(
                    membership = Membership.LEAVE, reason = it.args[2] as String
                )
            )
            Result.success(Unit)
        }

        every { keyServiceMock.getTrustLevel(any()) } returns flowOf(UserTrustLevel.Blocked)

        every { userServiceMock.getPresence(any()) } returns flowOf(
            UserPresence(Presence.OFFLINE, Clock.System.now())
        )

        everySuspend { usersApiClientMock.getProfile(eq(carol)) } returns Result.success(
            GetProfile.Response(
                displayName = "Carol",
                avatarUrl = null,
            )
        )

        every { onOpenRoomMock.invoke(any(), any()) } returns Unit
        every { onCloseSettingsMock.invoke() } returns Unit
    }


    @Test
    fun `initially do not create MemberElement before subscription`() = runTest {

        every { userServiceMock.getPowerLevel(eq(roomId), any()) } returns MutableStateFlow(50)

        val cut = userProfileViewModel(alice)

        cut.userInfo.value shouldBe null
    }

    @Test
    fun `Create MemberElement after subscription`() = runTest {

        every { userServiceMock.getPowerLevel(eq(roomId), any()) } returns MutableStateFlow(50)

        val cut = userProfileViewModel(alice)
        backgroundScope.launch { cut.userInfo.collect() }
        delay(100)

        cut.userInfo.value?.userId shouldBe memberElementAlice.userId
    }

    @Test
    fun `Fetch Profile from users not in room`() = runTest {
        every { userServiceMock.getPowerLevel(eq(roomId), any()) } returns MutableStateFlow(50)

        val cut = userProfileViewModel(carol)
        backgroundScope.launch { cut.userInfo.collect() }
        delay(100)

        val info = cut.userInfo.value
        requireNotNull(info)

        info.name shouldBe "Carol"
        info.userId shouldBe carol
    }


    fun setupKickingAUser() {
        every {
            userServiceMock.getPowerLevel(eq(roomId), any())
        } returns MutableStateFlow(50)
    }

    @Test
    fun `kicking a user » return to room settings after kicking an user`() = runTest {
        setupKickingAUser()
        everySuspend {
            roomsApiClientMock.kickUser(
                eq(roomId), eq(alice), eqNull(), eqNull()
            )
        } returns Result.success(Unit)

        val cut = userProfileViewModel(alice)
        cut.kickUser()
        yield()

        cut.error.value shouldBe null
        verifySuspend {
            roomsApiClientMock.kickUser(eq(roomId), eq(alice), eqNull(), eqNull())
        }
        cut.kickUserWarningOpen.value shouldBe false

    }

    @Test
    fun `kicking a user » show an error message when trying to kick an user and we are not connected`() = runTest {
        setupKickingAUser()
        syncStateMocker returns MutableStateFlow(SyncState.ERROR)

        val cut = userProfileViewModel(alice)
        cut.kickUser()
        yield()

        // we have not mocked roomsApiClientMock.kickUser(), so if they would be called, an exception would be thrown

        cut.error.value shouldNotBe null
    }

    @Test
    fun `kicking a user » show an error message when kicking an user fails`() = runTest {
        setupKickingAUser()
        everySuspend {
            roomsApiClientMock.kickUser(
                eq(roomId), eq(alice), eqNull(), eqNull()
            )
        } returns Result.failure(RuntimeException("Oh nooo"))

        val cut = userProfileViewModel(alice)
        cut.kickUser()
        yield()

        // we have not mocked roomsApiClientMock.kickUser(), so if they would be called, an exception would be thrown

        cut.error.value shouldNotBe null
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
    fun `role computation for the member list » member is admin » return the role admin`() = runTest {
        setupRoleComputationForTheMemberList()
        every {
            userServiceMock.getPowerLevel(roomId, bob)
        } returns MutableStateFlow(100)
        val cut = userProfileViewModel(bob)
        cut.role.first { it != Role.USER } shouldBe Role.ADMIN
    }

    @Test
    fun `role computation for the member list » member is admin » show role name in view`() = runTest {
        setupRoleComputationForTheMemberList()
        every {
            userServiceMock.getPowerLevel(roomId, bob)
        } returns MutableStateFlow(100)
        val cut = userProfileViewModel(bob)
        cut.showRole.first { it } shouldBe true
    }


    @Test
    fun `role computation for the member list » member is moderator » return the role moderator`() = runTest {
        setupRoleComputationForTheMemberList()
        every {
            userServiceMock.getPowerLevel(roomId, bob)
        } returns MutableStateFlow(50)
        val cut = userProfileViewModel(bob)
        cut.role.first { it != Role.USER } shouldBe Role.MODERATOR
    }

    @Test
    fun `role computation for the member list » member is moderator » show role name in view`() = runTest {
        setupRoleComputationForTheMemberList()
        every {
            userServiceMock.getPowerLevel(roomId, bob)
        } returns MutableStateFlow(50)
        val cut = userProfileViewModel(bob)
        cut.showRole.first { it } shouldBe true
    }


    @Test
    fun `role computation for the member list » member is a normal user » return the role user`() = runTest {
        setupRoleComputationForTheMemberList()
        every {
            userServiceMock.getPowerLevel(roomId, bob)
        } returns MutableStateFlow(0)
        val cut = userProfileViewModel(bob)
        cut.role.value shouldBe Role.USER
    }

    @Test
    fun `role computation for the member list » member is a normal user » do not show role name in view`() = runTest {
        setupRoleComputationForTheMemberList()
        every {
            userServiceMock.getPowerLevel(roomId, bob)
        } returns MutableStateFlow(0)
        val cut = userProfileViewModel(bob)
        cut.showRole.value shouldBe false
    }

    fun setupMembershipHandlingForKnockTest() {
        everySuspend { roomsApiClientMock.kickUser(eq(roomId), any(), any(), any()) } returns Result.success(Unit)
        everySuspend { roomsApiClientMock.inviteUser(eq(roomId), any(), any(), any()) } returns Result.success(Unit)
        every { userServiceMock.getPowerLevel(eq(roomId), any()) } returns MutableStateFlow(50)
    }

    @Test
    fun `knocking » accept knock`() = runTest {
        setupMembershipHandlingForKnockTest()

        val cut = userProfileViewModel(alice)
        cut.acceptKnock()
        delay(500.milliseconds)

        cut.error.value shouldBe null
        verifySuspend {
            roomsApiClientMock.inviteUser(eq(roomId), eq(alice), any(), any())
        }

        cut.membershipChanging.value shouldBe false
    }

    @Test
    fun `knocking » reject knock`() = runTest {
        setupMembershipHandlingForKnockTest()

        val cut = userProfileViewModel(alice)
        cut.rejectKnock()
        delay(500.milliseconds)

        cut.error.value shouldBe null
        verifySuspend {
            roomsApiClientMock.kickUser(eq(roomId), eq(alice), any(), any())
        }
        cut.membershipChanging.value shouldBe false
    }

    @Test
    fun `open the room where the user verification is started`() = runTest {
        every {
            userServiceMock.getPowerLevel(roomId, any())
        } returns MutableStateFlow(50)

        val verificationRoom = RoomId("verificationRoom", "localhost")
        everySuspend { verificationServiceMock.createUserVerificationRequest(alice) } returns Result.success(
            activeVerificationMock
        )
        every { activeVerificationMock.roomId } returns verificationRoom
        every { activeVerificationMock.state } returns MutableStateFlow(ActiveVerificationState.Undefined)

        val cut = userProfileViewModel(alice)
        cut.startVerification(true)
        delay(10.milliseconds)
        verifySuspend { onOpenRoomMock.invoke(me, verificationRoom) }
    }

    @Test
    fun `do not call onOpenRoom when the user verification is done in the same room`() = runTest {
        every {
            userServiceMock.getPowerLevel(roomId, any())
        } returns MutableStateFlow(50)

        everySuspend { verificationServiceMock.createUserVerificationRequest(alice) } returns Result.success(
            activeVerificationMock
        )
        every { activeVerificationMock.roomId } returns roomId
        every { activeVerificationMock.state } returns MutableStateFlow(ActiveVerificationState.Undefined)

        val cut = userProfileViewModel(alice)
        cut.startVerification(true)
        delay(10.milliseconds)
        verify { onCloseSettingsMock.invoke() }
        verifyNoMoreCalls(onOpenRoomMock)
    }

    @Test
    fun `don't send user verification request when user is already verified`() = runTest {
        every {
            userServiceMock.getPowerLevel(roomId, any())
        } returns MutableStateFlow(50)

        val cut = userProfileViewModel(alice)
        every { matrixClientMock.key.getTrustLevel(any()) } returns MutableStateFlow(UserTrustLevel.CrossSigned(true))
        cut.startVerification()
        delay(20)
        verifySuspend(VerifyMode.exactly(0)) { cut.startVerification() }
    }

    private fun setMemberEventContentOf(roomUser: MutableStateFlow<RoomUser?>, eventContent: MemberEventContent) {
        roomUser.value = requireNotNull(roomUser.value).copy(
            event = StateEvent(
                eventContent, EventId(""), requireNotNull(roomUser.value).userId, roomId, 0, stateKey = ""
            )
        )
    }

    private fun TestScope.userProfileViewModel(
        userId: UserId
    ): UserProfileViewModelImpl {
        return UserProfileViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = koinApplication {
                    allowOverride(true)
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("user1", "server") to matrixClientMock)
                        ),
                    )
                }.koin,
                userId = UserId("user1", "server"),
            ),
            userId = userId,
            selectedRoomId = roomId,
            onOpenRoom = onOpenRoomMock,
            onBack = mock(),
            onCloseSettings = onCloseSettingsMock,
        )
    }
}
