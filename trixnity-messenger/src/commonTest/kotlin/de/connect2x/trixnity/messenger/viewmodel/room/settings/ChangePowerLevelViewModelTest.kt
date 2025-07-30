package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eqNull
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.runTestWithCoroutineScope
import de.connect2x.trixnity.messenger.testDispatcher
import de.connect2x.trixnity.messenger.util.ImmediateDispatcherElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds


@OptIn(ExperimentalCoroutinesApi::class)
class ChangePowerLevelViewModelTest {

    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")
    private val testUser = UserId("test", "server")
    private val roomId = RoomId("!room")

    private val roomUserAlice = RoomUser(
        roomId, alice, "Alice", StateEvent(
            MemberEventContent(membership = Membership.JOIN),
            EventId(""),
            alice,
            roomId,
            0L,
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
            0L,
            stateKey = "",
        )
    )

    private val roomUserBobFlow = MutableStateFlow(roomUserBob)

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()
    val roomsApiClientMock = mock<RoomApiClient>()
    private val matrixClientServerApiMock = mock<MatrixClientServerApiClient>()
    private lateinit var syncStateMocker: BlockingAnsweringScope<StateFlow<SyncState>>

    private val canSetAlicePowerLevelToMax = MutableStateFlow<Long?>(null)
    private val aliceTargetPowerLevel = MutableStateFlow<Long>(0)

    @BeforeTest
    fun setup() {
        resetMocks(
            matrixClientMock,
            roomServiceMock,
            userServiceMock,
            matrixClientServerApiMock,
            roomsApiClientMock,
        )
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                }
            )
        }.koin
        syncStateMocker = every { matrixClientMock.syncState }
        syncStateMocker returns MutableStateFlow(SyncState.STARTED)

        every { matrixClientMock.api } returns matrixClientServerApiMock
        every { matrixClientServerApiMock.room } returns roomsApiClientMock

        every { userServiceMock.getById(eq(roomId), eq(alice)) } returns roomUserAliceFlow
        every { userServiceMock.getById(eq(roomId), eq(bob)) } returns roomUserBobFlow

        every {
            roomServiceMock.getState(roomId, PowerLevelsEventContent::class, "")
        } returns MutableStateFlow(
            StateEvent(
                PowerLevelsEventContent(),
                EventId("eventId"),
                bob,
                roomId,
                123,
                null,
                "",
            )
        )

        every {
            userServiceMock.canSetPowerLevelToMax(roomId, alice)
        } returns canSetAlicePowerLevelToMax
        every {
            userServiceMock.getPowerLevel(roomId, alice)
        } returns aliceTargetPowerLevel
    }

    @Test
    fun `changing a role should close member options after changing the user role`() =
        runTestWithCoroutineScope { coroutineScope ->
            canSetAlicePowerLevelToMax.value = 100
            every {
                userServiceMock.canSetPowerLevelToMax(eq(roomId), eq(testUser))
            } returns MutableStateFlow(100L)

            everySuspend {
                roomsApiClientMock.sendStateEvent(
                    eq(roomId),
                    any(),
                    any(),
                    eqNull()
                )
            } returns Result.success(EventId(""))

            val cut = changePowerLevelViewModel(backgroundScope, alice)
            cut.setRoleToAdmin()
            delay(100.milliseconds)

            cut.error.value shouldBe null
            verifySuspend {
                roomsApiClientMock.sendStateEvent(
                    eq(roomId),
                    eq(PowerLevelsEventContent(users = mapOf(alice to 100L))),
                    any(),
                    eqNull()
                )
            }
        }

    @Test
    fun `changing a role should show an error message when trying to change a role and we are not connected`() =
        runTestWithCoroutineScope { coroutineScope ->
            canSetAlicePowerLevelToMax.value = 100
            every {
                userServiceMock.canSetPowerLevelToMax(eq(roomId), eq(testUser))
            } returns MutableStateFlow(100L)

            syncStateMocker returns MutableStateFlow(SyncState.ERROR)

            val cut = changePowerLevelViewModel(backgroundScope, alice)
            cut.setRoleToAdmin()
            delay(100.milliseconds)

            cut.error.value shouldNotBe null
        }

    @Test
    fun `changing a role should show an error message when changing a role fails`() =
        runTestWithCoroutineScope { coroutineScope ->
            canSetAlicePowerLevelToMax.value = 100
            every {
                userServiceMock.canSetPowerLevelToMax(roomId, testUser)
            } returns MutableStateFlow(100L)

            everySuspend {
                roomsApiClientMock.sendStateEvent(
                    eq(roomId),
                    any(),
                    any(),
                    eqNull()
                )
            } returns Result.failure(Throwable())

            val cut = changePowerLevelViewModel(backgroundScope, alice)
            cut.setRoleToAdmin()
            delay(100.milliseconds)

            cut.error.value shouldNotBe null
        }

    @Test
    fun `changing the power level should close member options after changing the power level successfully`() =
        runTestWithCoroutineScope { coroutineScope ->
            canSetAlicePowerLevelToMax.value = 100

            everySuspend {
                roomsApiClientMock.sendStateEvent(
                    eq(roomId),
                    any(),
                    any(),
                    eqNull()
                )
            } returns Result.success(EventId(""))

            val cut = changePowerLevelViewModel(backgroundScope, alice)
            cut.setPowerLevelTo(99L)
            delay(100.milliseconds)

            cut.error.value shouldBe null
            verifySuspend {
                roomsApiClientMock.sendStateEvent(
                    eq(roomId),
                    eq(PowerLevelsEventContent(users = mapOf(alice to 99L))),
                    any(),
                    eqNull()
                )
            }
        }

    @Test
    fun `changing the power level should show an error message if trying to change a power level and we are not connected`() =
        runTestWithCoroutineScope { coroutineScope ->
            canSetAlicePowerLevelToMax.value = 100

            syncStateMocker returns MutableStateFlow(SyncState.ERROR)

            val cut = changePowerLevelViewModel(backgroundScope, alice)
            cut.setPowerLevelTo(99L)
            delay(100.milliseconds)

            cut.error.value shouldNotBe null
        }

    @Test
    fun `changing the power level should show an error message if changing the power level fails`() =
        runTestWithCoroutineScope { coroutineScope ->
            canSetAlicePowerLevelToMax.value = 100

            everySuspend {
                roomsApiClientMock.sendStateEvent(
                    eq(roomId),
                    any(),
                    any(),
                    eqNull()
                )
            } returns Result.failure(Throwable())

            val cut = changePowerLevelViewModel(backgroundScope, alice)
            cut.setPowerLevelTo(99L)
            delay(100.milliseconds)

            cut.error.value shouldNotBe null
        }

    @Test
    fun `changing the power level input should show an error message if input is empty`() =
        runTestWithCoroutineScope { coroutineScope ->
            canSetAlicePowerLevelToMax.value = 100

            val cut = changePowerLevelViewModel(backgroundScope, alice)
            coroutineScope.launch { cut.changingPowerLevelDialogError.collect() }

            cut.changingPowerLevelDialogInput.update("")
            delay(10.milliseconds)
            cut.changingPowerLevelDialogError.value shouldBe null
            cut.changingPowerLevelDialogInput.update("  ")
            delay(10.milliseconds)
            cut.changingPowerLevelDialogError.value shouldNotBe null

        }

    @Test
    fun `changing the power level input should show an error message if input is not a number`() =
        runTestWithCoroutineScope { coroutineScope ->
            canSetAlicePowerLevelToMax.value = 100

            val cut = changePowerLevelViewModel(backgroundScope, alice)
            coroutineScope.launch { cut.changingPowerLevelDialogError.collect() }

            cut.changingPowerLevelDialogInput.update(".,")
            delay(10.milliseconds)
            cut.changingPowerLevelDialogError.value shouldNotBe null
            cut.changingPowerLevelDialogInput.update("hjjku")
            delay(10.milliseconds)
            cut.changingPowerLevelDialogError.value shouldNotBe null
        }

    @Test
    fun `changing the power level input should show an error message if input is less than 0 or greater than 100`() =
        runTestWithCoroutineScope { coroutineScope ->
            every {
                userServiceMock.canSetPowerLevelToMax(roomId, alice)
            } returns MutableStateFlow(100)

            val cut = changePowerLevelViewModel(backgroundScope, alice)
            coroutineScope.launch { cut.changingPowerLevelDialogError.collect() }

            cut.changingPowerLevelDialogInput.update("-56")
            delay(10.milliseconds)
            cut.changingPowerLevelDialogError.value shouldNotBe null
            cut.changingPowerLevelDialogInput.update("124")
            delay(10.milliseconds)
            cut.changingPowerLevelDialogError.value shouldNotBe null
        }

    @Test
    fun `changing the power level input should show an error message if input level is higher than allowed to set by us`() =
        runTestWithCoroutineScope { coroutineScope ->
            every {
                userServiceMock.canSetPowerLevelToMax(roomId, alice)
            } returns MutableStateFlow(56)

            every {
                userServiceMock.getPowerLevel(roomId, alice)
            } returns MutableStateFlow(56)

            val cut = changePowerLevelViewModel(backgroundScope, alice)
            coroutineScope.launch { cut.changingPowerLevelDialogError.collect() }

            cut.changingPowerLevelDialogInput.update("57")
            delay(10.milliseconds)
            cut.changingPowerLevelDialogError.value shouldNotBe null
        }

    @Test
    fun `changing the power level input should show an error message if we are not allowed to change the power level`() =
        runTestWithCoroutineScope { coroutineScope ->
            val cut = changePowerLevelViewModel(backgroundScope, alice)
            coroutineScope.launch { cut.changingPowerLevelDialogError.collect() }

            cut.changingPowerLevelDialogInput.update("57")
            delay(10.milliseconds)
            cut.changingPowerLevelDialogError.value shouldNotBe null
        }

    @Test
    fun `canSetPowerLevelToRole - should be false when currentPowerLevel is the same`() =
        runTestWithCoroutineScope { coroutineScope ->
            canSetAlicePowerLevelToMax.value = 100

            val cut = changePowerLevelViewModel(backgroundScope, alice)
            coroutineScope.launch { cut.canSetRoleToUser.collect() } // subscribe all internal flows
            for (role in ChangePowerLevelViewModel.Role.entries) {
                withClue(role) {
                    aliceTargetPowerLevel.value = role.getMinPowerLevel()
                    delay(10.milliseconds)
                    cut.canSetPowerLevelToRole(role).first() shouldBe false
                }
            }
        }

    @Test
    fun `canSetPowerLevelToRole - should be false when we are not allowed to set it - null`() =
        runTestWithCoroutineScope { coroutineScope ->
            val cut = changePowerLevelViewModel(backgroundScope, alice)
            coroutineScope.launch { cut.canSetRoleToUser.collect() } // subscribe all internal flows
            for (role in ChangePowerLevelViewModel.Role.entries) {
                withClue(role) {
                    aliceTargetPowerLevel.value = role.getMinPowerLevel() - 1
                    delay(10.milliseconds)
                    cut.canSetPowerLevelToRole(role).first() shouldBe false
                }
            }
        }

    @Test
    fun `canSetPowerLevelToRole - should be false when we are not allowed to set it`() =
        runTestWithCoroutineScope { coroutineScope ->
            val cut = changePowerLevelViewModel(backgroundScope, alice)
            coroutineScope.launch { cut.canSetRoleToUser.collect() } // subscribe all internal flows
            for (role in ChangePowerLevelViewModel.Role.entries) {
                withClue(role) {
                    canSetAlicePowerLevelToMax.value = role.getMinPowerLevel() - 1
                    aliceTargetPowerLevel.value = role.getMinPowerLevel() - 1
                    delay(10.milliseconds)
                    cut.canSetPowerLevelToRole(role).first() shouldBe false
                }
            }
        }

    @Test
    fun `canSetPowerLevelToRole - should be true when we are allowed to set it`() =
        runTestWithCoroutineScope { coroutineScope ->
            val cut = changePowerLevelViewModel(backgroundScope, alice)
            coroutineScope.launch { cut.canSetRoleToUser.collect() } // subscribe all internal flows
            for (role in ChangePowerLevelViewModel.Role.entries) {
                withClue(role) {
                    canSetAlicePowerLevelToMax.value = role.getMinPowerLevel()
                    aliceTargetPowerLevel.value = role.getMinPowerLevel() - 1
                    delay(10.milliseconds)
                    cut.canSetPowerLevelToRole(role).first() shouldBe true
                }
            }
        }

    private fun TestScope.changePowerLevelViewModel(
        backgroundScope: CoroutineScope,
        userId: UserId,
    ): ChangePowerLevelViewModelImpl {
        return ChangePowerLevelViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(userId to matrixClientMock)
                        ),
                    )
                }.koin,
                userId = userId,
                coroutineContext = backgroundScope.coroutineContext + ImmediateDispatcherElement(testDispatcher),
            ),
            targetUser = userId,
            error = MutableStateFlow(null),
            selectedRoomId = roomId,
        )
    }
}
