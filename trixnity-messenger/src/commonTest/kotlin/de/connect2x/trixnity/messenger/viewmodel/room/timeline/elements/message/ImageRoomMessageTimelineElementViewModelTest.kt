package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ImageRoomMessageTimelineElementViewModelTest {
    val matrixClientMock = mock<MatrixClient>()

    val thumbnailsMock = mock<Thumbnails>()

    init {
        resetMocks(matrixClientMock, thumbnailsMock)
        every { thumbnailsMock.mapProgressToProgressElement(any()) } returns flowOf(null)
    }

    @Test
    fun `load a thumbnail successfully`() = runTest {
        everySuspend {
            thumbnailsMock.loadThumbnail(
                eq(matrixClientMock), any<RoomMessageEventContent.FileBased.Image>(), any(), any()
            )
        } returns "thumbnail".encodeToByteArray()

        val cut = imageMessageViewModel()
        backgroundScope.launch { cut.thumbnail.collect {} }

        eventually(2.seconds) {
            cut.thumbnail.value shouldBe "thumbnail".encodeToByteArray()
        }
    }

    @Test
    fun `load a thumbnail that takes a while to load`() = runTest {
        everySuspend {
            thumbnailsMock.loadThumbnail(
                eq(matrixClientMock), any<RoomMessageEventContent.FileBased.Image>(), any(), any()
            )
        } calls {
            delay(500.milliseconds)
            "thumbnail".encodeToByteArray()
        }

        val cut = imageMessageViewModel()
        backgroundScope.launch { cut.thumbnail.collect {} }

        cut.thumbnail.value shouldBe null

        delay(499.milliseconds)
        cut.thumbnail.value shouldBe null

        delay(1.milliseconds)
        cut.thumbnail.value shouldBe "thumbnail".encodeToByteArray()
    }

    @Test
    fun `return 'null' for a thumbnail that cannot be loaded`() = runTest {
        everySuspend {
            thumbnailsMock.loadThumbnail(
                eq(matrixClientMock), any<RoomMessageEventContent.FileBased.Image>(), any(), any()
            )
        } returns null

        val cut = imageMessageViewModel()
        backgroundScope.launch { cut.thumbnail.collect {} }

        eventually(1.seconds) {
            cut.thumbnail.value shouldBe null
        }
    }

    private fun TestScope.imageMessageViewModel(): ImageRoomMessageTimelineElementViewModelImpl {
        return ImageRoomMessageTimelineElementViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("test", "server") to matrixClientMock)
                        ) + module {
                            single { thumbnailsMock }
                        })
                }.koin,
                userId = UserId("test", "server"),
            ),
            content = RoomMessageEventContent.FileBased.Image(""),
        )
    }
}
