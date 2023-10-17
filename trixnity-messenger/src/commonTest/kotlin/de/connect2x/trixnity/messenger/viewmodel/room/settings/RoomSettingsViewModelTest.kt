package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomDisplayName
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomsApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.m.PushRulesEventContent
import net.folivo.trixnity.core.model.events.m.room.*
import net.folivo.trixnity.core.model.push.PushAction.Notify
import net.folivo.trixnity.core.model.push.PushCondition
import net.folivo.trixnity.core.model.push.PushRule
import net.folivo.trixnity.core.model.push.PushRuleSet
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomSettingsViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 4_000

    val mocker = Mocker()

    private val roomId = RoomId("room", "localhost")

    private val me = UserId("user1", "localhost")

    private val roomUserMe = RoomUser(
        roomId,
        me,
        "User1",
        StateEvent(
            MemberEventContent(membership = Membership.JOIN),
            EventId(""),
            me,
            roomId,
            0L,
            stateKey = ""
        )
    )

    private val powerLevelsEventContent =
        PowerLevelsEventContent(users = mapOf(me to 100))
    private val createEventContent = CreateEventContent(creator = me)

    private val powerLevelEvent = StateEvent(
        powerLevelsEventContent,
        EventId("I'm an EventId"),
        sender = me,
        originTimestamp = 123,
        roomId = roomId,
        stateKey = ""
    )
    private val createEvent = StateEvent(
        createEventContent,
        EventId("I'm an EventId too"),
        sender = me,
        originTimestamp = 122,
        roomId = roomId,
        stateKey = ""
    )

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var userServiceMock: UserService

    @Mock
    lateinit var matrixClientServerApiMock: MatrixClientServerApiClient

    @Mock
    lateinit var roomsApiClientMock: RoomsApiClient

    private lateinit var syncStateMocker: Mocker.Every<StateFlow<SyncState>>

    init {
        Dispatchers.setMain(testMainDispatcher)
        coroutineTestScope = true

        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
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
                every { matrixClientMock.userId } returns me

                every { matrixClientServerApiMock.rooms } returns roomsApiClientMock

                every {
                    roomServiceMock.getState(
                        roomId,
                        PowerLevelsEventContent::class,
                        ""
                    )
                } returns MutableStateFlow(powerLevelEvent)
                every {
                    roomServiceMock.getState(
                        roomId,
                        CreateEventContent::class,
                        ""
                    )
                } returns MutableStateFlow(createEvent)

                every {
                    userServiceMock.getAll(isEqual(roomId))
                } returns MutableStateFlow(
                    mapOf(
                        roomUserMe.userId to flowOf(roomUserMe),
                    )
                )

                every { userServiceMock.canKickUser(isEqual(roomId), isAny()) } returns
                        MutableStateFlow(true)

                every { userServiceMock.canInvite(isAny()) } returns
                        MutableStateFlow(true)

                every { userServiceMock.canSetPowerLevelToMax(isEqual(roomId), isAny()) } returns MutableStateFlow(100)

            }
        }

        should("go back to the room list view when leaving the room successfully") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(null)
            mocker.everySuspending {
                roomsApiClientMock.leaveRoom(
                    isEqual(roomId),
                    isAny(),
                    isNull()
                )
            } returns
                    Result.success(Unit)
            val onBackMock = mockFunction0(mocker) {}
            val cut = roomSettingsViewModel(coroutineContext, onBackMock)

            cut.leaveRoom()
            testCoroutineScheduler.advanceUntilIdle()

            mocker.verifyWithSuspend(exhaustive = false, inOrder = false) {
                roomsApiClientMock.leaveRoom(isEqual(roomId), isAny(), isNull())
                onBackMock()
            }

            cancelNeverEndingCoroutines()
        }

        should("show an error message when trying to leave a room and we are not connected") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(null)
            syncStateMocker returns MutableStateFlow(SyncState.ERROR)

            val cut = roomSettingsViewModel(coroutineContext)
            cut.leaveRoom()
            testCoroutineScheduler.advanceUntilIdle()

            cut.error.value shouldNotBe null
            // we have not mocked roomsApiClientMock.leaveRoom() and onBackMock.invoke(), so if they would be called, an exception would be thrown

            cancelNeverEndingCoroutines()
        }

        should("show an error message when leaving the room fails") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(null)
            mocker.everySuspending {
                roomsApiClientMock.leaveRoom(
                    isEqual(roomId),
                    isAny(),
                    isNull()
                )
            } returns
                    Result.failure(RuntimeException("Oh no!"))

            val cut = roomSettingsViewModel(coroutineContext)
            cut.leaveRoom()
            testCoroutineScheduler.advanceUntilIdle()

            // onBackMock is not mocked correctly, so if called, an exception would be thrown
            cut.error.value shouldNotBe null

            cancelNeverEndingCoroutines()
        }

        should("not allow to invite users") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(null)

            mocker.every { userServiceMock.canInvite(roomId) } returns
                    MutableStateFlow(false)
            val cut = roomSettingsViewModel(coroutineContext)

            delay(50)
            cut.hasPowerToInvite.first() shouldBe false

            cancelNeverEndingCoroutines()
        }


        should("allow to invite users") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(null)

            mocker.every { userServiceMock.canInvite(roomId) } returns
                    MutableStateFlow(true)
            val cut = roomSettingsViewModel(coroutineContext)
            cut.hasPowerToInvite.first { it } shouldBe true

            cancelNeverEndingCoroutines()
        }
    }

    private fun roomSettingsViewModel(
        coroutineContext: CoroutineContext,
        onBackMock: () -> Unit = mockFunction0(mocker),
    ) = RoomSettingsViewModelImpl(
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
        selectedRoomId = roomId,
        onBack = onBackMock,
        onCloseRoomSettings = mockFunction0(mocker),

        onShowAddMembers = mockFunction0(mocker)
    )
}
