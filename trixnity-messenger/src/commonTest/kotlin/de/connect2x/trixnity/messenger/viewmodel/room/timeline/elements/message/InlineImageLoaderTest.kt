package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media.MediaService
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class InlineImageLoaderTest {
    private val matrixClient = mock<MatrixClient>()
    private val mediaService = mock<MediaService>()
    private val uri = "mxc://server/image"
    private val maxSize = MatrixMessengerConfiguration().downloadLimits.thumbnail

    @Test
    fun `load server thumbnail without downloading original`() = runTest {
        val bytes = "thumbnail".encodeToByteArray()
        everySuspend { mediaService.getThumbnail(uri, 800L, 800L, maxSize, maxSize, any(), progress = any()) } returns
            Result.success(InMemoryPlatformMedia(bytes))

        withViewModel { it.loadInlineImage(uri) shouldBe bytes }
    }

    @Test
    fun `fall back to original with download limit when thumbnail fails`() = runTest {
        val bytes = "original".encodeToByteArray()
        everySuspend { mediaService.getThumbnail(uri, 800L, 800L, maxSize, maxSize, any(), progress = any()) } returns
            Result.failure(IllegalStateException("thumbnail unavailable"))
        everySuspend { mediaService.getMedia(uri, maxSize, maxSize, any()) } returns
            Result.success(InMemoryPlatformMedia(bytes))

        withViewModel { it.loadInlineImage(uri) shouldBe bytes }
    }

    @Test
    fun `reject non Matrix media sources`() = runTest {
        withViewModel { it.loadInlineImage("https://server/image") shouldBe null }
    }

    private suspend fun TestScope.withViewModel(block: suspend (InlineImageLoader) -> Unit) {
        val clientDi = koinApplication { modules(module { single { mediaService } }) }
        every { matrixClient.di } returns clientDi.koin
        val userId = UserId("test", "server")
        val di = koinApplication { modules(createTestDefaultTrixnityMessengerModules(mapOf(userId to matrixClient))) }
        try {
            block(
                TextRoomMessageTimelineElementViewModelImpl(
                    testMatrixClientViewModelContext(di = di.koin, userId = userId),
                    RoomMessageEventContent.TextBased.Text("test"),
                    RoomId("!room:server"),
                    { _, _ -> },
                )
            )
        } finally {
            di.close()
            clientDi.close()
        }
    }
}
