package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
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
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.Unknown
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class OutboxElementViewModelTest : ShouldSpec() {

    val matrixClientMock = mock<MatrixClient>()

    val downloadManagerMock = mock<DownloadManager>()

    val clock = mock<Clock>()

    private val roomId = RoomId("room1", "localhost")

    init {
        Dispatchers.setMain(Dispatchers.Unconfined)
        beforeTest {
            resetMocks(matrixClientMock, downloadManagerMock, clock)
            every { clock.now() } returns Instant.parse("2020-10-01T01:00:00.000Z")
        }

        should("result in a TextMessageViewModel for a text message") {
            val cut = outboxElementViewModel(
                RoomOutboxMessage(
                    transactionId = "1",
                    roomId = RoomId(""),
                    content = TextBased.Text(body = "Hello World"),
                    createdAt = Instant.fromEpochMilliseconds(0)
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
                    content = FileBased.Image(body = "", url = "mxc://localhost/123456"),
                    createdAt = Instant.fromEpochMilliseconds(0)
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
                    content = FileBased.Video(body = "", url = "mxc://localhost/123456"),
                    createdAt = Instant.fromEpochMilliseconds(0)
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
                    content = FileBased.File(body = "", url = "mxc://localhost/123456"),
                    createdAt = Instant.fromEpochMilliseconds(0)
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
                    content = Unknown("m.dino", "I love unicorns.", JsonObject(mapOf())),
                    createdAt = Instant.fromEpochMilliseconds(0)
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
            onOpenModal = mock(),
            onOpenMention = mock(),
        )
    }
}
