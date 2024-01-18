package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.messenger.viewmodel.mock.MediaServiceMock
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.MediaService
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.utils.toByteArrayFlow
import org.kodein.mock.Fake
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class ThumbnailsTest : ShouldSpec() {

    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    lateinit var mediaServiceMock: MediaService

    @Fake
    lateinit var jwk: EncryptedFile.JWK

    init {
        Dispatchers.setMain(Dispatchers.Unconfined)
        beforeTest {
            mocker.reset()
            injectMocks(mocker)
            mediaServiceMock = MediaServiceMock(mocker)

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { mediaServiceMock }
                        }
                    )
                }.koin
            }
        }

        should("load encrypted thumbnail file successfully") {
            val thumbnailFile = EncryptedFile("http://host.local/media/123456", jwk, "", mapOf())
            mocker.everySuspending {
                mediaServiceMock.getEncryptedMedia(
                    isEqual(thumbnailFile),
                    isAny(),
                    isAny()
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
            mocker.everySuspending {
                mediaServiceMock.getEncryptedMedia(
                    isEqual(thumbnailFile),
                    isAny(),
                    isAny()
                )
            } returns
                    Result.failure(RuntimeException("Oh no!"))
            val originalFile = EncryptedFile("http://host.local/media/abcdef", jwk, "", mapOf())
            mocker.everySuspending {
                mediaServiceMock.getEncryptedMedia(
                    isEqual(originalFile),
                    isAny(),
                    isAny()
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
            mocker.everySuspending {
                mediaServiceMock.getEncryptedMedia(
                    isEqual(thumbnailFile),
                    isAny(),
                    isAny()
                )
            } returns
                    Result.failure(RuntimeException("Oh no!"))
            val originalFile = EncryptedFile("http://host.local/media/abcdef", jwk, "", mapOf())
            mocker.everySuspending {
                mediaServiceMock.getEncryptedMedia(
                    isEqual(originalFile),
                    isAny(),
                    isAny()
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

        should("get no thumbnail when the encrypted thumbnail could not be loaded and the original file is larger than 1MB") {
            val thumbnailFile = EncryptedFile("http://host.local/media/123456", jwk, "", mapOf())
            mocker.everySuspending {
                mediaServiceMock.getEncryptedMedia(
                    isEqual(thumbnailFile),
                    isAny(),
                    isAny()
                )
            } returns
                    Result.failure(RuntimeException("Oh no!"))
            val originalFile = EncryptedFile("http://host.local/media/abcdef", jwk, "", mapOf())
            mocker.everySuspending {
                mediaServiceMock.getEncryptedMedia(
                    isEqual(originalFile),
                    isAny(),
                    isAny()
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
                sizeInBytes = 5_000_000, // too large!
                thumbnailProgressFlow = MutableStateFlow(null),
            )

            result shouldBe null
        }

        should("suspend when loading the encrypted thumbnail takes a while") {
            val thumbnailFile = EncryptedFile("http://host.local/media/123456", jwk, "", mapOf())
            mocker.everySuspending {
                mediaServiceMock.getEncryptedMedia(
                    isEqual(thumbnailFile),
                    isAny(),
                    isAny()
                )
            } runs {
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