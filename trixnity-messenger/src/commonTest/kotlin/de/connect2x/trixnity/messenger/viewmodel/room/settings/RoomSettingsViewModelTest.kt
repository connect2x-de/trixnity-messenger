package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.eqNull
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.PushRulesEventContent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext


@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomSettingsViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 4_000

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

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val userServiceMock = mock<UserService>()

    val matrixClientServerApiMock = mock<MatrixClientServerApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    private lateinit var syncStateMocker: BlockingAnsweringScope<StateFlow<SyncState>>

    init {
        coroutineTestScope = true

        beforeTest {
            resetMocks(
                matrixClientMock,
                roomServiceMock,
                userServiceMock,
                matrixClientServerApiMock,
                roomsApiClientMock
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
            every { matrixClientMock.userId } returns me

            every { matrixClientServerApiMock.room } returns roomsApiClientMock

            every { roomServiceMock.getById(any()) } returns flowOf(Room(roomId))
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
                userServiceMock.getAll(eq(roomId))
            } returns MutableStateFlow(
                mapOf(
                    roomUserMe.userId to flowOf(roomUserMe),
                )
            )

            every { userServiceMock.canKickUser(eq(roomId), any()) } returns
                    MutableStateFlow(true)

            every { userServiceMock.canInvite(any()) } returns
                    MutableStateFlow(true)

            every { userServiceMock.canSetPowerLevelToMax(eq(roomId), any()) } returns MutableStateFlow(100)
        }

        should("go back to the room list view when leaving the room successfully") {
            every {
                userServiceMock.getAccountData(eq(PushRulesEventContent::class), any())
            } returns MutableStateFlow(null)
            everySuspend {
                roomsApiClientMock.leaveRoom(
                    eq(roomId),
                    any(),
                    eqNull()
                )
            } returns
                    Result.success(Unit)
            val onBackMock = mock<Function0<Unit>>()
            val cut = roomSettingsViewModel(coroutineContext, onBackMock)

            cut.leaveRoom()
            testCoroutineScheduler.advanceUntilIdle()

            verifySuspend {
                roomsApiClientMock.leaveRoom(eq(roomId), any(), eqNull())
                onBackMock()
            }

            cancelNeverEndingCoroutines()
        }

        should("show an error message when trying to leave a room and we are not connected") {
            every {
                userServiceMock.getAccountData(eq(PushRulesEventContent::class), any())
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
            every {
                userServiceMock.getAccountData(eq(PushRulesEventContent::class), any())
            } returns MutableStateFlow(null)
            everySuspend {
                roomsApiClientMock.leaveRoom(
                    eq(roomId),
                    any(),
                    eqNull()
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
            every {
                userServiceMock.getAccountData(eq(PushRulesEventContent::class), any())
            } returns MutableStateFlow(null)

            every { userServiceMock.canInvite(roomId) } returns
                    MutableStateFlow(false)
            val cut = roomSettingsViewModel(coroutineContext)

            delay(50)
            cut.hasPowerToInvite.first() shouldBe false

            cancelNeverEndingCoroutines()
        }


        should("allow to invite users") {
            every {
                userServiceMock.getAccountData(eq(PushRulesEventContent::class), any())
            } returns MutableStateFlow(null)

            every { userServiceMock.canInvite(roomId) } returns
                    MutableStateFlow(true)
            val cut = roomSettingsViewModel(coroutineContext)
            cut.hasPowerToInvite.first { it } shouldBe true

            cancelNeverEndingCoroutines()
        }
    }

    private suspend fun roomSettingsViewModel(
        coroutineContext: CoroutineContext,
        onBackMock: () -> Unit = mock<Function0<Unit>>(),
    ): RoomSettingsViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        return RoomSettingsViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(me to matrixClientMock)) + module {
                            single<MemberListViewModelFactory> {
                                object : MemberListViewModelFactory {
                                    override fun create(
                                        viewModelContext: MatrixClientViewModelContext,
                                        selectedRoomId: RoomId,
                                        error: MutableStateFlow<String?>
                                    ): MemberListViewModel = object : MemberListViewModel {
                                        override val elements: StateFlow<List<MemberListElementViewModel>> =
                                            MutableStateFlow(listOf())
                                        override val membershipCounts: StateFlow<Map<Membership, Int>> =
                                            MutableStateFlow(emptyMap())
                                        override val showLoadingSpinner: StateFlow<Boolean> = MutableStateFlow(false)
                                        override val error: StateFlow<String?> = MutableStateFlow(null)

                                    }
                                }
                            }
                        },
                    )
                }.koin,
                userId = me,
                coroutineContext = coroutineContext,
            ),
            selectedRoomId = roomId,
            onBack = onBackMock,
            onCloseRoomSettings = mock(),
            onOpenAvatarCutter = { _, _, _ -> },
            onShowAddMembers = mock(),
            onShowExportRoom = mock(),
        )
    }
}
