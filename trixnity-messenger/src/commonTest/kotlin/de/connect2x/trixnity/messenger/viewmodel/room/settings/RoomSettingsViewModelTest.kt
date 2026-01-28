package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.user.PowerLevel
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.RoomApiClient
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import de.connect2x.trixnity.core.model.events.m.PushRulesEventContent
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.PowerLevelsEventContent
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test

class RoomSettingsViewModelTest {
    private val roomId = RoomId("!room")
    private val me = UserId("user1", "localhost")

    private val roomUserMe = RoomUser(
        roomId, me, "User1", StateEvent(
            MemberEventContent(membership = Membership.JOIN), EventId(""), me, roomId, 0L, stateKey = ""
        )
    )

    private val powerLevelsEventContent = PowerLevelsEventContent(users = mapOf(me to 100))
    private val createEventContent = CreateEventContent()

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

    private val syncStateMocker: BlockingAnsweringScope<StateFlow<SyncState>>

    init {
        resetMocks(
            matrixClientMock, roomServiceMock, userServiceMock, matrixClientServerApiMock, roomsApiClientMock
        )
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                })
        }.koin
        syncStateMocker = every { matrixClientMock.syncState }
        syncStateMocker returns MutableStateFlow(SyncState.STARTED)
        every { matrixClientMock.api } returns matrixClientServerApiMock
        every { matrixClientMock.userId } returns me

        every { matrixClientServerApiMock.room } returns roomsApiClientMock

        every { roomServiceMock.getById(any()) } returns flowOf(Room(roomId))
        every {
            roomServiceMock.getState(
                roomId, PowerLevelsEventContent::class, ""
            )
        } returns MutableStateFlow(powerLevelEvent)
        every {
            roomServiceMock.getState(
                roomId, CreateEventContent::class, ""
            )
        } returns MutableStateFlow(createEvent)

        every {
            userServiceMock.getAll(roomId)
        } returns MutableStateFlow(
            mapOf(
                roomUserMe.userId to flowOf(roomUserMe),
            )
        )

        every { userServiceMock.canKickUser(roomId, any()) } returns MutableStateFlow(true)

        every { userServiceMock.canInvite(any()) } returns MutableStateFlow(true)

        every {
            userServiceMock.canSetPowerLevelToMax(
                roomId,
                any()
            )
        } returns MutableStateFlow(PowerLevel.User(100))
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `go back to the room list view when leaving the room successfully`() = runTest {
        every {
            userServiceMock.getAccountData(PushRulesEventContent::class, any())
        } returns MutableStateFlow(null)
        everySuspend {
            roomsApiClientMock.leaveRoom(
                roomId, any()
            )
        } returns Result.success(Unit)

        val cut = roomSettingsViewModel()

        cut.leaveRoom()
        yield()

        verifySuspend {
            roomsApiClientMock.leaveRoom(roomId, any())
        }
    }

    @Test
    fun `show an error message when trying to leave a room and we are not connected`() = runTest {
        every {
            userServiceMock.getAccountData(PushRulesEventContent::class, any())
        } returns MutableStateFlow(null)
        syncStateMocker returns MutableStateFlow(SyncState.ERROR)

        val cut = roomSettingsViewModel()
        cut.leaveRoom()
        yield()

        cut.error.value shouldNotBe null
    }

    @Test
    fun `show an error message when leaving the room fails`() = runTest {
        every {
            userServiceMock.getAccountData(PushRulesEventContent::class, any())
        } returns MutableStateFlow(null)
        everySuspend {
            roomsApiClientMock.leaveRoom(
                roomId, any(),
            )
        } returns Result.failure(RuntimeException("Oh no!"))

        val cut = roomSettingsViewModel()
        cut.leaveRoom()
        yield()

        cut.error.value shouldNotBe null
    }

    @Test
    fun `not allow to invite users`() = runTest {
        every {
            userServiceMock.getAccountData(PushRulesEventContent::class, any())
        } returns MutableStateFlow(null)

        every { userServiceMock.canInvite(roomId) } returns MutableStateFlow(false)
        val cut = roomSettingsViewModel()

        delay(50)
        cut.hasPowerToInvite.first() shouldBe false
    }


    @Test
    fun `allow to invite users`() = runTest {
        every {
            userServiceMock.getAccountData(PushRulesEventContent::class, any())
        } returns MutableStateFlow(null)

        every { userServiceMock.canInvite(roomId) } returns MutableStateFlow(true)
        val cut = roomSettingsViewModel()
        cut.hasPowerToInvite.first { it } shouldBe true
    }

    private fun TestScope.roomSettingsViewModel(
        onBackMock: () -> Unit = mock<Function0<Unit>>(),
    ): RoomSettingsViewModelImpl {
        return RoomSettingsViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(me to matrixClientMock)
                        ) + module {
                            single<MemberListViewModelFactory> {
                                object : MemberListViewModelFactory {
                                    override fun create(
                                        viewModelContext: MatrixClientViewModelContext,
                                        selectedRoomId: RoomId,
                                        error: MutableStateFlow<String?>,
                                        onOpenUserProfile: (UserId) -> Unit,
                                    ): MemberListViewModel = object : MemberListViewModel {
                                        override val filterByMemberships =
                                            MutableStateFlow(
                                                setOf(
                                                    Membership.JOIN,
                                                    Membership.BAN,
                                                    Membership.KNOCK,
                                                    Membership.INVITE
                                                )
                                            )
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
            ),
            selectedRoomId = roomId,
            onCloseRoom = onBackMock,
            onCloseRoomSettings = mock(),
            onOpenAvatarCutter = { _, _, _ -> },
            onOpenAddMembers = mock(),
            onOpenExportRoom = mock(),
            onOpenUserProfile = mock(),
            onOpenMention = { _, _ -> },
            onOpenPowerLevel = mock(),
        )
    }
}
