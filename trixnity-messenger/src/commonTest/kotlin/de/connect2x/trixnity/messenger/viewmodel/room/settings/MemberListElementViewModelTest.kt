package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.eqNull
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel.Role.ADMIN
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel.Role.MODERATOR
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel.Role.USER
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.util.createTestMatrixMessengerSettingsHolder
import dev.mokkery.answering.BlockingAnsweringScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
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
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class MemberListElementViewModelTest : ShouldSpec() {
    private val me = UserId("user1", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    private val roomId = RoomId("room", "localhost")

    private val memberElementAlice =
        MemberListElementViewModel.MemberElement(null, "Alice", alice.full, "A")

    private val roomUserAlice = RoomUser(
        roomId, alice, "Alice", StateEvent(
            MemberEventContent(membership = Membership.JOIN),
            EventId(""),
            alice,
            roomId,
            0L,
            stateKey = ""
        )
    )

    private val roomUserBob = RoomUser(
        roomId, bob, "Bob", StateEvent(
            MemberEventContent(membership = Membership.JOIN),
            EventId(""),
            bob,
            roomId,
            0L,
            stateKey = ""
        )
    )

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val userServiceMock = mock<UserService>()

    val keyServiceMock = mock<KeyService>()

    val matrixClientServerApiMock = mock<MatrixClientServerApiClient>()

    val usersApiClientMock = mock<UserApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    lateinit var i18n: I18n

    private lateinit var syncStateMocker: BlockingAnsweringScope<StateFlow<SyncState>>

    init {
        coroutineTestScope = true

        beforeTest {

            i18n = object : I18n(DefaultLanguages, createTestMatrixMessengerSettingsHolder(), GetSystemLang { "en" }) {}
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
                    }
                )
            }.koin
            syncStateMocker = every { matrixClientMock.syncState }
            syncStateMocker returns MutableStateFlow(SyncState.STARTED)
            every { matrixClientMock.api } returns matrixClientServerApiMock

            every { matrixClientServerApiMock.room } returns roomsApiClientMock

            every { matrixClientMock.userId } returns me

            every { roomServiceMock.getById(eq(roomId)) } returns MutableStateFlow(
                Room(isDirect = true, roomId = roomId)
            )
            
            every { userServiceMock.getById(eq(roomId), eq(roomUserAlice.userId)) } returns flowOf(roomUserAlice)
            every { userServiceMock.getById(eq(roomId), eq(roomUserBob.userId)) } returns flowOf(roomUserBob)
            every { userServiceMock.canKickUser(eq(roomId), any()) } returns
                    MutableStateFlow(true)
            every { userServiceMock.canBanUser(eq(roomId), any()) } returns
                    MutableStateFlow(true)
            every { userServiceMock.canUnbanUser(eq(roomId), any()) } returns
                    MutableStateFlow(true)
            every { userServiceMock.getPowerLevel(eq(roomId), eq(alice)) } returns
                    MutableStateFlow(50)
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


        should("initially do not create MemberElement before subscription") {

            every { userServiceMock.getPowerLevel(eq(roomId), any()) } returns
                    MutableStateFlow(50)

            val cut = memberListElementViewModel(coroutineContext, roomUserAlice)

            testCoroutineScheduler.advanceTimeBy(200)

            cut.member.value shouldBe null

            cancelNeverEndingCoroutines()
        }

        should("Create MemberElement after subscription") {

            every { userServiceMock.getPowerLevel(eq(roomId), any()) } returns
                    MutableStateFlow(50)

            val cut = memberListElementViewModel(coroutineContext, roomUserAlice)

            launch { cut.member.collect() }

            testCoroutineScheduler.advanceTimeBy(200)

            cut.member.value shouldBe memberElementAlice

            cancelNeverEndingCoroutines()
        }

        context("kicking an user") {
            beforeTest {
                every {
                    userServiceMock.getPowerLevel(eq(roomId), any())
                } returns
                        MutableStateFlow(50)
            }
            should("return to room settings after kicking an user") {
                everySuspend {
                    roomsApiClientMock.kickUser(
                        eq(roomId),
                        eq(alice),
                        eqNull(),
                        eqNull()
                    )
                } returns Result.success(Unit)

                val cut = memberListElementViewModel(coroutineContext, roomUserAlice)
                cut.kickUser(alice)
                testCoroutineScheduler.advanceTimeBy(100.milliseconds)

                cut.error.value shouldBe ""
                verifySuspend {
                    roomsApiClientMock.kickUser(eq(roomId), eq(alice), eqNull(), eqNull())
                }
                cut.memberOptionsOpen.value shouldBe false
                cancelNeverEndingCoroutines()

            }

            should("show an error message when trying to kick an user and we are not connected") {
                syncStateMocker returns MutableStateFlow(SyncState.ERROR)

                val cut = memberListElementViewModel(coroutineContext, roomUserAlice)
                cut.kickUser(alice)

                testCoroutineScheduler.advanceTimeBy(100.milliseconds)
                // we have not mocked roomsApiClientMock.kickUser(), so if they would be called, an exception would be thrown

                cut.error.value shouldNotBe null
                cancelNeverEndingCoroutines()
            }

            should("show an error message when kicking an user fails") {
                everySuspend {
                    roomsApiClientMock.kickUser(
                        eq(roomId),
                        eq(alice),
                        eqNull(),
                        eqNull()
                    )
                } returns
                        Result.failure(RuntimeException("Oh nooo"))

                val cut = memberListElementViewModel(coroutineContext, roomUserAlice)
                cut.kickUser(alice)

                testCoroutineScheduler.advanceTimeBy(100.milliseconds)
                // we have not mocked roomsApiClientMock.kickUser(), so if they would be called, an exception would be thrown

                cut.error.value shouldNotBe null
                cancelNeverEndingCoroutines()
            }
        }

        context("role computation for the member list") {
            beforeTest {
                every {
                    userServiceMock.getPowerLevel(eq(roomId), eq(alice))
                } returns
                        MutableStateFlow(50)
                every {
                    userServiceMock.getPowerLevel(eq(roomId), eq(me))
                } returns
                        MutableStateFlow(50)
            }
            context("Member is admin") {
                should("return the role: admin") {
                    every {
                        userServiceMock.getPowerLevel(roomId, bob)
                    } returns MutableStateFlow(100)
                    val cut = memberListElementViewModel(coroutineContext, roomUserBob)
                    cut.role.first { it != USER } shouldBe ADMIN
                    cancelNeverEndingCoroutines()

                }
                should("show role name in view") {
                    every {
                        userServiceMock.getPowerLevel(roomId, bob)
                    } returns MutableStateFlow(100)
                    val cut = memberListElementViewModel(coroutineContext, roomUserBob)
                    cut.showRole.first { it } shouldBe true
                    cancelNeverEndingCoroutines()

                }
            }
            context("Member is moderator") {
                should("return the role: moderator") {
                    every {
                        userServiceMock.getPowerLevel(roomId, bob)
                    } returns MutableStateFlow(50)
                    val cut = memberListElementViewModel(coroutineContext, roomUserBob)
                    cut.role.first { it != USER } shouldBe MODERATOR
                    cancelNeverEndingCoroutines()
                }
                should("show role name in view") {
                    every {
                        userServiceMock.getPowerLevel(roomId, bob)
                    } returns MutableStateFlow(50)
                    val cut = memberListElementViewModel(coroutineContext, roomUserBob)
                    delay(100)
                    cut.showRole.first { it } shouldBe true
                    cancelNeverEndingCoroutines()
                }

            }

            context("Member is a normal user") {
                should("return the role: user") {
                    every {
                        userServiceMock.getPowerLevel(roomId, bob)
                    } returns MutableStateFlow(0)
                    val cut = memberListElementViewModel(coroutineContext, roomUserBob)
                    cut.role.value shouldBe USER
                    cancelNeverEndingCoroutines()
                }
                should("do not show role name in view") {
                    every {
                        userServiceMock.getPowerLevel(roomId, bob)
                    } returns MutableStateFlow(0)
                    val cut = memberListElementViewModel(coroutineContext, roomUserBob)
                    delay(50)
                    cut.showRole.value shouldBe false
                    cancelNeverEndingCoroutines()
                }
            }
        }

    }


    private suspend fun memberListElementViewModel(
        coroutineContext: CoroutineContext, roomUser: RoomUser
    ): MemberListElementViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        return MemberListElementViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock)),
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext,
            ),
            roomUser,
            error = MutableStateFlow(""),
            selectedRoomId = roomId
        )
    }
}
