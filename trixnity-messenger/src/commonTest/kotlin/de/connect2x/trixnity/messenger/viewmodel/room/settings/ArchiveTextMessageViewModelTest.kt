package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.timeline
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.waitForSize
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldInclude
import io.kotest.matchers.string.shouldNotInclude
import korlibs.io.util.Indenter.Companion.single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveTextMessageViewModelTest : ShouldSpec() {

    private val roomId = RoomId("room1", "localhost")
    private val alice = UserId("alice", "localhost")
    private val ourUserId = UserId("bob", "localhost")
    private val roomName = "DemoTestRoom"
    val eventId = EventId("0")

    val messageEvent = ClientEvent.RoomEvent.MessageEvent(
        content = RoomMessageEventContent.TextBased.Text("Hello"),
        id = eventId,
        sender = alice,
        roomId = roomId,
        originTimestamp = 0L,
    )

    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var matrixClientServerApiClientMock: MatrixClientServerApiClient

    @Mock
    lateinit var roomsApiClientMock: RoomApiClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var userServiceMock: UserService

    init {
        beforeTest {
            Dispatchers.setMain(Dispatchers.Unconfined)
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
                every { matrixClientMock.userId } returns ourUserId
                every { matrixClientMock.api } returns matrixClientServerApiClientMock
                every { matrixClientServerApiClientMock.room } returns roomsApiClientMock
                every { userServiceMock.getById(isAny(), isAny()) } returns
                        MutableStateFlow(null)

                every { roomServiceMock.getById(isAny()) } returns MutableStateFlow(
                    Room(isDirect = true, roomId = roomId, lastEventId = EventId("lastEventId"))
                )
                every {
                    roomServiceMock.getTimelineEvents(isAny(), isAny(), isAny(), isAny())
                        .onCompletion { emptyList<String>() }
                } returns flowOf(
                    flowOf(
                        TimelineEvent(
                            event = messageEvent,
                            content = Result.success(RoomMessageEventContent.TextBased.Text("Hello")),
                            previousEventId = null,
                            nextEventId = null,
                            gap = null,
                        )
                    )
                )
            }
        }


        should("specifiedMessageLimit validate the input and through error if it contains non digit value ") {
            val cut = archiveTestMessageViewModel()
            val thresholdType = ThresholdType.SpecifyNumberOfMessages
            cut.archiveRoomThreshold.value = ArchiveOptions.RoomThreshold(thresholdType)
            cut.specifiedMessageLimit.value = "Abc"
            cut.archiveRoom()
            cut.archiveRoomState.value shouldBe ArchiveRoomState.Error("Bitte geben Sie eine gültige Zahl für die angegebenen Nachrichten ein, die größer als 0 sein sollte.")
        }

        should("PlainText formatType file should contain .txt extension") {
            val cut = archiveTestMessageViewModel()
            val selectedFormat = FormatType.PlainText
            cut.archiveFormat.value = ArchiveOptions.Format(selectedFormat)
            cut.fileName.value shouldInclude (".txt")
            cut.fileName.value shouldNotInclude (".csv")
        }


        should("CSV formatType file should contain .csv extension") {
            val cut = archiveTestMessageViewModel()
            val selectedFormat = FormatType.CSV
            cut.archiveFormat.value = ArchiveOptions.Format(selectedFormat)
            cut.fileName.value shouldInclude (".csv")
            cut.fileName.value shouldNotInclude (".txt")
        }

        should("Return success once archive is success") {
            val cut = archiveTestMessageViewModel()
            val selectedFormat = FormatType.PlainText
            cut.archiveFormat.value = ArchiveOptions.Format(selectedFormat)
            cut.archiveRoom()
            cut.fileName.value shouldInclude (".txt")
            cut.archiveRoomState.value shouldBe ArchiveRoomState.Success
        }
    }


    private fun archiveTestMessageViewModel(): ArchiveTextMessageViewModel {
        return ArchiveTextMessageViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(
                                UserId(
                                    "test",
                                    "server"
                                ) to matrixClientMock
                            )
                        ),
                    )
                    module {
                        single<ArchiveRoomResultHandler> {
                            object : ArchiveRoomHandlerBase() {
                                override val onProcessArchiveResult: MutableSharedFlow<Pair<String, String>> =
                                    MutableStateFlow(Pair("test.txt", "fileCotent"))
                            }
                        }
                    }

                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = Dispatchers.Unconfined,
            ),
            selectedRoomId = roomId,
            roomName = roomName,
            onArchiveMessageDialogDismiss = {}

        )

    }
}
