package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.eqNull
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.setMain
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
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds


@OptIn(ExperimentalCoroutinesApi::class)
class ChangePowerLevelViewModelTest : ShouldSpec() {

    private val bob = UserId("bob", "localhost")
    private val alice = UserId("alice", "localhost")
    private val roomId = RoomId("room", "localhost")
    private val testUser = UserId("test", "server")

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

    init {
        coroutineTestScope = true

        beforeTest {
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
        }

        context("changing a role") {

            beforeTest {
                every {
                    userServiceMock.canSetPowerLevelToMax(eq(roomId), eq(alice))
                } returns MutableStateFlow(100L)
                every {
                    userServiceMock.canSetPowerLevelToMax(eq(roomId), eq(testUser))
                } returns MutableStateFlow(100L)
            }

            should("close member options after changing the user role") {

                everySuspend {
                    roomsApiClientMock.sendStateEvent(
                        eq(roomId),
                        any(),
                        any(),
                        eqNull()
                    )
                } returns Result.success(EventId(""))

                val cut = changePowerLevelViewModel(
                    coroutineContext, alice, MutableStateFlow(100L)
                )
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

                cancelNeverEndingCoroutines()
            }

            should("show an error message when trying to change a role and we are not connected") {
                syncStateMocker returns MutableStateFlow(SyncState.ERROR)

                val cut = changePowerLevelViewModel(
                    coroutineContext, alice, MutableStateFlow(100L)
                )
                cut.setRoleToAdmin()
                delay(100.milliseconds)

                cut.error.value shouldNotBe null
                cancelNeverEndingCoroutines()
            }

            should("show an error message when changing a role fails") {

                everySuspend {
                    roomsApiClientMock.sendStateEvent(
                        eq(roomId),
                        any(),
                        any(),
                        eqNull()
                    )
                } returns Result.failure(Throwable())

                val cut = changePowerLevelViewModel(
                    coroutineContext, alice, MutableStateFlow(100L)
                )
                cut.setRoleToAdmin()
                delay(100.milliseconds)

                cut.error.value shouldNotBe null
                cancelNeverEndingCoroutines()
            }
        }

        context("change Power Level") {

            beforeTest {
                every {
                    userServiceMock.canSetPowerLevelToMax(roomId, alice)
                } returns MutableStateFlow(100L)
            }

            should("close member options after changing the power level successfully") {

                everySuspend {
                    roomsApiClientMock.sendStateEvent(
                        eq(roomId),
                        any(),
                        any(),
                        eqNull()
                    )
                } returns Result.success(EventId(""))

                val cut = changePowerLevelViewModel(
                    coroutineContext, alice, MutableStateFlow(100L)
                )
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

                cancelNeverEndingCoroutines()
            }

            should("show an error message if trying to change a power level and we are not connected") {

                syncStateMocker returns MutableStateFlow(SyncState.ERROR)

                val cut = changePowerLevelViewModel(
                    coroutineContext, alice, MutableStateFlow(100L)
                )
                cut.setPowerLevelTo(99L)
                delay(100.milliseconds)

                cut.error.value shouldNotBe null
                cancelNeverEndingCoroutines()
            }

            should("show an error message if changing the power level fails") {

                everySuspend {
                    roomsApiClientMock.sendStateEvent(
                        eq(roomId),
                        any(),
                        any(),
                        eqNull()
                    )
                } returns Result.failure(Throwable())

                val cut = changePowerLevelViewModel(
                    coroutineContext, alice, MutableStateFlow(100L)
                )
                cut.setPowerLevelTo(99L)
                delay(100.milliseconds)

                cut.error.value shouldNotBe null
                cancelNeverEndingCoroutines()
            }
        }

        context("power level input") {

            should("show an error message if input is empty") {

                every {
                    userServiceMock.canSetPowerLevelToMax(roomId, alice)
                } returns MutableStateFlow(100L)

                val cut = changePowerLevelViewModel(
                    coroutineContext, alice, MutableStateFlow(100L)
                )

                cut.onPowerLevelEntered("")
                cut.changingPowerLevelDialogInput.value.errorId shouldNotBe null
                cut.onPowerLevelEntered("  ")
                cut.changingPowerLevelDialogInput.value.errorId shouldNotBe null

                cancelNeverEndingCoroutines()
            }

            should("show an error message if input is not a number") {

                every {
                    userServiceMock.canSetPowerLevelToMax(roomId, alice)
                } returns MutableStateFlow(100L)

                val cut = changePowerLevelViewModel(
                    coroutineContext, alice, MutableStateFlow(100L)
                )

                cut.onPowerLevelEntered(".,")
                cut.changingPowerLevelDialogInput.value.errorId shouldNotBe null
                cut.onPowerLevelEntered("hjjku")
                cut.changingPowerLevelDialogInput.value.errorId shouldNotBe null

                cancelNeverEndingCoroutines()
            }

            should("show an error message if input is < 0 or > 100") {

                every {
                    userServiceMock.canSetPowerLevelToMax(roomId, alice)
                } returns MutableStateFlow(100)

                val cut = changePowerLevelViewModel(
                    coroutineContext, alice, MutableStateFlow(100L)
                )

                cut.onPowerLevelEntered("-56")
                cut.changingPowerLevelDialogInput.value.errorId shouldNotBe null
                cut.onPowerLevelEntered("124")
                cut.changingPowerLevelDialogInput.value.errorId shouldNotBe null

                cancelNeverEndingCoroutines()
            }

            should("show an error message if input level is higher than allowed to set by us") {

                every {
                    userServiceMock.canSetPowerLevelToMax(roomId, alice)
                } returns MutableStateFlow(56)

                val cut = changePowerLevelViewModel(
                    coroutineContext, alice, MutableStateFlow(56L)
                )

                cut.onPowerLevelEntered("57")
                cut.changingPowerLevelDialogInput.value.errorId shouldNotBe null

                cancelNeverEndingCoroutines()
            }

            should("show an error message if we are not allowed to change the power level") {

                every {
                    userServiceMock.canSetPowerLevelToMax(roomId, alice)
                } returns MutableStateFlow(null)

                val cut = changePowerLevelViewModel(
                    coroutineContext, alice, MutableStateFlow(100L)
                )

                cut.onPowerLevelEntered("57")
                cut.changingPowerLevelDialogInput.value.errorId shouldNotBe null

                cancelNeverEndingCoroutines()
            }
        }
    }

    private fun changePowerLevelViewModel(
        coroutineContext: CoroutineContext,
        userId: UserId,
        powerLevel: StateFlow<Long>,
    ): ChangePowerLevelViewModelImpl {
        Dispatchers.setMain(Dispatchers.Unconfined)
        return ChangePowerLevelViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(userId to matrixClientMock)),
                    )
                }.koin,
                userId = userId,
                coroutineContext = coroutineContext,
            ),
            targetUser = userId,
            error = MutableStateFlow(null),
            selectedRoomId = roomId,
            powerLevel = powerLevel,
        )
    }
}
