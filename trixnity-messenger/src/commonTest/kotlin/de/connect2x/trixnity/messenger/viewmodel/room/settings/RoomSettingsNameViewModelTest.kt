package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
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
import net.folivo.trixnity.core.model.events.m.PushRulesEventContent
import net.folivo.trixnity.core.model.events.m.room.NameEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds


@OptIn(ExperimentalCoroutinesApi::class)
class RoomSettingsNameViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 4_000

    val mocker = Mocker()

    private val roomId = RoomId("room", "localhost")
    private val me = UserId("user", "localhost")

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var userServiceMock: UserService

    @Mock
    lateinit var matrixClientServerApiMock: MatrixClientServerApiClient

    @Mock
    lateinit var roomsApiClientMock: RoomApiClient

    private lateinit var canSendEventMocker: Mocker.Every<Flow<Boolean>>
    private lateinit var roomGetById: Mocker.Every<Flow<Room?>>

    init {
        beforeTest {
            mocker.reset()
            injectMocks(mocker)
            Dispatchers.setMain(Dispatchers.Unconfined)

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                            single { userServiceMock }
                        }
                    )
                }.koin
                every { matrixClientMock.userId } returns me
                every { matrixClientMock.api } returns matrixClientServerApiMock
                every { matrixClientServerApiMock.room } returns roomsApiClientMock

                roomGetById = every { roomServiceMock.getById(roomId) }
                roomGetById returns MutableStateFlow(room(""))

                canSendEventMocker = mocker.every {
                    userServiceMock.canSendEvent(isAny(), isAny())
                }
                canSendEventMocker returns flowOf(true)
            }
        }

        should("load permissions to change the room name based on the user's power level") {
            withTestingHarness {
                val canSendEvent = MutableStateFlow(true)
                canSendEventMocker returns canSendEvent

                val viewModel = roomSettingsNameViewModel(coroutineContext)
                launch { viewModel.canChangeRoomName.collect() }
                eventually(2.seconds) {
                    viewModel.canChangeRoomName.value shouldBe true
                }

                canSendEvent.value = false
                eventually(2.seconds) {
                    viewModel.canChangeRoomName.value shouldBe false
                }
            }
        }

        should("load the room name") {
            withTestingHarness {
                roomGetById returns MutableStateFlow<Room?>(room("room name"))
                val viewModel = roomSettingsNameViewModel(coroutineContext)
                eventually(2.seconds) {
                    viewModel.roomName.state.value.value shouldBe "room name"
                }
            }
        }

        should("edit and apply room name change") {
            withTestingHarness {
                val homeServerHandle = mockSendToHomeServer(NameEventContent("edited name"))
                launch { homeServerHandle.numCallsToHomeServer.collect() }
                roomGetById returns MutableStateFlow<Room?>(room("current name"))

                val viewModel = roomSettingsNameViewModel(coroutineContext)
                viewModel.roomName.isLoading.first { it.not() }
                viewModel.roomName.startEdit()
                viewModel.roomName.state.value.setEdit("edited name")
                viewModel.roomName.applyEdit()
                homeServerHandle.numCallsToHomeServer.first { it == 1 }
            }
        }
    }

    private fun room(name: String) =
        Room(roomId, name = RoomDisplayName(explicitName = name, summary = null), isDirect = false)

    private suspend fun TestScope.withTestingHarness(testFn: suspend TestScope.() -> Unit) {
        mocker.every {
            // mockmp requires us to mock the user service within each test.
            userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
        } returns MutableStateFlow(null)

        testFn(this)
        cancelNeverEndingCoroutines()
    }

    private suspend fun mockSendToHomeServer(expectedRequestContent: NameEventContent): MockHomeServerHandle {
        val handle = MockHomeServerHandle()
        mocker.everySuspending {
            roomsApiClientMock.sendStateEvent(isEqual(roomId), isEqual(expectedRequestContent), isAny(), isAny())
        } runs {
            println("----call")
            handle.numCallsToHomeServer.value += 1
            Result.success(EventId("1"))
        }
        return handle
    }

    data class MockHomeServerHandle(val numCallsToHomeServer: MutableStateFlow<Int> = MutableStateFlow(0))

    private fun roomSettingsNameViewModel(
        coroutineContext: CoroutineContext,
    ): RoomSettingsNameViewModelImpl {
        return RoomSettingsNameViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("test", "server") to matrixClientMock)
                        )
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext,
            ),
            selectedRoomId = roomId,
        )
    }
}
