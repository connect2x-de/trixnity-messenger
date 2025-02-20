package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.withCleanup
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
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
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.PushRulesEventContent
import net.folivo.trixnity.core.model.events.m.room.TopicEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds


@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomSettingsTopicViewModelTest : ShouldSpec() {
    private val roomId = RoomId("room", "localhost")
    private val me = UserId("user", "localhost")

    private val matrixClientMock = mock<MatrixClient>()
    private val roomServiceMock = mock<RoomService>()
    private val userServiceMock = mock<UserService>()
    private val matrixClientServerApiMock = mock<MatrixClientServerApiClient>()
    private val roomsApiClientMock = mock<RoomApiClient>()

    private lateinit var canSendEventMocker: BlockingAnsweringScope<Flow<Boolean>>
    private lateinit var roomGetState: BlockingAnsweringScope<Flow<TopicEvent?>>
    private lateinit var roomGetById: BlockingAnsweringScope<Flow<Room?>>

    init {
        coroutineTestScope = true

        beforeTest {
            resetMocks(
                matrixClientMock,
                roomsApiClientMock,
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
            roomGetState = every { roomServiceMock.getState(roomId, TopicEventContent::class) }
            roomGetState returns MutableStateFlow(topicEvent("topic"))

            canSendEventMocker = every {
                userServiceMock.canSendEvent(any(), any())
            }
            canSendEventMocker returns flowOf(true)
        }

        should("load permissions to change the room topic based on the user's power level").withCleanup {
            every {
                // mockmp requires us to mock the user service within each test.
                userServiceMock.getAccountData(eq(PushRulesEventContent::class), any())
            } returns MutableStateFlow(null)
            val canSendEvent = MutableStateFlow(true)
            canSendEventMocker returns canSendEvent

            val viewModel = roomSettingsTopicViewModel(coroutineContext)
            launch { viewModel.canChangeRoomTopic.collect() }
            eventually(2.seconds) {
                viewModel.canChangeRoomTopic.value shouldBe true
            }

            canSendEvent.value = false
            eventually(2.seconds) {
                viewModel.canChangeRoomTopic.value shouldBe false
            }
        }

        should("load the room topic").withCleanup {
            every {
                // mockmp requires us to mock the user service within each test.
                userServiceMock.getAccountData(eq(PushRulesEventContent::class), any())
            } returns MutableStateFlow(null)
            roomGetState returns MutableStateFlow<TopicEvent?>(topicEvent("room topic"))
            val viewModel = roomSettingsTopicViewModel(coroutineContext)
            eventually(2.seconds) {
                viewModel.roomTopic.textValue shouldBe "room topic"
            }
        }

        should("edit and apply room topic change").withCleanup {
            every {
                // mockmp requires us to mock the user service within each test.
                userServiceMock.getAccountData(eq(PushRulesEventContent::class), any())
            } returns MutableStateFlow(null)
            val homeServerHandle = mockSendToHomeServer(TopicEventContent("edited topic"))
            launch { homeServerHandle.numCallsToHomeServer.collect() }
            roomGetState returns MutableStateFlow<TopicEvent?>(topicEvent("current topic"))

            val viewModel = roomSettingsTopicViewModel(coroutineContext)
            viewModel.roomTopic.isLoading.first { it.not() }
            viewModel.roomTopic.startEdit()
            viewModel.roomTopic.update("edited topic")
            viewModel.roomTopic.approveEdit()
            homeServerHandle.numCallsToHomeServer.first { it == 1 }
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

    private suspend fun mockSendToHomeServer(expectedRequestContent: TopicEventContent): MockHomeServerHandle {
        val handle = MockHomeServerHandle()
        everySuspend {
            roomsApiClientMock.sendStateEvent(eq(roomId), eq(expectedRequestContent), any(), any())
        } calls {
            handle.numCallsToHomeServer.value += 1
            Result.success(EventId("1"))
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
