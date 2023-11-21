package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel.Role.*
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
import de.connect2x.trixnity.messenger.viewmodel.util.testMessengerSettings
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.KeyService
import net.folivo.trixnity.client.key.UserTrustLevel
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.IgnoredUserListEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class MemberListElementViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val mocker = Mocker()

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

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var userServiceMock: UserService

    @Mock
    lateinit var keyServiceMock: KeyService

    @Mock
    lateinit var matrixClientServerApiMock: MatrixClientServerApiClient

    @Mock
    lateinit var roomsApiClientMock: RoomApiClient

    lateinit var i18n: I18n

    private lateinit var syncStateMocker: Mocker.Every<StateFlow<SyncState>>

    init {
        Dispatchers.setMain(testMainDispatcher)
        coroutineTestScope = true

        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            i18n = object : I18n(DefaultLanguages, testMessengerSettings("en")) {}

            with(mocker) {
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

                every { matrixClientServerApiMock.rooms } returns roomsApiClientMock

                every { matrixClientMock.userId } returns me

                every { roomServiceMock.getById(isEqual(roomId)) } returns MutableStateFlow(
                    Room(isDirect = true, roomId = roomId)
                )

                every { userServiceMock.canKickUser(isEqual(roomId), isAny()) } returns
                        MutableStateFlow(true)
                every { userServiceMock.getPowerLevel(isEqual(roomId), isEqual(alice)) } returns
                        MutableStateFlow(50)
                every {
                    userServiceMock.canSetPowerLevelToMax(isEqual(roomId), isAny())
                } returns MutableStateFlow(100)
                every { userServiceMock.getAccountData<IgnoredUserListEventContent>() } returns flowOf(
                    IgnoredUserListEventContent(emptyMap())
                )

                every { keyServiceMock.getTrustLevel(isAny()) } returns flowOf(UserTrustLevel.Blocked)
            }
        }


        should("initially do not create MemberElement before subscription") {

            mocker.every { userServiceMock.getPowerLevel(isEqual(roomId), isAny()) } returns
                    MutableStateFlow(50)

            val cut = memberListElementViewModel(coroutineContext, roomUserAlice)

            testCoroutineScheduler.advanceTimeBy(200)

            cut.member.value shouldBe null

            cancelNeverEndingCoroutines()
        }

        should("Create MemberElement after subscription") {

            mocker.every { userServiceMock.getPowerLevel(isEqual(roomId), isAny()) } returns
                    MutableStateFlow(50)

            val cut = memberListElementViewModel(coroutineContext, roomUserAlice)

            launch { cut.member.collect() }

            testCoroutineScheduler.advanceTimeBy(200)

            cut.member.value shouldBe memberElementAlice

            cancelNeverEndingCoroutines()
        }

        context("kicking an user") {
            beforeTest {
                mocker.every {
                    userServiceMock.getPowerLevel(isEqual(roomId), isAny())
                } returns
                        MutableStateFlow(50)
            }
            should("return to room settings after kicking an user") {
                mocker.everySuspending {
                    roomsApiClientMock.kickUser(
                        isEqual(roomId),
                        isEqual(alice),
                        isNull(),
                        isNull()
                    )
                } returns Result.success(Unit)

                val cut = memberListElementViewModel(coroutineContext, roomUserAlice)
                cut.kickUser(alice)
                testCoroutineScheduler.advanceUntilIdle()

                cut.error.value shouldBe ""
                mocker.verifyWithSuspend(exhaustive = false, false) {
                    roomsApiClientMock.kickUser(isEqual(roomId), isEqual(alice), isNull(), isNull())
                }
                cut.memberOptionsOpen.value shouldBe false
                cancelNeverEndingCoroutines()

            }

            should("show an error message when trying to kick an user and we are not connected") {
                syncStateMocker returns MutableStateFlow(SyncState.ERROR)

                val cut = memberListElementViewModel(coroutineContext, roomUserAlice)
                cut.kickUser(alice)

                testCoroutineScheduler.advanceUntilIdle()
                // we have not mocked roomsApiClientMock.kickUser(), so if they would be called, an exception would be thrown

                cut.error.value shouldNotBe null
                cancelNeverEndingCoroutines()
            }

            should("show an error message when kicking an user fails") {
                mocker.everySuspending {
                    roomsApiClientMock.kickUser(
                        isEqual(roomId),
                        isEqual(alice),
                        isNull(),
                        isNull()
                    )
                } returns
                        Result.failure(RuntimeException("Oh nooo"))

                val cut = memberListElementViewModel(coroutineContext, roomUserAlice)
                cut.kickUser(alice)

                testCoroutineScheduler.advanceUntilIdle()
                // we have not mocked roomsApiClientMock.kickUser(), so if they would be called, an exception would be thrown

                cut.error.value shouldNotBe null
                cancelNeverEndingCoroutines()
            }
        }

        context("role computation for the member list") {
            beforeTest {
                mocker.every {
                    userServiceMock.getPowerLevel(isEqual(roomId), isEqual(alice))
                } returns
                        MutableStateFlow(50)
                mocker.every {
                    userServiceMock.getPowerLevel(isEqual(roomId), isEqual(me))
                } returns
                        MutableStateFlow(50)
            }
            context("Member is admin") {
                should("return the role: admin") {
                    mocker.every {
                        userServiceMock.getPowerLevel(roomId, bob)
                    } returns MutableStateFlow(100)
                    val cut = memberListElementViewModel(coroutineContext, roomUserBob)
                    cut.role.first { it != USER } shouldBe ADMIN
                    cancelNeverEndingCoroutines()

                }
                should("show role name in view") {
                    mocker.every {
                        userServiceMock.getPowerLevel(roomId, bob)
                    } returns MutableStateFlow(100)
                    val cut = memberListElementViewModel(coroutineContext, roomUserBob)
                    cut.showRole.first { it } shouldBe true
                    cancelNeverEndingCoroutines()

                }
            }

            context("Member is moderator") {
                should("return the role: moderator") {
                    mocker.every {
                        userServiceMock.getPowerLevel(roomId, bob)
                    } returns MutableStateFlow(50)
                    val cut = memberListElementViewModel(coroutineContext, roomUserBob)
                    cut.role.first { it != USER } shouldBe MODERATOR
                    cancelNeverEndingCoroutines()
                }
                should("show role name in view") {
                    mocker.every {
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
                    mocker.every {
                        userServiceMock.getPowerLevel(roomId, bob)
                    } returns MutableStateFlow(0)
                    val cut = memberListElementViewModel(coroutineContext, roomUserBob)
                    cut.role.value shouldBe USER
                    cancelNeverEndingCoroutines()
                }
                should("do not show role name in view") {
                    mocker.every {
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


    private fun memberListElementViewModel(
        coroutineContext: CoroutineContext, roomUser: RoomUser
    ) = MemberListElementViewModelImpl(
        viewModelContext = MatrixClientViewModelContextImpl(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            di = koinApplication {
                modules(
                    trixnityMessengerModule(),
                    testMatrixClientModule(matrixClientMock),
                )
            }.koin,
            accountName = "test",
            coroutineContext = coroutineContext,
        ),
        roomUser,
        error = MutableStateFlow(""),
        selectedRoomId = roomId
    )
}
