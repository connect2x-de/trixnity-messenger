package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.Mentions
import de.connect2x.trixnity.core.model.events.m.StickerEventContent
import de.connect2x.trixnity.core.model.events.m.room.ImageInfo
import de.connect2x.trixnity.core.model.events.m.room.ThumbnailInfo
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class StickerTimelineElementViewModelTest {
    private val matrixClient = mock<MatrixClient>()
    private val thumbnails = mock<Thumbnails>()

    init {
        resetMocks(matrixClient, thumbnails)
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `load sticker image`() = runTest {
        val image = "sticker".encodeToByteArray()
        everySuspend {
            thumbnails.loadThumbnail(
                coroutineScope = any(),
                matrixClient = matrixClient,
                thumbnailFile = any(),
                thumbnailUrl = any(),
                thumbnailInfo = any(),
                file = any(),
                fileUrl = any(),
                fileInfo = any(),
                sizeInBytes = any(),
                thumbnailProgressFlow = any(),
                maxMediaSizeInMemory = any(),
                maxDownloadSize = any(),
            )
        } returns image

        val cut = stickerViewModel()
        cut.thumbnail.value shouldBe null
        backgroundScope.launch { cut.thumbnail.collect {} }
        runCurrent()

        cut.thumbnail.value shouldBe image
        cut.thumbnailLoading.value shouldBe false
    }

    @Test
    fun `expose sticker metadata when image cannot be loaded`() = runTest {
        var thumbnailRequested = false
        everySuspend {
            thumbnails.loadThumbnail(
                coroutineScope = any(),
                matrixClient = matrixClient,
                thumbnailFile = any(),
                thumbnailUrl = any(),
                thumbnailInfo = any(),
                file = any(),
                fileUrl = any(),
                fileInfo = any(),
                sizeInBytes = any(),
                thumbnailProgressFlow = any(),
                maxMediaSizeInMemory = any(),
                maxDownloadSize = any(),
            )
        } calls
            {
                thumbnailRequested = true
                null
            }

        val cut = stickerViewModel()
        cut.thumbnail.value shouldBe null
        backgroundScope.launch { cut.thumbnail.collect {} }
        runCurrent()

        thumbnailRequested shouldBe true
        cut.thumbnail.value shouldBe null
        cut.thumbnailLoading.value shouldBe false
        cut.name shouldBe "Party parrot.png"
        cut.mimeType shouldBe "image/png"
        cut.width shouldBe 512
        cut.height shouldBe 256
        cut.thumbnailWidth shouldBe 128
        cut.thumbnailHeight shouldBe 64
        cut.isMentioned shouldBe true
    }

    @Test
    fun `sticker downloads use the media extension`() {
        stickerFileName("Party parrot", "image/png") shouldBe "Party parrot.png"
        stickerFileName("Party.parrot", "image/webp") shouldBe "Party.parrot.webp"
        stickerFileName("Party.PNG", "image/png") shouldBe "Party.PNG"
        stickerFileName("Party", "image/gif; charset=binary") shouldBe "Party.gif"
        stickerFileName("../Party:parrot", "image/png") shouldBe ".._Party_parrot.png"
        stickerFileName("", "image/png") shouldBe "sticker.png"
        stickerFileName("Party", null) shouldBe "Party.bin"
        stickerFileName("Party", "invalid") shouldBe "Party.bin"
    }

    @Test
    fun `sticker filenames replace control characters and reserved characters`() {
        stickerFileName("Party\u0000\u001F\u007Fparrot", "image/png") shouldBe "Party___parrot.png"
        stickerFileName("Party\n\tparrot", "image/png") shouldBe "Party__parrot.png"
        stickerFileName("\\/:*?\"<>|", "image/png") shouldBe "_________.png"
        stickerFileName("Party 🦜", "image/png") shouldBe "Party 🦜.png"
    }

    private fun TestScope.stickerViewModel(): StickerTimelineElementViewModelImpl {
        val userId = UserId("test", "server")
        val di =
            koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(userId to matrixClient)) +
                            module { single<Thumbnails> { thumbnails } }
                    )
                }
                .koin

        return StickerTimelineElementViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(di = di, userId = userId),
            content =
                StickerEventContent(
                    url = "mxc://server/sticker",
                    body = "Party parrot",
                    info =
                        ImageInfo(
                            width = 512,
                            height = 256,
                            mimeType = "image/png",
                            thumbnailInfo = ThumbnailInfo(width = 128, height = 64),
                        ),
                    mentions = Mentions(users = setOf(userId)),
                ),
        )
    }
}
