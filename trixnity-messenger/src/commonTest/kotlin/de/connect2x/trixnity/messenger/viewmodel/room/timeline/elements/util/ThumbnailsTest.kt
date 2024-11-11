package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.resetMocks
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.MediaService
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.utils.toByteArrayFlow
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class ThumbnailsTest : ShouldSpec() {

    val matrixClientMock = mock<MatrixClient>()

    val mediaServiceMock: MediaService = mock()

    val jwk: EncryptedFile.JWK = EncryptedFile.JWK("bla")

    init {
        Dispatchers.setMain(Dispatchers.Unconfined)
        beforeTest {
            resetMocks(matrixClientMock, mediaServiceMock)

            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { mediaServiceMock }
                    }
                )
            }.koin
        }

        should("load encrypted thumbnail file successfully") {
            val thumbnailFile = EncryptedFile("http://host.local/media/123456", jwk, "", mapOf())
            everySuspend {
                mediaServiceMock.getEncryptedMedia(
                    eq(thumbnailFile),
                    any(),
                    any()
                )
            } returns
                    Result.success("encryptedThumbnail".encodeToByteArray().toByteArrayFlow())
            val cut = ThumbnailsImpl()

            val result = cut.loadThumbnail(
                matrixClientMock,
                thumbnailFile = thumbnailFile,
                thumbnailUrl = null,
                file = null,
                url = null,
                sizeInBytes = 1_000,
                thumbnailProgressFlow = MutableStateFlow(null),
            )

            result shouldBe "encryptedThumbnail".encodeToByteArray()
        }

        should("get the original file (<1MB) when the the encrypted thumbnail could not be loaded") {
            val thumbnailFile = EncryptedFile("http://host.local/media/123456", jwk, "", mapOf())
            everySuspend {
                mediaServiceMock.getEncryptedMedia(
                    eq(thumbnailFile),
                    any(),
                    any()
                )
            } returns
                    Result.failure(RuntimeException("Oh no!"))
            val originalFile = EncryptedFile("http://host.local/media/abcdef", jwk, "", mapOf())
            everySuspend {
                mediaServiceMock.getEncryptedMedia(
                    eq(originalFile),
                    any(),
                    any()
                )
            } returns
                    Result.success("encryptedOriginal".encodeToByteArray().toByteArrayFlow())

            val cut = ThumbnailsImpl()
            val result = cut.loadThumbnail(
                matrixClientMock,
                thumbnailFile = thumbnailFile,
                thumbnailUrl = null,
                file = originalFile,
                url = null,
                sizeInBytes = 1_000,
                thumbnailProgressFlow = MutableStateFlow(null),
            )

            result shouldBe "encryptedOriginal".encodeToByteArray()
        }

        should("get no thumbnail when neither the encrypted thumbnail nor the original file could be loaded") {
            val thumbnailFile = EncryptedFile("http://host.local/media/123456", jwk, "", mapOf())
            everySuspend {
                mediaServiceMock.getEncryptedMedia(
                    eq(thumbnailFile),
                    any(),
                    any()
                )
            } returns
                    Result.failure(RuntimeException("Oh no!"))
            val originalFile = EncryptedFile("http://host.local/media/abcdef", jwk, "", mapOf())
            everySuspend {
                mediaServiceMock.getEncryptedMedia(
                    eq(originalFile),
                    any(),
                    any()
                )
            } returns
                    Result.failure(RuntimeException("Oh no!"))

            val cut = ThumbnailsImpl()
            val result = cut.loadThumbnail(
                matrixClientMock,
                thumbnailFile = thumbnailFile,
                thumbnailUrl = null,
                file = originalFile,
                url = null,
                sizeInBytes = 1_000,
                thumbnailProgressFlow = MutableStateFlow(null),
            )

            result shouldBe null
        }

        should("get no thumbnail when the encrypted thumbnail could not be loaded and the original file is larger than the maximum preview size") {
            val thumbnailFile = EncryptedFile("http://host.local/media/123456", jwk, "", mapOf())
            everySuspend {
                mediaServiceMock.getEncryptedMedia(
                    eq(thumbnailFile),
                    any(),
                    any()
                )
            } returns
                    Result.failure(RuntimeException("Oh no!"))
            val originalFile = EncryptedFile("http://host.local/media/abcdef", jwk, "", mapOf())
            everySuspend {
                mediaServiceMock.getEncryptedMedia(
                    eq(originalFile),
                    any(),
                    any()
                )
            } returns
                    Result.success("encryptedOriginal".encodeToByteArray().toByteArrayFlow())

            val maxPreviewSize = MatrixMessengerConfiguration().filePreviewMaxSize
            val cut = ThumbnailsImpl()
            val result = cut.loadThumbnail(
                matrixClientMock,
                thumbnailFile = thumbnailFile,
                thumbnailUrl = null,
                file = originalFile,
                url = null,
                sizeInBytes = maxPreviewSize + 1, // too large!
                thumbnailProgressFlow = MutableStateFlow(null),
            )

            result shouldBe null
        }

        should("suspend when loading the encrypted thumbnail takes a while") {
            val thumbnailFile = EncryptedFile("http://host.local/media/123456", jwk, "", mapOf())
            everySuspend {
                mediaServiceMock.getEncryptedMedia(
                    eq(thumbnailFile),
                    any(),
                    any()
                )
            } calls {
                delay(500)
                Result.success("encryptedThumbnail".encodeToByteArray().toByteArrayFlow())
            }

            val scope = CoroutineScope(Dispatchers.Default)

            val cut = ThumbnailsImpl()
            val result = scope.async {
                cut.loadThumbnail(
                    matrixClientMock,
                    thumbnailFile = thumbnailFile,
                    thumbnailUrl = null,
                    file = null,
                    url = null,
                    sizeInBytes = 1_000,
                    thumbnailProgressFlow = MutableStateFlow(null),
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
}
