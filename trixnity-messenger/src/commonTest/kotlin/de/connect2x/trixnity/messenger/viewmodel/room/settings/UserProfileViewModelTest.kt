package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.eqNull
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.shouldGroup
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ChangePowerLevelViewModel.Role
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.util.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.withCleanup
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
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
import net.folivo.trixnity.clientserverapi.model.users.GetProfile
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.DirectEventContent
import net.folivo.trixnity.core.model.events.m.IgnoredUserListEventContent
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.PresenceEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds


@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class UserProfileViewModelTest : ShouldSpec() {

    private val me = UserId("user1", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")
    private val carol = UserId("carol", "localhost")

    private val roomId = RoomId("room", "localhost")

    private val memberElementAlice =
        UserInfoElement("Alice", alice, "A", null)

    private val roomUserAlice = RoomUser(
        roomId, alice, "Alice", StateEvent(
            MemberEventContent(membership = Membership.JOIN),
            EventId(""),
            alice,
            roomId,
            0,
            stateKey = "",
        )
    )

    private val roomUserAliceFlow = MutableStateFlow(roomUserAlice)

    private val roomUserBob = RoomUser(
        roomId, bob, "Bob", StateEvent(
            MemberEventContent(membership = Membership.JOIN),
            EventId(""),
            bob,
            roomId,
            0,
            stateKey = "",
        )
    )

    private val roomUserBobFlow = MutableStateFlow(roomUserBob)
    private val roomUserMapFlow = MutableStateFlow(mapOf<UserId, MutableStateFlow<RoomUser>>())

    private val matrixClientMock = mock<MatrixClient>()
    private val roomServiceMock = mock<RoomService>()
    private val userServiceMock = mock<UserService>()
    private val keyServiceMock = mock<KeyService>()
    private val matrixClientServerApiMock = mock<MatrixClientServerApiClient>()
    private val usersApiClientMock = mock<UserApiClient>()
    private val roomsApiClientMock = mock<RoomApiClient>()

    private lateinit var i18n: I18n

    private lateinit var syncStateMocker: BlockingAnsweringScope<StateFlow<SyncState>>

    init {
        timeout = 4_000
        coroutineTestScope = true

        beforeEach {

            i18n = object : I18n(
                DefaultLanguages,
                createTestMatrixMessengerSettingsHolder(),
                GetSystemLang { "en" },
                TimeZone.of("CET"),
            ) {}
            resetMocks(
                matrixClientMock,
                roomServiceMock,
                userServiceMock,
                keyServiceMock,
                matrixClientServerApiMock,
                usersApiClientMock,
                roomsApiClientMock,
            )

            roomUserMapFlow.value = mapOf(alice to roomUserAliceFlow, bob to roomUserBobFlow)

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
            syncStateMocker returns MutableStateFlow(SyncState.STARTED)
            every { matrixClientMock.userId } returns me
            every { matrixClientMock.api } returns matrixClientServerApiMock
            every { matrixClientServerApiMock.room } returns roomsApiClientMock
            every { matrixClientServerApiMock.user } returns usersApiClientMock
            every { roomServiceMock.getById(eq(roomId)) } returns
                    MutableStateFlow(Room(isDirect = true, roomId = roomId))
            every { userServiceMock.getAll(eq(roomId)) } returns roomUserMapFlow
            every { userServiceMock.getById(eq(roomId), eq(alice)) } returns roomUserAliceFlow
            every { userServiceMock.getById(eq(roomId), eq(bob)) } returns roomUserBobFlow
            every { userServiceMock.getById(eq(roomId), eq(carol)) } returns flowOf(null)
            every { userServiceMock.canKickUser(eq(roomId), any()) } returns MutableStateFlow(true)
            every { userServiceMock.canBanUser(eq(roomId), any()) } returns MutableStateFlow(true)
            every { userServiceMock.canUnbanUser(eq(roomId), any()) } returns MutableStateFlow(true)
            every { userServiceMock.getPowerLevel(eq(roomId), eq(alice)) } returns MutableStateFlow(50)
            every { userServiceMock.getAccountData<DirectEventContent>(any()) } returns
                    MutableStateFlow(DirectEventContent(mapOf()))
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
                        membership = Membership.LEAVE,
                        reason = it.args[2] as String
                    )
                )
                Result.success(Unit)
            }
            every { keyServiceMock.getTrustLevel(any()) } returns flowOf(UserTrustLevel.Blocked)
            every { userServiceMock.userPresence } returns MutableStateFlow(
                mapOf(me to PresenceEventContent(Presence.OFFLINE))
            )
            everySuspend { usersApiClientMock.getProfile(eq(carol)) } returns Result.success(
                GetProfile.Response(
                    displayName = "Carol",
                    avatarUrl = null,
                )
            )
        }

        should("initially do not create a MemberElement before subscription").withCleanup {
            every { userServiceMock.getPowerLevel(eq(roomId), any()) } returns MutableStateFlow(50)
            val cut = userProfileViewModel(coroutineContext, alice)

            testCoroutineScheduler.advanceTimeBy(200)

            cut.userInfo.value shouldBe null
        }

        should("create a MemberElement after subscription").withCleanup {
            every { userServiceMock.getPowerLevel(eq(roomId), any()) } returns MutableStateFlow(50)
            val cut = userProfileViewModel(coroutineContext, alice)

            launch { cut.userInfo.collect() }
            testCoroutineScheduler.advanceTimeBy(200)

            cut.userInfo.value?.userId shouldBe memberElementAlice.userId
        }

        should("fetch profile from users not in the room").withCleanup {
            every { userServiceMock.getPowerLevel(eq(roomId), any()) } returns MutableStateFlow(50)
            val cut = userProfileViewModel(coroutineContext, carol)

            launch { cut.userInfo.collect() }

            testCoroutineScheduler.advanceTimeBy(200)

            val info = cut.userInfo.value
            requireNotNull(info)

            info.name shouldBe "Carol"
            info.userId shouldBe carol
        }

        shouldGroup("kicking an user") {
            beforeTest {
                every {
                    userServiceMock.getPowerLevel(eq(roomId), any())
                } returns MutableStateFlow(50)
            }

            should("return to room settings after kicking an user").withCleanup {
                everySuspend {
                    roomsApiClientMock.kickUser(
                        eq(roomId),
                        eq(alice),
                        eqNull(),
                        eqNull()
                    )
                } returns Result.success(Unit)

                val cut = userProfileViewModel(coroutineContext, alice)
                cut.kickUser()
                testCoroutineScheduler.advanceUntilIdle()

                cut.error.value shouldBe null
                verifySuspend {
                    roomsApiClientMock.kickUser(eq(roomId), eq(alice), eqNull(), eqNull())
                }
                cut.kickUserWarningOpen.value shouldBe false
            }

            should("show an error message when trying to kick an user and we are not connected").withCleanup {
                syncStateMocker returns MutableStateFlow(SyncState.ERROR)

                val cut = userProfileViewModel(coroutineContext, alice)
                cut.kickUser()

                testCoroutineScheduler.advanceTimeBy(100.milliseconds)
                // we have not mocked roomsApiClientMock.kickUser(), so if they would be called, an exception would be thrown

                cut.error.value shouldNotBe null
            }

            should("show an error message when kicking an user fails").withCleanup {
                everySuspend {
                    roomsApiClientMock.kickUser(
                        eq(roomId),
                        eq(alice),
                        eqNull(),
                        eqNull()
                    )
                } returns
                        Result.failure(RuntimeException("Oh nooo"))

                val cut = userProfileViewModel(coroutineContext, alice)
                cut.kickUser()

                testCoroutineScheduler.advanceTimeBy(100.milliseconds)
                // we have not mocked roomsApiClientMock.kickUser(), so if they would be called, an exception would be thrown

                cut.error.value shouldNotBe null
            }
        }

        shouldGroup("role computation for the member list") {

            beforeTest {
                every {
                    userServiceMock.getPowerLevel(eq(roomId), eq(alice))
                } returns MutableStateFlow(50)

                every {
                    userServiceMock.getPowerLevel(eq(roomId), eq(me))
                } returns MutableStateFlow(50)
            }

            shouldGroup("member is admin") {

                should("return the role: admin").withCleanup {
                    every {
                        userServiceMock.getPowerLevel(roomId, bob)
                    } returns MutableStateFlow(100)
                    val cut = userProfileViewModel(coroutineContext, bob)
                    cut.role.first { it != Role.USER } shouldBe Role.ADMIN
                }

                should("show role name in view").withCleanup {
                    every {
                        userServiceMock.getPowerLevel(roomId, bob)
                    } returns MutableStateFlow(100)
                    val cut = userProfileViewModel(coroutineContext, bob)
                    cut.showRole.first { it } shouldBe true
                }
            }

            shouldGroup("member is moderator") {

                should("return the role: moderator").withCleanup {
                    every {
                        userServiceMock.getPowerLevel(roomId, bob)
                    } returns MutableStateFlow(50)
                    val cut = userProfileViewModel(coroutineContext, bob)
                    cut.role.first { it != Role.USER } shouldBe Role.MODERATOR
                }

                should("show role name in view").withCleanup {
                    every {
                        userServiceMock.getPowerLevel(roomId, bob)
                    } returns MutableStateFlow(50)
                    val cut = userProfileViewModel(coroutineContext, bob)
                    testCoroutineScheduler.advanceTimeBy(100)
                    cut.showRole.first { it } shouldBe true
                }

            }

            shouldGroup("Member is a normal user") {

                should("return the role: user").withCleanup {
                    every {
                        userServiceMock.getPowerLevel(roomId, bob)
                    } returns MutableStateFlow(0)
                    val cut = userProfileViewModel(coroutineContext, bob)
                    cut.role.value shouldBe Role.USER
                }

                should("do not show role name in view").withCleanup {
                    every {
                        userServiceMock.getPowerLevel(roomId, bob)
                    } returns MutableStateFlow(0)
                    val cut = userProfileViewModel(coroutineContext, bob)
                    testCoroutineScheduler.advanceTimeBy(50)
                    cut.showRole.value shouldBe false
                }
            }
        }
    }

    private fun setMemberEventContentOf(roomUser: MutableStateFlow<RoomUser?>, eventContent: MemberEventContent) {
        roomUser.value = requireNotNull(roomUser.value).copy(
            event = StateEvent(
                eventContent,
                EventId(""),
                requireNotNull(roomUser.value).userId,
                roomId,
                0,
                stateKey = ""
            )
        )
    }

    private suspend fun userProfileViewModel(
        coroutineContext: CoroutineContext, userId: UserId,
    ): UserProfileViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        return UserProfileViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(UserId("user1", "server") to matrixClientMock)),
                    )
                }.koin,
                userId = UserId("user1", "server"),
                coroutineContext = coroutineContext,
            ),
            userId = userId,
            selectedRoomId = roomId,
            onOpenRoom = mock(),
            onBack = mock(),
        )
    }
}
