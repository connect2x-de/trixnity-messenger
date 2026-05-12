package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId.Companion.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.core.model.events.m.room.ThumbnailInfo
import de.connect2x.trixnity.core.model.events.m.room.VideoInfo
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class VideoRoomMessageTimelineElementViewModelTest {
    val matrixClientMock = mock<MatrixClient>()
    val thumbnailsMock = mock<Thumbnails>()

    val roomId = RoomId("!bathroom:server")
    val meUserId = UserId("tester", "server")

    init {
        resetMocks(matrixClientMock, thumbnailsMock)
        every { thumbnailsMock.mapProgressToProgressElement(any()) } returns flowOf(null)
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `load a thumbnail successfully`() = runTest {
        everySuspend {
            thumbnailsMock.loadThumbnail(
                any(), matrixClientMock, any<RoomMessageEventContent.FileBased.Video>(), any(), any()
            )
        } returns "thumbnail".encodeToByteArray()

        val cut = videoRoomMessageTimelineElementViewModel()
        backgroundScope.launch { cut.thumbnail.collect {} }

        delay(100.milliseconds)
        cut.thumbnail.value shouldBe "thumbnail".encodeToByteArray()
    }

    @Test
    fun `load a thumbnail that takes a while to load`() = runTest {
        everySuspend {
            thumbnailsMock.loadThumbnail(
                any(), matrixClientMock, any<RoomMessageEventContent.FileBased.Video>(), any(), any()
            )
        } calls {
            delay(500.milliseconds)
            "thumbnail".encodeToByteArray()
        }

        val cut = videoRoomMessageTimelineElementViewModel()
        backgroundScope.launch { cut.thumbnail.collect {} }

        cut.thumbnail.value shouldBe null

        delay(499.milliseconds)
        cut.thumbnail.value shouldBe null

        delay(2.milliseconds)
        cut.thumbnail.value shouldBe "thumbnail".encodeToByteArray()
    }

    @Test
    fun `return 'null' for a thumbnail that cannot be loaded`() = runTest {
        everySuspend {
            thumbnailsMock.loadThumbnail(
                any(), matrixClientMock, any<RoomMessageEventContent.FileBased.Video>(), any(), any()
            )
        } returns null

        val cut = videoRoomMessageTimelineElementViewModel()
        backgroundScope.launch { cut.thumbnail.collect {} }

        delay(100.milliseconds)
        cut.thumbnail.value shouldBe null
    }

    fun TestScope.videoRoomMessageTimelineElementViewModel(): RoomMessageTimelineElementViewModel.FileBased.Video {
        return VideoRoomMessageTimelineElementViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(meUserId to matrixClientMock)
                        ) + module {
                            single { thumbnailsMock }
                        }
                    )
                }.koin,
                userId = meUserId,
            ),
            content = RoomMessageEventContent.FileBased.Video(
                "",
                info = VideoInfo(
                    duration = 100L,
                    height = 500,
                    width = 500,
                    thumbnailInfo = ThumbnailInfo(
                        height = 100,
                        width = 100,
                    )
                )
            ),
            roomId = roomId,
            eventIdOrTransactionId = EventIdOrTransactionId(EventId("\$videomessage")),
            onOpenMention = { _, _ -> }
        )
    }
}
