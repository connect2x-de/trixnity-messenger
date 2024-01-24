package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomSettingsNameViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 4_000

    val mocker = Mocker()

    private val roomId = RoomId("room", "localhost")
    private val me = UserId("user1", "localhost")

    private val createEventContent = CreateEventContent(creator = me)

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
                        isDirect = true,
                        roomId = roomId,
                        name = RoomDisplayName(explicitName = "Old name", summary = null),
                    )
                )

                canSendEventMocker = mocker.every {
                    userServiceMock.canSendEvent(isAny(), isAny())
                }
                canSendEventMocker returns flowOf(true)
            }
        }

        should("allow to change to room's name when the user's power level is allowed to") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(null)

            val canSendEvent = MutableStateFlow(true)
            canSendEventMocker returns canSendEvent

            val cut = roomSettingsNameViewModel(coroutineContext, MutableStateFlow(null))
            val canChangeRoomNameStateFlow = cut.canChangeRoomName // hold reference for WhileSubscribed
            canChangeRoomNameStateFlow.first { it }

            canSendEvent.value = false

            canChangeRoomNameStateFlow.first { it.not() }

            cancelNeverEndingCoroutines()
        }

        should("load the room name, set it when loaded, and can be manipulated afterwards") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(null)

            val roomStateFlow = MutableStateFlow<Room?>(null)
            roomGetById returns roomStateFlow

            val cut = roomSettingsNameViewModel(coroutineContext, MutableStateFlow(null))
            // subscribe to all values in order to check for correct values later
            CoroutineScope(Dispatchers.Default).launch {
                cut.roomNameLoading.collect()
            }
            CoroutineScope(Dispatchers.Default).launch {
                cut.roomName.collect()
            }

            cut.roomNameLoading.value shouldBe true
            cut.roomName.value shouldBe ""

            roomStateFlow.value = Room(
                roomId,
                name = RoomDisplayName(explicitName = "Old name", summary = null)
            )
            testCoroutineScheduler.advanceUntilIdle()
            cut.roomNameLoading.value shouldBe false
            cut.roomName.value shouldBe "Old name"

            cancelNeverEndingCoroutines()
        }

        should("set the room's name to `Undetermined` when the name is currently set") {
            val coroutineScope = CoroutineScope(Dispatchers.Default)
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(null)
            mocker.everySuspending {
                roomsApiClientMock.sendStateEvent(isEqual(roomId), isAny(), isAny(), isAny())
            } runs {
                coroutineScope.async {
                    delay(1.seconds)
                    Result.success(EventId("1"))
                }.await()
            }

            val cut = roomSettingsNameViewModel(coroutineContext, MutableStateFlow(null))
            // subscribe to all values in order to check for correct values later
            coroutineScope.launch { cut.roomNameLoading.collect() }
            coroutineScope.launch { cut.roomNameIsBeingEdited.collect() }
            coroutineScope.launch { cut.canChangeRoomName.collect() }

            testCoroutineScheduler.advanceUntilIdle()
            cut.roomName.value shouldBe "Old name"

            cut.roomName.value = "New name"
            testCoroutineScheduler.advanceUntilIdle()
            cut.roomNameIsBeingEdited.value shouldBe true
            cut.roomNameLoading.value shouldBe false

            cut.changeRoomName()
            testCoroutineScheduler.advanceUntilIdle()
            cut.roomNameLoading.value shouldBe true

            coroutineScope.launch {
                eventually(2.seconds) {
                    cut.roomName.value shouldBe "New name"
                    cut.roomNameLoading.value shouldBe false
                }
            }.join()

            cancelNeverEndingCoroutines()
        }
    }

    private suspend fun roomSettingsNameViewModel(
        coroutineContext: CoroutineContext,
        error: MutableStateFlow<String?>,
    ): RoomSettingsNameViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        return RoomSettingsNameViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock)),
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext,
            ),
            selectedRoomId = roomId,
            error = error,
        )
    }
}