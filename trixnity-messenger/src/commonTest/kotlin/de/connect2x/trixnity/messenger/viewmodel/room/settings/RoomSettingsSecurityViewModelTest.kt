package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.continually
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomDisplayName
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.StateEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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

    init {
        resetMocks(
            matrixClientMock, roomServiceMock, userServiceMock, matrixClientServerApiClientMock, roomsApiClientMock
        )
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { userServiceMock }
                    single { roomServiceMock }
                })
        }.koin
        canSendEventMock = every { userServiceMock.canSendEvent(roomId, EncryptionEventContent::class) }
        roomGetById = every { roomServiceMock.getById(roomId) }
        every { matrixClientMock.userId } returns me
        every { matrixClientMock.api } returns matrixClientServerApiClientMock
        every { matrixClientServerApiClientMock.room } returns roomsApiClientMock
    }

    @Test
    fun `enable encryption in unencrypted room while be able to`() = runTest {
        canSendEventMock returns flowOf(true)
        roomGetById returns MutableStateFlow(room("room", false))
        val encryptionEventCounter = MutableStateFlow(0)
        mockSendStateEvent(EncryptionEventContent(), encryptionEventCounter)

        val error: MutableStateFlow<String?> = MutableStateFlow(null)
        val cut = roomSettingsSecurityViewModel(error)
        eventually(2.seconds) {
            cut.canEnableEncryption.value shouldBe true
        }
        cut.enableEncryption()
        eventually(2.seconds) {
            encryptionEventCounter.value shouldBe 1
        }
    }

    @Test
    fun `enable encryption in encrypted room while be able to`() = runTest {
        canSendEventMock returns flowOf(false)
        roomGetById returns MutableStateFlow(room("room", true))
        val encryptionEventCounter = MutableStateFlow(0)
        mockSendStateEvent(EncryptionEventContent(), encryptionEventCounter)

        val cut = roomSettingsSecurityViewModel(MutableStateFlow(null))
        continually(200.milliseconds) {
            cut.canEnableEncryption.value shouldBe false
        }
        cut.enableEncryption()
        continually(200.milliseconds) {
            encryptionEventCounter.value shouldBe 0
        }
    }

    private fun mockSendStateEvent(expectedEvent: StateEventContent, callCounter: MutableStateFlow<Int>) {
        everySuspend {
            roomsApiClientMock.sendStateEvent(roomId, expectedEvent, any(), any())
        } calls {
            callCounter.value += 1
            Result.success(EventId("1"))
        }
    }

    private fun room(name: String, encrypted: Boolean) = Room(
        roomId, name = RoomDisplayName(explicitName = name, summary = null), isDirect = false, encrypted = encrypted
    )

    private fun TestScope.roomSettingsSecurityViewModel(
        error: MutableStateFlow<String?>
    ): RoomSettingsSecurityViewModelImpl {
        return RoomSettingsSecurityViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("test", "server") to matrixClientMock)
                        ),
                    )
                }.koin, userId = UserId("test", "server")
            ), selectedRoomId = roomId, error = error
        )
    }

}
