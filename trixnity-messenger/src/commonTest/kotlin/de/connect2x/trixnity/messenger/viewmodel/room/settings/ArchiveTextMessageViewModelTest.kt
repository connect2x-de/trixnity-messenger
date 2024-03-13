package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.MatrixClientFactory
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.room.archive.CSVArchiveFormat
import de.connect2x.trixnity.messenger.viewmodel.room.archive.PlainTextFormat
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.timeline
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.waitForSize
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.util.createTestMatrixMessengerSettingsHolder
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

    lateinit var i18n: I18n


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

            i18n = object : I18n(DefaultLanguages, createTestMatrixMessengerSettingsHolder(), GetSystemLang { "en" }) {}


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


        should("PlainText formatType file should contain .txt extension") {
            val cut = archiveTestMessageViewModel()

            val selectedFormat = PlainTextFormat(i18n)
            cut.selectedSinkFormat.value = selectedFormat

            cut.selectedSinkFormat.value shouldBe selectedFormat
            cut.selectedSinkFormat.value.formatExtension shouldBe ".txt"
            cut.selectedSinkFormat.value.formatExtension shouldNotBe ".csv"
        }


        should("CSV formatType file should contain .csv extension") {
            val cut = archiveTestMessageViewModel()

            val selectedFormat = CSVArchiveFormat(i18n)
            cut.selectedSinkFormat.value = selectedFormat

            cut.supportedFormats.value.size  shouldBe 2
            cut.selectedSinkFormat.value shouldBe selectedFormat
            cut.selectedSinkFormat.value.formatExtension shouldNotBe ".txt"
            cut.selectedSinkFormat.value.formatExtension shouldBe ".csv"
        }


        should("Return success once archive is success") {
            val cut = archiveTestMessageViewModel()
            val selectedFormat = PlainTextFormat(i18n)
            cut.selectedSinkFormat.value = selectedFormat
            cut.archiveRoom()
            cut.selectedSinkFormat.value.formatExtension shouldInclude (".txt")
            cut.archiveRoomState.value shouldBe ArchiveRoomState.Success
        }
    }


    private suspend fun archiveTestMessageViewModel(): ArchiveTextMessageViewModel {
        val di = koinApplication {
            modules(createTestDefaultTrixnityMessengerModules())
        }.koin

        di.get<I18n>().setCurrentLang(DefaultLanguages.EN)

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
