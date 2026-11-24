package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.RoomDisplayName
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.RoomApiClient
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import de.connect2x.trixnity.core.model.events.StateEventContent
import de.connect2x.trixnity.core.model.events.m.room.EncryptionEventContent
import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class RoomSettingsSecurityViewModelTest {
    private val roomId = RoomId("!room")
    private val me = UserId("user1", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val userServiceMock = mock<UserService>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    var roomsApiClientMock = mock<RoomApiClient>()

    private var canSendEventMock: BlockingAnsweringScope<Flow<Boolean>>
    private var roomGetById: BlockingAnsweringScope<Flow<Room?>>

    private fun joinRulesEvent(joinRulesEventContent: JoinRulesEventContent) =
        StateEvent(
            joinRulesEventContent,
            id = EventId("An awesome EventId"),
            sender = me,
            originTimestamp = 122,
            roomId = roomId,
            stateKey = "",
        )

    init {
        resetMocks(
            matrixClientMock,
            roomServiceMock,
            userServiceMock,
            matrixClientServerApiClientMock,
            roomsApiClientMock,
        )
        every { matrixClientMock.di } returns
            koinApplication {
                    modules(
                        module {
                            single { userServiceMock }
                            single { roomServiceMock }
                        }
                    )
                }
                .koin
        canSendEventMock = every { userServiceMock.canSendEvent(roomId, EncryptionEventContent::class) }
        roomGetById = every { roomServiceMock.getById(roomId) }
        every { matrixClientMock.userId } returns me
        every { matrixClientMock.api } returns matrixClientServerApiClientMock
        every { matrixClientServerApiClientMock.room } returns roomsApiClientMock
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `can enable encryption if can send encryption event content and room not encrypted and joinRule not public`() =
        runTest {
            canSendEventMock returns flowOf(true)
            roomGetById returns MutableStateFlow(room("room", false))
            val encryptionEventCounter = MutableStateFlow(0)
            mockSendStateEvent(EncryptionEventContent(), encryptionEventCounter)

            every {
                roomServiceMock.getState(
                    roomId = roomId,
                    eventContentClass = JoinRulesEventContent::class,
                    stateKey = "",
                )
            } returns (flowOf(joinRulesEvent(JoinRulesEventContent(JoinRulesEventContent.JoinRule.Private))))

            val error: MutableStateFlow<String?> = MutableStateFlow(null)
            val cut = roomSettingsSecurityViewModel(error)
            eventually(2.seconds) { cut.canEnableEncryption.value shouldBe true }
            cut.enableEncryption()
            eventually(2.seconds) { encryptionEventCounter.value shouldBe 1 }
        }

    @Test
    fun `cannot enable encryption if cannot send encryption event content`() = runTest {
        canSendEventMock returns flowOf(false)
        roomGetById returns MutableStateFlow(room("room", false))
        val encryptionEventCounter = MutableStateFlow(0)
        mockSendStateEvent(EncryptionEventContent(), encryptionEventCounter)

        every {
            roomServiceMock.getState(roomId = roomId, eventContentClass = JoinRulesEventContent::class, stateKey = "")
        } returns flowOf(joinRulesEvent(JoinRulesEventContent(JoinRulesEventContent.JoinRule.Private)))

        val error: MutableStateFlow<String?> = MutableStateFlow(null)
        val cut = roomSettingsSecurityViewModel(error)

        eventually(2.seconds) { cut.canEnableEncryption.value shouldBe false }
        encryptionEventCounter.value shouldBe 0
    }

    @Test
    fun `cannot enable encryption if room is already encrypted`() = runTest {
        canSendEventMock returns flowOf(true)
        roomGetById returns MutableStateFlow(room("room", true))
        val encryptionEventCounter = MutableStateFlow(0)
        mockSendStateEvent(EncryptionEventContent(), encryptionEventCounter)

        every {
            roomServiceMock.getState(roomId = roomId, eventContentClass = JoinRulesEventContent::class, stateKey = "")
        } returns flowOf(joinRulesEvent(JoinRulesEventContent(JoinRulesEventContent.JoinRule.Private)))

        val error: MutableStateFlow<String?> = MutableStateFlow(null)
        val cut = roomSettingsSecurityViewModel(error)

        eventually(2.seconds) { cut.canEnableEncryption.value shouldBe false }
        encryptionEventCounter.value shouldBe 0
    }

    @Test
    fun `can enable encryption if joinRule state event is null`() = runTest {
        canSendEventMock returns flowOf(true)
        roomGetById returns MutableStateFlow(room("room", false))
        val encryptionEventCounter = MutableStateFlow(0)
        mockSendStateEvent(EncryptionEventContent(), encryptionEventCounter)

        every {
            roomServiceMock.getState(roomId = roomId, eventContentClass = JoinRulesEventContent::class, stateKey = "")
        } returns flowOf(null)

        val error: MutableStateFlow<String?> = MutableStateFlow(null)
        val cut = roomSettingsSecurityViewModel(error)

        eventually(2.seconds) { cut.canEnableEncryption.value shouldBe true }
        cut.enableEncryption()
        eventually(2.seconds) { encryptionEventCounter.value shouldBe 1 }
    }

    @Test
    fun `cannot enable encryption if joinRule is public`() = runTest {
        canSendEventMock returns flowOf(true)
        roomGetById returns MutableStateFlow(room("room", false))
        val encryptionEventCounter = MutableStateFlow(0)
        mockSendStateEvent(EncryptionEventContent(), encryptionEventCounter)

        every {
            roomServiceMock.getState(roomId = roomId, eventContentClass = JoinRulesEventContent::class, stateKey = "")
        } returns flowOf(joinRulesEvent(JoinRulesEventContent(JoinRulesEventContent.JoinRule.Public)))

        val error: MutableStateFlow<String?> = MutableStateFlow(null)
        val cut = roomSettingsSecurityViewModel(error)

        eventually(2.seconds) { cut.canEnableEncryption.value shouldBe false }
        encryptionEventCounter.value shouldBe 0
    }

    private fun mockSendStateEvent(expectedEvent: StateEventContent, callCounter: MutableStateFlow<Int>) {
        everySuspend { roomsApiClientMock.sendStateEvent(roomId, expectedEvent, any()) } calls
            {
                callCounter.value += 1
                Result.success(EventId("1"))
            }
    }

    private fun room(name: String, encrypted: Boolean) =
        Room(
            roomId,
            name = RoomDisplayName(explicitName = name, summary = null),
            isDirect = false,
            encrypted = encrypted,
        )

    private fun TestScope.roomSettingsSecurityViewModel(
        error: MutableStateFlow<String?>
    ): RoomSettingsSecurityViewModelImpl {
        return RoomSettingsSecurityViewModelImpl(
            viewModelContext =
                testMatrixClientViewModelContext(
                    di =
                        koinApplication {
                                modules(
                                    createTestDefaultTrixnityMessengerModules(
                                        mapOf(UserId("test", "server") to matrixClientMock)
                                    )
                                )
                            }
                            .koin,
                    userId = UserId("test", "server"),
                ),
            selectedRoomId = roomId,
            error = error,
        )
    }
}
