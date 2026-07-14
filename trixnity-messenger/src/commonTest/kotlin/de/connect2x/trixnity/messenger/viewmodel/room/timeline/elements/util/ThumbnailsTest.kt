package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media.MediaService
import de.connect2x.trixnity.core.model.events.m.room.EncryptedFile
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
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun `load encrypted thumbnail file successfully`() = runTest {
        val thumbnailFile = EncryptedFile("http://host.local/media/123456", jwk, "", mapOf())
        everySuspend { mediaServiceMock.getEncryptedMedia(thumbnailFile, any(), any(), any()) } returns
            Result.success(InMemoryPlatformMedia("encryptedThumbnail".encodeToByteArray().toByteArrayFlow()))
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
                thumbnailProgressFlow = MutableStateFlow(null),
                maxMediaSizeInMemory = MatrixMessengerConfiguration().downloadLimits.thumbnail,
            )

        result shouldBe "encryptedThumbnail".encodeToByteArray()
    }

    @Test
    fun `get the original file less than 1MB when the the encrypted thumbnail could not be loaded`() = runTest {
        val thumbnailFile = EncryptedFile("http://host.local/media/123456", jwk, "", mapOf())
        everySuspend { mediaServiceMock.getEncryptedMedia(thumbnailFile, any(), any(), any()) } returns
            Result.failure(RuntimeException("Oh no!"))
        val originalFile = EncryptedFile("http://host.local/media/abcdef", jwk, "", mapOf())
        everySuspend { mediaServiceMock.getEncryptedMedia(originalFile, any(), any(), any()) } returns
            Result.success(InMemoryPlatformMedia("encryptedOriginal".encodeToByteArray().toByteArrayFlow()))

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
                thumbnailProgressFlow = MutableStateFlow(null),
                maxMediaSizeInMemory = MatrixMessengerConfiguration().downloadLimits.thumbnail,
            )

        result shouldBe "encryptedOriginal".encodeToByteArray()
    }

    @Test
    fun `get no thumbnail when neither the encrypted thumbnail nor the original file could be loaded`() = runTest {
        val thumbnailFile = EncryptedFile("http://host.local/media/123456", jwk, "", mapOf())
        everySuspend { mediaServiceMock.getEncryptedMedia(thumbnailFile, any(), any(), any()) } returns
            Result.failure(RuntimeException("Oh no!"))
        val originalFile = EncryptedFile("http://host.local/media/abcdef", jwk, "", mapOf())
        everySuspend { mediaServiceMock.getEncryptedMedia(originalFile, any(), any(), any()) } returns
            Result.failure(RuntimeException("Oh no!"))

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
                thumbnailProgressFlow = MutableStateFlow(null),
                maxMediaSizeInMemory = MatrixMessengerConfiguration().downloadLimits.thumbnail,
            )

        result shouldBe null
    }

    @Test
    fun `get no thumbnail when the encrypted thumbnail could not be loaded and the original file is larger than the maximum preview size`() =
        runTest {
            val thumbnailFile = EncryptedFile("http://host.local/media/123456", jwk, "", mapOf())
            everySuspend { mediaServiceMock.getEncryptedMedia(thumbnailFile, any(), any(), any()) } returns
                Result.failure(RuntimeException("Oh no!"))
            val originalFile = EncryptedFile("http://host.local/media/abcdef", jwk, "", mapOf())
            everySuspend { mediaServiceMock.getEncryptedMedia(originalFile, any(), any(), any()) } returns
                Result.success(InMemoryPlatformMedia("encryptedOriginal".encodeToByteArray().toByteArrayFlow()))

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
                    thumbnailProgressFlow = MutableStateFlow(null),
                    MatrixMessengerConfiguration().downloadLimits.thumbnail,
                )

            result shouldBe null
        }

    @Test
    fun `suspend when loading the encrypted thumbnail takes a while`() = runTest {
        val thumbnailFile = EncryptedFile("http://host.local/media/123456", jwk, "", mapOf())
        everySuspend { mediaServiceMock.getEncryptedMedia(thumbnailFile, any(), any(), any()) } calls
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
                thumbnailProgressFlow = MutableStateFlow(null),
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
}
