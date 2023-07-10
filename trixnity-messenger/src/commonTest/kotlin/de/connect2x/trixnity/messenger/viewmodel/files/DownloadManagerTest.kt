package de.connect2x.trixnity.messenger.viewmodel.files

import de.connect2x.trixnity.messenger.viewmodel.mock.MediaServiceMock
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.MediaService
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.utils.toByteArrayFlow
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class DownloadManagerTest : ShouldSpec() {
    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    lateinit var mediaServiceMock: MediaService

    init {
        Dispatchers.setMain(testMainDispatcher)
        coroutineTestScope = true

        beforeTest {
            mocker.reset()
            injectMocks(mocker)
            mediaServiceMock = MediaServiceMock(mocker)

            mocker.every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { mediaServiceMock }
                    }
                )
            }.koin
        }

        should("return 'success' when download is finished successfully") {
            val cut = DownloadManagerImpl(coroutineContext)
            val progress = MutableStateFlow<FileTransferProgress?>(FileTransferProgress(0, 100))
            mocker.everySuspending {
                mediaServiceMock.getMedia("mxc://localhost/ABCDEFGH", progress)
            } returns Result.success("test".encodeToByteArray().toByteArrayFlow())
            val progressElement = MutableStateFlow<FileTransferProgressElement?>(null)
            val download = Download("mxc://localhost/ABCDEFGH", "file.png", null, progress)

            val result = cut.startDownloadAsync(matrixClientMock, download, progressElement).await()

            result.getOrNull() shouldBe "test".encodeToByteArray()
        }

        should("download encrypted file") {
            val cut = DownloadManagerImpl(coroutineContext)
            val progress = MutableStateFlow<FileTransferProgress?>(FileTransferProgress(0, 100))
            val encryptedFile = EncryptedFile(
                url = "mxc://localhost/ABCDEFGH",
                key = EncryptedFile.JWK(key = "key"),
                initialisationVector = "vector",
                hashes = mapOf()
            )
            mocker.everySuspending {
                mediaServiceMock.getEncryptedMedia(encryptedFile, progress)
            } returns Result.success("test".encodeToByteArray().toByteArrayFlow())
            val progressElement = MutableStateFlow<FileTransferProgressElement?>(null)
            val download = Download("mxc://localhost/ABCDEFGH", "file.png", encryptedFile, progress)

            val result = cut.startDownloadAsync(matrixClientMock, download, progressElement).await()

            result.getOrNull() shouldBe "test".encodeToByteArray()
        }

        should("track progress of download") {
            val cut = DownloadManagerImpl(coroutineContext)
            val progress = MutableStateFlow<FileTransferProgress?>(FileTransferProgress(0, 1000))
            mocker.everySuspending { mediaServiceMock.getMedia("mxc://localhost/abcdefgh", progress) } runs {
                withContext(Dispatchers.Default) {
                    delay(500)
                    Result.success("test".encodeToByteArray().toByteArrayFlow())
                }
            }
            val progressElement = MutableStateFlow<FileTransferProgressElement?>(null)
            val download = Download("mxc://localhost/abcdefgh", "file.png", null, progress)

            cut.startDownloadAsync(matrixClientMock, download, progressElement)

            progress.value = FileTransferProgress(300, 1000)
            testCoroutineScheduler.advanceUntilIdle()
            cut.getProgressElement("mxc://localhost/abcdefgh")?.value shouldBe FileTransferProgressElement(
                0.3f,
                "0,3kB / 1,0kB"
            )
            progress.value = FileTransferProgress(600, 1000)
            testCoroutineScheduler.advanceUntilIdle()
            cut.getProgressElement("mxc://localhost/abcdefgh")?.value shouldBe FileTransferProgressElement(
                0.6f,
                "0,6kB / 1,0kB"
            )
            progress.value = FileTransferProgress(1000, 1000)
            testCoroutineScheduler.advanceUntilIdle()
            cut.getProgressElement("mxc://localhost/abcdefgh")?.value shouldBe FileTransferProgressElement(
                1.0f,
                "1,0kB / 1,0kB"
            )

            withContext(Dispatchers.Default) {
                delay(600)
                cut.getSuccess("mxc://localhost/abcdefgh")?.value shouldBe true
            }
        }

        should("stop tracking progress of download when download is cancelled") {
            val cut = DownloadManagerImpl(coroutineContext)
            val progress = MutableStateFlow<FileTransferProgress?>(null)
            mocker.everySuspending { mediaServiceMock.getMedia("mxc://localhost/123456", progress) } runs {
                withContext(Dispatchers.Default) { // real delay
                    delay(2_000)
                    Result.success("test".encodeToByteArray().toByteArrayFlow())
                }
            }
            val progressElement = MutableStateFlow<FileTransferProgressElement?>(null)
            val download = Download("mxc://localhost/123456", "file.png", null, progress)

            val result = cut.startDownloadAsync(matrixClientMock, download, progressElement)

            progress.value = FileTransferProgress(300, 1000)
            testCoroutineScheduler.advanceUntilIdle()
            cut.getProgressElement("mxc://localhost/123456")?.value shouldBe
                    FileTransferProgressElement(0.3f, "0,3kB / 1,0kB")

            result.cancelAndJoin()

            progress.value = FileTransferProgress(600, 1000)
            testCoroutineScheduler.advanceUntilIdle()
            cut.getProgressElement("mxc://localhost/123456")?.value shouldBe FileTransferProgressElement(
                0.3f,
                "0,3kB / 1,0kB"
            )
            progress.value = FileTransferProgress(1000, 1000)
            testCoroutineScheduler.advanceUntilIdle()
            cut.getProgressElement("mxc://localhost/123456")?.value shouldBe FileTransferProgressElement(
                0.3f,
                "0,3kB / 1,0kB"
            )
        }
    }
}