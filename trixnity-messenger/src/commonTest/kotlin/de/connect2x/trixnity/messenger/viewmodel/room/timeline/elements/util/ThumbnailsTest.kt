package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media.MediaService
import de.connect2x.trixnity.clientserverapi.model.media.FileTransferProgress
import de.connect2x.trixnity.core.model.events.m.room.EncryptedFile
import de.connect2x.trixnity.core.model.events.m.room.ImageInfo
import de.connect2x.trixnity.core.model.events.m.room.ThumbnailInfo
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
import de.connect2x.trixnity.utils.toByteArrayFlow
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class ThumbnailsTest {

    val matrixClientMock = mock<MatrixClient>()

    val mediaServiceMock: MediaService = mock()

    val jwk: EncryptedFile.JWK = EncryptedFile.JWK("bla")

    init {
        resetMocks(matrixClientMock, mediaServiceMock)

        every { matrixClientMock.di } returns koinApplication { modules(module { single { mediaServiceMock } }) }.koin
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `missing thumbnail total uses thumbnail event size`(): TestResult {
        return checkDownloadProgress(true, null, 40)
    }

    @Test
    fun `thumbnail network total takes precedence over event size`(): TestResult {
        return checkDownloadProgress(true, 80, 80)
    }

    @Test
    fun `missing original total after thumbnail failure uses original event size`(): TestResult {
        return checkDownloadProgress(false, null, 100)
    }

    @Test
    fun `original network total takes precedence over event size`(): TestResult {
        return checkDownloadProgress(false, 80, 80)
    }

    @Test
    fun `request custom thumbnail dimensions for thumbnail and original URLs`() = runTest {
        val uri = "mxc://host.local/image"
        everySuspend { mediaServiceMock.getThumbnail(uri, 800L, 600L, any(), any(), any(), onProgress = any()) } returns
            Result.success(InMemoryPlatformMedia("thumbnail".encodeToByteArray().toByteArrayFlow()))
        val cut: Thumbnails = ThumbnailsImpl()

        for (useThumbnailUrl in listOf(true, false)) {
            val result =
                cut.loadThumbnail(
                    backgroundScope,
                    matrixClientMock,
                    thumbnailFile = null,
                    thumbnailUrl = if (useThumbnailUrl) uri else null,
                    thumbnailInfo = null,
                    file = null,
                    fileUrl = if (useThumbnailUrl) null else uri,
                    fileInfo = null,
                    sizeInBytes = 1_000,
                    onProgress = {},
                    maxMediaSizeInMemory = MatrixMessengerConfiguration().downloadLimits.thumbnail,
                    maxDownloadSize = MatrixMessengerConfiguration().downloadLimits.thumbnail,
                    width = 800L,
                    height = 600L,
                )

            result shouldBe "thumbnail".encodeToByteArray()
        }
    }

    @Test
    fun `keep default thumbnail dimensions for existing callers`() = runTest {
        val uri = "mxc://host.local/image"
        everySuspend { mediaServiceMock.getThumbnail(uri, 400L, 300L, any(), any(), any(), onProgress = any()) } returns
            Result.success(InMemoryPlatformMedia("thumbnail".encodeToByteArray().toByteArrayFlow()))

        val result =
            ThumbnailsImpl()
                .loadThumbnail(
                    backgroundScope,
                    matrixClientMock,
                    thumbnailFile = null,
                    thumbnailUrl = null,
                    thumbnailInfo = null,
                    file = null,
                    fileUrl = uri,
                    fileInfo = null,
                    sizeInBytes = 1_000,
                    onProgress = {},
                    maxMediaSizeInMemory = MatrixMessengerConfiguration().downloadLimits.thumbnail,
                    maxDownloadSize = MatrixMessengerConfiguration().downloadLimits.thumbnail,
                )

        result shouldBe "thumbnail".encodeToByteArray()
    }

    @Test
    fun `load encrypted thumbnail file successfully`() = runTest {
        val thumbnailFile = EncryptedFile("http://host.local/media/123456", jwk, "", mapOf())
        everySuspend {
            mediaServiceMock.getEncryptedMedia(thumbnailFile, any(), any(), onProgress = any(), saveToCache = any())
        } returns Result.success(InMemoryPlatformMedia("encryptedThumbnail".encodeToByteArray().toByteArrayFlow()))
        val cut = ThumbnailsImpl()

        val result =
            cut.loadThumbnail(
                backgroundScope,
                matrixClientMock,
                thumbnailFile = thumbnailFile,
                thumbnailUrl = null,
                thumbnailInfo = null,
                file = null,
                fileUrl = null,
                fileInfo = null,
                sizeInBytes = 1_000,
                onProgress = {},
                maxMediaSizeInMemory = MatrixMessengerConfiguration().downloadLimits.thumbnail,
                maxDownloadSize = MatrixMessengerConfiguration().downloadLimits.thumbnail,
            )

        result shouldBe "encryptedThumbnail".encodeToByteArray()
    }

    @Test
    fun `get the original file less than 1MB when the the encrypted thumbnail could not be loaded`() = runTest {
        val thumbnailFile = EncryptedFile("http://host.local/media/123456", jwk, "", mapOf())
        everySuspend {
            mediaServiceMock.getEncryptedMedia(thumbnailFile, any(), any(), onProgress = any(), saveToCache = any())
        } returns Result.failure(RuntimeException("Oh no!"))
        val originalFile = EncryptedFile("http://host.local/media/abcdef", jwk, "", mapOf())
        var progress: FileTransferProgress? = FileTransferProgress(10, 40)
        everySuspend {
            mediaServiceMock.getEncryptedMedia(originalFile, any(), any(), onProgress = any(), saveToCache = any())
        } calls
            { args ->
                progress shouldBe FileTransferProgress(0, null)
                args.arg<(FileTransferProgress) -> Unit>(3)(FileTransferProgress(25, 100))
                Result.success(InMemoryPlatformMedia("encryptedOriginal".encodeToByteArray().toByteArrayFlow()))
            }

        val cut = ThumbnailsImpl()

        val result =
            cut.loadThumbnail(
                backgroundScope,
                matrixClientMock,
                thumbnailFile = thumbnailFile,
                thumbnailUrl = null,
                thumbnailInfo = null,
                file = originalFile,
                fileUrl = null,
                fileInfo = null,
                sizeInBytes = 1_000,
                onProgress = { progress = it },
                maxMediaSizeInMemory = MatrixMessengerConfiguration().downloadLimits.thumbnail,
                maxDownloadSize = MatrixMessengerConfiguration().downloadLimits.thumbnail,
            )

        result shouldBe "encryptedOriginal".encodeToByteArray()
        progress shouldBe FileTransferProgress(25, 100)
    }

    @Test
    fun `get no thumbnail when neither the encrypted thumbnail nor the original file could be loaded`() = runTest {
        val thumbnailFile = EncryptedFile("http://host.local/media/123456", jwk, "", mapOf())
        everySuspend {
            mediaServiceMock.getEncryptedMedia(thumbnailFile, any(), any(), onProgress = any(), saveToCache = any())
        } returns Result.failure(RuntimeException("Oh no!"))
        val originalFile = EncryptedFile("http://host.local/media/abcdef", jwk, "", mapOf())
        everySuspend {
            mediaServiceMock.getEncryptedMedia(originalFile, any(), any(), onProgress = any(), saveToCache = any())
        } returns Result.failure(RuntimeException("Oh no!"))

        val cut = ThumbnailsImpl()

        val result =
            cut.loadThumbnail(
                backgroundScope,
                matrixClientMock,
                thumbnailFile = thumbnailFile,
                thumbnailUrl = null,
                thumbnailInfo = null,
                file = originalFile,
                fileUrl = null,
                fileInfo = null,
                sizeInBytes = 1_000,
                onProgress = {},
                maxMediaSizeInMemory = MatrixMessengerConfiguration().downloadLimits.thumbnail,
                maxDownloadSize = MatrixMessengerConfiguration().downloadLimits.thumbnail,
            )

        result shouldBe null
    }

    @Test
    fun `get no thumbnail when the encrypted thumbnail could not be loaded and the original file is larger than the maximum preview size`() =
        runTest {
            val thumbnailFile = EncryptedFile("http://host.local/media/123456", jwk, "", mapOf())
            everySuspend {
                mediaServiceMock.getEncryptedMedia(thumbnailFile, any(), any(), onProgress = any(), saveToCache = any())
            } returns Result.failure(RuntimeException("Oh no!"))
            val originalFile = EncryptedFile("http://host.local/media/abcdef", jwk, "", mapOf())
            everySuspend {
                mediaServiceMock.getEncryptedMedia(originalFile, any(), any(), onProgress = any(), saveToCache = any())
            } returns Result.success(InMemoryPlatformMedia("encryptedOriginal".encodeToByteArray().toByteArrayFlow()))

            val cut = ThumbnailsImpl()
            val result =
                cut.loadThumbnail(
                    backgroundScope,
                    matrixClientMock,
                    thumbnailFile = thumbnailFile,
                    thumbnailUrl = null,
                    thumbnailInfo = null,
                    file = originalFile,
                    fileUrl = null,
                    fileInfo = null,
                    sizeInBytes = MatrixMessengerConfiguration().downloadLimits.thumbnail + 1, // too large!
                    onProgress = {},
                    MatrixMessengerConfiguration().downloadLimits.thumbnail,
                    maxDownloadSize = MatrixMessengerConfiguration().downloadLimits.thumbnail,
                )

            result shouldBe null
        }

    @Test
    fun `suspend when loading the encrypted thumbnail takes a while`() = runTest {
        val thumbnailFile = EncryptedFile("http://host.local/media/123456", jwk, "", mapOf())
        everySuspend {
            mediaServiceMock.getEncryptedMedia(thumbnailFile, any(), any(), onProgress = any(), saveToCache = any())
        } calls
            {
                delay(500)
                Result.success(InMemoryPlatformMedia("encryptedThumbnail".encodeToByteArray().toByteArrayFlow()))
            }

        val cut = ThumbnailsImpl()

        val result = backgroundScope.async {
            cut.loadThumbnail(
                backgroundScope,
                matrixClientMock,
                thumbnailFile = thumbnailFile,
                thumbnailUrl = null,
                thumbnailInfo = null,
                file = null,
                fileUrl = null,
                fileInfo = null,
                sizeInBytes = 1_000,
                onProgress = {},
                MatrixMessengerConfiguration().downloadLimits.thumbnail,
                MatrixMessengerConfiguration().downloadLimits.thumbnail,
            )
        }
        delay(250)
        result.isActive shouldBe true
        delay(300)
        result.isActive shouldBe false
        result.isCompleted shouldBe true
        result.await() shouldBe "encryptedThumbnail".encodeToByteArray()
    }

    private fun checkDownloadProgress(useThumbnail: Boolean, httpTotal: Long?, expectedTotal: Long): TestResult {
        return runTest {
            val thumbnailFile = EncryptedFile("mxc://server/thumbnail", jwk, "", mapOf())
            val originalFile = EncryptedFile("mxc://server/original", jwk, "", mapOf())
            if (!useThumbnail) {
                everySuspend {
                    mediaServiceMock.getEncryptedMedia(
                        thumbnailFile,
                        any(),
                        any(),
                        onProgress = any(),
                        saveToCache = any(),
                    )
                } returns Result.failure(IllegalStateException("thumbnail unavailable"))
            }
            val requestedFile = if (useThumbnail) thumbnailFile else originalFile
            everySuspend {
                mediaServiceMock.getEncryptedMedia(requestedFile, any(), any(), onProgress = any(), saveToCache = any())
            } calls
                { args ->
                    args.arg<(FileTransferProgress) -> Unit>(3)(FileTransferProgress(25, httpTotal))
                    Result.success(InMemoryPlatformMedia("image".encodeToByteArray().toByteArrayFlow()))
                }
            var progress: FileTransferProgress? = null
            val result =
                ThumbnailsImpl()
                    .loadThumbnail(
                        backgroundScope,
                        matrixClientMock,
                        thumbnailFile = thumbnailFile,
                        thumbnailUrl = null,
                        thumbnailInfo = ThumbnailInfo(size = 40),
                        file = originalFile,
                        fileUrl = null,
                        fileInfo = ImageInfo(size = 100),
                        sizeInBytes = 100,
                        onProgress = { progress = it },
                        maxMediaSizeInMemory = 1_000,
                        maxDownloadSize = 1_000,
                    )
            progress shouldBe FileTransferProgress(25, expectedTotal)
            result shouldBe "image".encodeToByteArray()
        }
    }
}
