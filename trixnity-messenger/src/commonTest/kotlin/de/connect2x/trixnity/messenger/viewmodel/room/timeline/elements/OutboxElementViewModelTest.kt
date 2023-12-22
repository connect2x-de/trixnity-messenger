package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.files.DownloadManager
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonObject
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.store.RoomOutboxMessage
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.*
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction4
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class OutboxElementViewModelTest : ShouldSpec() {

    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var downloadManagerMock: DownloadManager

    @Mock
    lateinit var clock: Clock

    private val roomId = RoomId("room1", "localhost")

    init {
        Dispatchers.setMain(testMainDispatcher)
        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            mocker.every { clock.now() } returns Instant.parse("2020-10-01T01:00:00.000Z")
        }

        should("result in a TextMessageViewModel for a text message") {
            val cut = outboxElementViewModel(
                RoomOutboxMessage(
                    transactionId = "1",
                    roomId = RoomId(""),
                    content = TextBased.Text(body = "Hello World")
                )
            )

            val value = cut.timelineElementViewModel.filterNotNull().first()
            value.shouldBeInstanceOf<TextMessageViewModel>()
            value.isByMe shouldBe true
            value.showSender.first { !it }
        }

        should("result in an ImageMessageViewModel for a image message") {
            val cut = outboxElementViewModel(
                RoomOutboxMessage(
                    transactionId = "1",
                    roomId = RoomId(""),
                    content = FileBased.Image(body = "", url = "mxc://localhost/123456")
                )
            )

            val value = cut.timelineElementViewModel.filterNotNull().first()
            value.shouldBeInstanceOf<ImageMessageViewModel>()
            value.isByMe shouldBe true
            value.showSender.first { !it }
        }

        should("result in a VideoMessageViewModel for a video message") {
            val cut = outboxElementViewModel(
                RoomOutboxMessage(
                    transactionId = "1",
                    roomId = RoomId(""),
                    content = FileBased.Video(body = "", url = "mxc://localhost/123456")
                )
            )

            val value = cut.timelineElementViewModel.filterNotNull().first()
            value.shouldBeInstanceOf<VideoMessageViewModel>()
            value.isByMe shouldBe true
            value.showSender.first { !it }
        }

        should("result in a FileMessageViewModel for a file message") {
            val cut = outboxElementViewModel(
                RoomOutboxMessage(
                    transactionId = "1",
                    roomId = RoomId(""),
                    content = FileBased.File(body = "", url = "mxc://localhost/123456")
                )
            )

            val value = cut.timelineElementViewModel.filterNotNull().first()
            value.shouldBeInstanceOf<FileMessageViewModel>()
            value.isByMe shouldBe true
            value.showSender.first { !it }
        }

        should("not display any other message type ('== null')") {
            val cut = outboxElementViewModel(
                RoomOutboxMessage(
                    transactionId = "1",
                    roomId = RoomId(""),
                    content = Unknown("m.dino", "I love unicorns.", JsonObject(mapOf()))
                )
            )

            cut.timelineElementViewModel.filterNotNull().first().shouldBeInstanceOf<NullTimelineElementViewModel>()
        }
    }

    private fun outboxElementViewModel(outboxMessage: RoomOutboxMessage<*>): OutboxElementHolderViewModel {
        return OutboxElementHolderViewModelImpl(
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
                            single { downloadManagerMock }
                            single { clock }
                        })
                }.koin,
                userId = UserId("test", "server"),
            ),
            key = outboxMessage.transactionId,
            outboxMessageFlow = flowOf(outboxMessage),
            transactionId = outboxMessage.transactionId,
            showDateAboveFlow = flowOf(false),
            showChatBubbleEdgeFlow = flowOf(false),
            selectedRoomId = roomId,
            onOpenModal = mockFunction4(mocker),
        )
    }
}