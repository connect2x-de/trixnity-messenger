package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.room.archive.ArchiveSink
import de.connect2x.trixnity.messenger.viewmodel.room.archive.ArchiveSinkConfig
import de.connect2x.trixnity.messenger.viewmodel.room.archive.ArchiveSinkState
import de.connect2x.trixnity.messenger.viewmodel.room.archive.ArchiveSinkViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.archive.ArchiveSinkViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.room.archive.GetPlainTextArchiveSinkConfig
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.util.createTestMatrixMessengerSettingsHolder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction1
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveSinkViewModelTest : ShouldSpec() {

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

    @Mock
    lateinit var archiveSinkFactoryMock: ArchiveSink


    private val processArchive = mockFunction1<Unit, ArchiveSinkState>(mocker)


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
                every { processArchive.invoke(isAny()) } returns Unit

            }
        }

        should("Return correct number of supported formats") {
            val cut = archiveTestMessageViewModel()
            cut.supportedFormats.size shouldBe 2
        }

        should("Correct format should return sinkState success") {
            val cut = archiveTestMessageViewModel()

            cut.selectedSinkFormat.value = GetPlainTextArchiveSinkConfig()
            cut.selectedSinkFormat.value shouldBe GetPlainTextArchiveSinkConfig()

            cut.archiveRoom(cut.selectedSinkFormat.value)
            cut.archiveSinkState.value shouldBe ArchiveSinkState.Success

        }

        should("Wrong format throw illegal argument exception ") {
            val cut = archiveTestMessageViewModel()

            cut.selectedSinkFormat.value = GetPlainTextArchiveSinkConfig()
            cut.selectedSinkFormat.value shouldBe GetPlainTextArchiveSinkConfig()

            shouldThrow<IllegalArgumentException> {
                cut.archiveRoom(FakeConfig())
            }
        }
    }


    data class FakeConfig(val name: String = "Fake") : ArchiveSinkConfig

    private suspend fun archiveTestMessageViewModel(): ArchiveSinkViewModel {
        val di = koinApplication {
            modules(createTestDefaultTrixnityMessengerModules())
        }.koin

        di.get<I18n>().setCurrentLang(DefaultLanguages.EN)

        return ArchiveSinkViewModelImpl(
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
                        ) + module {
//                            single<ArchiveSinkFactory> {
//                                object : ArchiveSinkFactory {
//                                    override val supportedFormats: List<Pair<String, ArchiveSinkConfig>>
//                                        get() = listOf(
//                                            Pair("CSV", GetCSVArchiveSinkConfig()),
//                                            Pair("Plain/Text", GetPlainTextArchiveSinkConfig())
//                                        )
//
//                                    override fun create(
//                                        roomId: RoomId,
//                                        matrixClient: MatrixClient,
//                                        viewModelContext: ViewModelContext,
//                                        sinkConfig: ArchiveSinkConfig
//                                    ): ArchiveSink {
//                                        return when (sinkConfig) {
//                                            is GetCSVArchiveSinkConfig -> PlainTexArchiveSink(
//                                                i18n, matrixClient, roomId, sinkConfig
//                                            )
//
//                                            is GetPlainTextArchiveSinkConfig -> CSVArchiveSink(
//                                                i18n, matrixClient, roomId, sinkConfig
//                                            )
//
//                                            else -> throw IllegalArgumentException("Unsupported sinkConfig: $sinkConfig")
//                                        }
//                                    }
//                                }
//                            }
                        }
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
