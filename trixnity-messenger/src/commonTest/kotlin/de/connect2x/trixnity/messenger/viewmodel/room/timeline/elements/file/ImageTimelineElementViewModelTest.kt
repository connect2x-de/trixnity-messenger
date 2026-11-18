package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.clientserverapi.model.media.FileTransferProgress
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.EncryptedFile
import de.connect2x.trixnity.core.model.events.m.room.ImageInfo
import de.connect2x.trixnity.core.model.events.m.room.ThumbnailInfo
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.ThumbnailsImpl
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class ImageTimelineElementViewModelTest {
    private val thumbnailFile = EncryptedFile("mxc://localhost/thumbnail", EncryptedFile.JWK(""), "", mapOf())
    val matrixClientMock = mock<MatrixClient>()
    val thumbnailsMock = mock<Thumbnails>()

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
                coroutineScope = any(),
                matrixClient = matrixClientMock,
                thumbnailFile = any(),
                thumbnailUrl = any(),
                thumbnailInfo = any(),
                file = any(),
                fileUrl = any(),
                fileInfo = any(),
                sizeInBytes = any(),
                onProgress = any(),
                maxMediaSizeInMemory = any(),
                maxDownloadSize = any(),
            )
        } returns "thumbnail".encodeToByteArray()

        val cut = cut()
        backgroundScope.launch { cut.thumbnail.collect {} }

        delay(100.milliseconds)
        cut.thumbnail.value shouldBe "thumbnail".encodeToByteArray()
    }

    @Test
    fun `load a thumbnail that takes a while to load`() = runTest {
        everySuspend {
            thumbnailsMock.loadThumbnail(
                coroutineScope = any(),
                matrixClient = matrixClientMock,
                thumbnailFile = any(),
                thumbnailUrl = any(),
                thumbnailInfo = any(),
                file = any(),
                fileUrl = any(),
                fileInfo = any(),
                sizeInBytes = any(),
                onProgress = any(),
                maxMediaSizeInMemory = any(),
                maxDownloadSize = any(),
            )
        } calls
            {
                delay(500.milliseconds)
                "thumbnail".encodeToByteArray()
            }

        val cut = cut()
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
                coroutineScope = any(),
                matrixClient = matrixClientMock,
                thumbnailFile = any(),
                thumbnailUrl = any(),
                thumbnailInfo = any(),
                file = any(),
                fileUrl = any(),
                fileInfo = any(),
                sizeInBytes = any(),
                onProgress = any(),
                maxMediaSizeInMemory = any(),
                maxDownloadSize = any(),
            )
        } returns null

        val cut = cut()
        backgroundScope.launch { cut.thumbnail.collect {} }

        delay(100.milliseconds)
        cut.thumbnail.value shouldBe null
    }

    @Test
    fun `thumbnail progress uses HTTP total and clears after loading`(): TestResult {
        return checkThumbnailProgress(
            ImageInfo(thumbnailFile = thumbnailFile, thumbnailInfo = ThumbnailInfo(size = 40)),
            FileTransferProgress(25, 100),
            0.25f,
        )
    }

    @Test
    fun `download with unknown total remains visibly loading`(): TestResult {
        return checkThumbnailProgress(ImageInfo(), FileTransferProgress(25, null), null)
    }

    private fun TestScope.cut(
        info: ImageInfo = ImageInfo(height = 500, width = 500, thumbnailInfo = ThumbnailInfo(height = 100, width = 100))
    ): ImageTimelineElementViewModelImpl {
        return ImageTimelineElementViewModelImpl(
            viewModelContext =
                testMatrixClientViewModelContext(
                    di =
                        koinApplication {
                                modules(
                                    createTestDefaultTrixnityMessengerModules(
                                        mapOf(UserId("test", "server") to matrixClientMock)
                                    ) + module { single<Thumbnails> { thumbnailsMock } }
                                )
                            }
                            .koin,
                    userId = UserId("test", "server"),
                ),
            url = "mxc://localhost/unencrypted123456",
            encryptedFile = EncryptedFile(url = "mxc://localhost/123456", key = EncryptedFile.JWK(""), "", mapOf()),
            info = info,
            fileName = "test.png",
        )
    }

    private fun checkThumbnailProgress(info: ImageInfo, progress: FileTransferProgress, expected: Float?): TestResult {
        return runTest {
            val result = CompletableDeferred<ByteArray?>()
            every { thumbnailsMock.mapProgressToProgressElement(any()) } calls
                { args ->
                    ThumbnailsImpl().mapProgressToProgressElement(args.arg(0))
                }
            everySuspend {
                thumbnailsMock.loadThumbnail(
                    coroutineScope = any(),
                    matrixClient = matrixClientMock,
                    thumbnailFile = any(),
                    thumbnailUrl = any(),
                    thumbnailInfo = any(),
                    file = any(),
                    fileUrl = any(),
                    fileInfo = any(),
                    sizeInBytes = any(),
                    onProgress = any(),
                    maxMediaSizeInMemory = any(),
                    maxDownloadSize = any(),
                )
            } calls
                { args ->
                    args.arg<(FileTransferProgress?) -> Unit>(9)(progress)
                    result.await()
                }
            val cut = cut(info)
            backgroundScope.launch { cut.thumbnail.collect {} }
            delay(10)
            assertNotNull(cut.thumbnailLoadingProgress.value).percent shouldBe expected
            result.complete("thumbnail".encodeToByteArray())
            delay(10)
            cut.thumbnail.value shouldBe "thumbnail".encodeToByteArray()
            cut.thumbnailLoadingProgress.value shouldBe null
        }
    }
}
