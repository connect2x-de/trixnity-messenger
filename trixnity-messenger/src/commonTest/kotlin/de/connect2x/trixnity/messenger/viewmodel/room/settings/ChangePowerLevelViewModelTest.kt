package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.eqNull
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
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

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class ChangePowerLevelViewModelTest : ShouldSpec() {

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

    val matrixClientServerApiMock = mock<MatrixClientServerApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    private lateinit var syncStateMocker: BlockingAnsweringScope<StateFlow<SyncState>>

    private val closeMemberOptions = mock<Function0<Unit>>()


    init {
        coroutineTestScope = true

        beforeTest {
            resetMocks(
                matrixClientMock,
                roomServiceMock,
                userServiceMock,
                matrixClientServerApiMock,
                roomsApiClientMock,
                closeMemberOptions
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
                    ""
                )
            )
        }

        context("changing a role") {

            beforeTest {
                every {
                    userServiceMock.canSetPowerLevelToMax(roomId, alice)
                } returns MutableStateFlow(100L)

            }

            should("close member options after changing the user role") {

                var closeMemberOptionsWasCalled = false
                every { closeMemberOptions.invoke() } calls {
                    closeMemberOptionsWasCalled = true
                }

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
                testCoroutineScheduler.advanceUntilIdle()

                cut.error.value shouldBe ""
                verifySuspend {
                    roomsApiClientMock.sendStateEvent(
                        eq(roomId),
                        eq(PowerLevelsEventContent(users = mapOf(alice to 100L))),
                        any(),
                        eqNull()
                    )
                }

                closeMemberOptionsWasCalled shouldBe true
                cancelNeverEndingCoroutines()

            }

            should("show an error message when trying to change a role and we are not connected") {
                syncStateMocker returns MutableStateFlow(SyncState.ERROR)

                var closeMemberOptionsWasCalled = false
                every { closeMemberOptions.invoke() } calls {
                    closeMemberOptionsWasCalled = true
                }

                val cut = changePowerLevelViewModel(
                    coroutineContext, alice, MutableStateFlow(100L)
                )
                cut.setRoleToAdmin()
                testCoroutineScheduler.advanceUntilIdle()

                cut.error.value shouldNotBe null
                closeMemberOptionsWasCalled shouldBe false
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
                testCoroutineScheduler.advanceUntilIdle()

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

                var closeMemberOptionsWasCalled = false
                every { closeMemberOptions.invoke() } calls {
                    closeMemberOptionsWasCalled = true
                }

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
                testCoroutineScheduler.advanceUntilIdle()

                cut.error.value shouldBe ""
                verifySuspend {
                    roomsApiClientMock.sendStateEvent(
                        eq(roomId),
                        eq(PowerLevelsEventContent(users = mapOf(alice to 99L))),
                        any(),
                        eqNull()
                    )
                }

                closeMemberOptionsWasCalled shouldBe true
                cancelNeverEndingCoroutines()
            }
            should("show an error message if trying to change a power level and we are not connected") {

                syncStateMocker returns MutableStateFlow(SyncState.ERROR)

                var closeMemberOptionsWasCalled = false
                every { closeMemberOptions.invoke() } calls {
                    closeMemberOptionsWasCalled = true
                }

                val cut = changePowerLevelViewModel(
                    coroutineContext, alice, MutableStateFlow(100L)
                )
                cut.setPowerLevelTo(99L)
                testCoroutineScheduler.advanceUntilIdle()

                cut.error.value shouldNotBe null
                closeMemberOptionsWasCalled shouldBe false
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
                testCoroutineScheduler.advanceUntilIdle()

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

    private suspend fun changePowerLevelViewModel(
        coroutineContext: CoroutineContext,
        userId: UserId,
        powerLevel: StateFlow<Long>,
    ): ChangePowerLevelViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        return ChangePowerLevelViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock)),
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext
            ),
            userId = userId,
            error = MutableStateFlow(""),
            selectedRoomId = roomId,
            closeMemberOptions = closeMemberOptions,
            powerLevel = powerLevel,
        )
    }
}
