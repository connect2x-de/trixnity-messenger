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
import de.connect2x.trixnity.core.model.events.RoomEventContent
import de.connect2x.trixnity.core.model.events.m.PushRulesEventContent
import de.connect2x.trixnity.core.model.events.m.room.NameEventContent
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
import kotlin.reflect.KClass
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class RoomSettingsNameViewModelTest {
    private val roomId = RoomId("!room")
    private val me = UserId("user", "localhost")

    private val matrixClientMock = mock<MatrixClient>()
    private val roomServiceMock = mock<RoomService>()
    private val userServiceMock = mock<UserService>()
    private val matrixClientServerApiMock = mock<MatrixClientServerApiClient>()
    private val roomsApiClientMock = mock<RoomApiClient>()

    private var canSendEventMocker: BlockingAnsweringScope<Flow<Boolean>>
    private var roomGetById: BlockingAnsweringScope<Flow<Room?>>

    init {
        resetMocks(matrixClientMock, roomServiceMock, userServiceMock, matrixClientServerApiMock, roomsApiClientMock)
        every { matrixClientMock.di } returns
            koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                            single { userServiceMock }
                        }
                    )
                }
                .koin
        every { matrixClientMock.userId } returns me
        every { matrixClientMock.api } returns matrixClientServerApiMock
        every { matrixClientServerApiMock.room } returns roomsApiClientMock

        roomGetById = every { roomServiceMock.getById(roomId) }
        roomGetById returns MutableStateFlow(room(""))

        canSendEventMocker = every { userServiceMock.canSendEvent(any(), any<KClass<out RoomEventContent>>()) }
        canSendEventMocker returns flowOf(true)
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `load permissions to change the room name based on the user's power level`() = runTest {
        every {
            // mockmp requires us to mock the user service within each test.
            userServiceMock.getAccountData(PushRulesEventContent::class, any())
        } returns MutableStateFlow(null)
        val canSendEvent = MutableStateFlow(true)
        canSendEventMocker returns canSendEvent

        val viewModel = roomSettingsNameViewModel()
        backgroundScope.launch { viewModel.canChangeRoomName.collect() }
        eventually(2.seconds) { viewModel.canChangeRoomName.value shouldBe true }

        canSendEvent.value = false
        eventually(2.seconds) { viewModel.canChangeRoomName.value shouldBe false }
    }

    @Test
    fun `load the room name`() = runTest {
        every {
            // mockmp requires us to mock the user service within each test.
            userServiceMock.getAccountData(PushRulesEventContent::class, any())
        } returns MutableStateFlow(null)
        roomGetById returns MutableStateFlow<Room?>(room("room name"))
        val viewModel = roomSettingsNameViewModel()
        eventually(2.seconds) { viewModel.roomName.textValue shouldBe "room name" }
    }

    @Test
    fun `edit and apply room name change`() = runTest {
        every {
            // mockmp requires us to mock the user service within each test.
            userServiceMock.getAccountData(PushRulesEventContent::class, any())
        } returns MutableStateFlow(null)
        val homeServerHandle = mockSendToHomeServer(NameEventContent("edited name"))
        backgroundScope.launch { homeServerHandle.numCallsToHomeServer.collect() }
        roomGetById returns MutableStateFlow<Room?>(room("current name"))

        val viewModel = roomSettingsNameViewModel()
        viewModel.roomName.isLoading.first { it.not() }
        viewModel.roomName.startEdit()
        viewModel.roomName.update("edited name")
        viewModel.roomName.approveEdit()
        homeServerHandle.numCallsToHomeServer.first { it == 1 }
    }

    private fun room(name: String) =
        Room(roomId, name = RoomDisplayName(explicitName = name, summary = null), isDirect = false)

    private fun mockSendToHomeServer(expectedRequestContent: NameEventContent): MockHomeServerHandle {
        val handle = MockHomeServerHandle()
        everySuspend { roomsApiClientMock.sendStateEvent(roomId, expectedRequestContent, any()) } calls
            {
                handle.numCallsToHomeServer.value += 1
                Result.success(EventId("1"))
            }
        return handle
    }

    data class MockHomeServerHandle(val numCallsToHomeServer: MutableStateFlow<Int> = MutableStateFlow(0))

    private fun TestScope.roomSettingsNameViewModel(): RoomSettingsNameViewModelImpl {
        return RoomSettingsNameViewModelImpl(
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
        )
    }
}
