package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.core.test.advanceUntilIdle
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomDisplayName
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.PushRulesEventContent
import net.folivo.trixnity.core.model.events.m.room.TopicEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext


@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomSettingsTopicViewModelTest : ShouldSpec() {
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
    private lateinit var roomGetState: Mocker.Every<Flow<TopicEvent?>>
    private lateinit var roomGetById: Mocker.Every<Flow<Room?>>

    init {
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
                every { matrixClientMock.userId } returns me
                every { matrixClientMock.api } returns matrixClientServerApiMock
                every { matrixClientServerApiMock.room } returns roomsApiClientMock

                roomGetById = every { roomServiceMock.getById(roomId) }
                roomGetById returns MutableStateFlow(
                    Room(
                        roomId,
                        name = RoomDisplayName(explicitName = "", summary = null),
                        isDirect = false,
                    )
                )
                roomGetState = every { roomServiceMock.getState<TopicEventContent>(roomId) }
                roomGetState returns MutableStateFlow(topicEvent("topic"))

                canSendEventMocker = mocker.every {
                    userServiceMock.canSendEvent(isAny(), isAny())
                }
                canSendEventMocker returns flowOf(true)
            }
        }

        should("load permissions to change the room topic based on the user's power level") {
            withTestingHarness {
                val coroutineScope = CoroutineScope(Dispatchers.Default)
                val canSendEvent = MutableStateFlow(true)
                canSendEventMocker returns canSendEvent

                val viewModel = roomSettingsTopicViewModel(coroutineContext)
                coroutineScope.launch { viewModel.canChangeRoomTopic.collect() }
                advanceUntilIdle()
                viewModel.canChangeRoomTopic.value shouldBe true

                canSendEvent.value = false
                coroutineScope.launch { viewModel.canChangeRoomTopic.collect() }
                advanceUntilIdle()
                viewModel.canChangeRoomTopic.value shouldBe false
            }
        }

        should("load the room topic") {
            withTestingHarness {
                roomGetState returns MutableStateFlow<TopicEvent?>(topicEvent("room topic"))
                val viewModel = roomSettingsTopicViewModel(coroutineContext)
                advanceUntilIdle()
                viewModel.roomTopic.state.value.value shouldBe "room topic"
            }
        }

        should("edit and apply room topic change") {
            withTestingHarness {
                val coroutineScope = CoroutineScope(Dispatchers.Default)
                val homeServerHandle = mockSendToHomeServer(TopicEventContent("edited topic"))
                roomGetState returns MutableStateFlow<TopicEvent?>(topicEvent("current topic"))

                val viewModel = roomSettingsTopicViewModel(coroutineContext)
                advanceUntilIdle()
                viewModel.roomTopic.startEdit()
                viewModel.roomTopic.state.value.setEdit("edited topic")
                viewModel.roomTopic.applyEdit()
                coroutineScope.launch { homeServerHandle.numCallsToHomeServer.collect() }
                advanceUntilIdle()
                homeServerHandle.numCallsToHomeServer.first { it == 1 }
            }
        }
    }

    private fun topicEvent(topic: String) = StateEvent(
        roomId = roomId,
        stateKey = "",
        id = EventId("1"),
        sender = me,
        content = TopicEventContent(topic),
        originTimestamp = 1,
    )

    private suspend fun TestScope.withTestingHarness(testFn: suspend TestScope.() -> Unit) {
        mocker.every {
            // mockmp requires us to mock the user service within each test.
            userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
        } returns MutableStateFlow(null)

        testFn(this)
        cancelNeverEndingCoroutines()
    }

    private suspend fun mockSendToHomeServer(expectedRequestContent: TopicEventContent): MockHomeServerHandle {
        val handle = MockHomeServerHandle()
        val coroutineScope = CoroutineScope(Dispatchers.Default)
        mocker.everySuspending {
            roomsApiClientMock.sendStateEvent(isEqual(roomId), isEqual(expectedRequestContent), isAny(), isAny())
        } runs {
            coroutineScope.async {
                handle.numCallsToHomeServer.value += 1
                Result.success(EventId("1"))
            }.await()
        }
        return handle
    }

    data class MockHomeServerHandle(val numCallsToHomeServer: MutableStateFlow<Int> = MutableStateFlow(0))

    private fun roomSettingsTopicViewModel(
        coroutineContext: CoroutineContext,
    ): RoomSettingsTopicViewModelImpl {
        Dispatchers.setMain(checkNotNull(coroutineContext[CoroutineDispatcher]))
        return RoomSettingsTopicViewModelImpl(
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

typealias TopicEvent = ClientEvent.StateBaseEvent<TopicEventContent>
