package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.util.DownloadManagerImpl
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.mock.MediaServiceMock
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.MediaService
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.toByteArray
import net.folivo.trixnity.utils.toByteArrayFlow
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class DownloadManagerTest : ShouldSpec() {
    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    lateinit var mediaServiceMock: MediaService

    init {
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
            Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
            val cut = DownloadManagerImpl(coroutineContext)
            mocker.everySuspending {
                mediaServiceMock.getMedia(isEqual("mxc://localhost/ABCDEFGH"), isAny(), isAny())
            } returns Result.success("test".encodeToByteArray().toByteArrayFlow())
            val progress = MutableStateFlow<FileTransferProgressElement?>(null)
            val success = MutableStateFlow(false)

            val result = cut.startDownloadAsync(
                matrixClientMock,
                RoomMessageEventContent.FileBased.File("", url = "mxc://localhost/ABCDEFGH"),
                "file.pdf",
                progress,
                success
            ).await()

            result.getOrNull()?.toByteArray() shouldBe "test".encodeToByteArray()
        }

        should("download encrypted file") {
            Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
            val cut = DownloadManagerImpl(coroutineContext)
            val encryptedFile = EncryptedFile(
                url = "mxc://localhost/ABCDEFGH",
                key = EncryptedFile.JWK(key = "key"),
                initialisationVector = "vector",
                hashes = mapOf()
            )
            mocker.everySuspending {
                mediaServiceMock.getEncryptedMedia(isEqual(encryptedFile), isAny(), isAny())
            } returns Result.success("test".encodeToByteArray().toByteArrayFlow())
            val progress = MutableStateFlow<FileTransferProgressElement?>(null)
            val success = MutableStateFlow(false)

            val result = cut.startDownloadAsync(
                matrixClientMock,
                RoomMessageEventContent.FileBased.File("", file = encryptedFile),
                "file.pdf",
                progress,
                success
            ).await()

            result.getOrNull()?.toByteArray() shouldBe "test".encodeToByteArray()
        }

        should("track progress of download") {
            Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
            val cut = DownloadManagerImpl(coroutineContext)
            val internalProgressState: MutableStateFlow<MutableStateFlow<FileTransferProgress?>?> =
                MutableStateFlow(null)
            mocker.everySuspending {
                mediaServiceMock.getMedia(
                    isEqual("mxc://localhost/ABCDEFGH"),
                    isAny(),
                    isAny()
                )
            } runs {
                @Suppress("UNCHECKED_CAST")
                internalProgressState.value = it[1] as MutableStateFlow<FileTransferProgress?>
                delay(1.minutes)
                Result.success("test".encodeToByteArray().toByteArrayFlow())
            }
            val progress = MutableStateFlow<FileTransferProgressElement?>(null)
            val success = MutableStateFlow(false)

            val result = cut.startDownloadAsync(
                matrixClientMock,
                RoomMessageEventContent.FileBased.File("", url = "mxc://localhost/ABCDEFGH"),
                "file.pdf",
                progress,
                success
            )
            val internalProgress = internalProgressState.filterNotNull().first()

            internalProgress.value = FileTransferProgress(300, 1000)
            testCoroutineScheduler.advanceTimeBy(100.milliseconds)
            progress.value shouldBe FileTransferProgressElement(0.3f, "0,3kB / 1,0kB")
            internalProgress.value = FileTransferProgress(600, 1000)
            testCoroutineScheduler.advanceTimeBy(100.milliseconds)
            progress.value shouldBe FileTransferProgressElement(0.6f, "0,6kB / 1,0kB")
            internalProgress.value = FileTransferProgress(1000, 1000)
            testCoroutineScheduler.advanceTimeBy(100.milliseconds)
            progress.value shouldBe FileTransferProgressElement(1.0f, "1,0kB / 1,0kB")

            withContext(Dispatchers.Default) {
                delay(600)
                success.value shouldBe true
            }

            result.await()
        }

        should("stop tracking progress of download when download is cancelled") {
            Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
            val cut = DownloadManagerImpl(coroutineContext)
            val internalProgressState: MutableStateFlow<MutableStateFlow<FileTransferProgress?>?> =
                MutableStateFlow(null)
            mocker.everySuspending {
                mediaServiceMock.getMedia(
                    isEqual("mxc://localhost/ABCDEFGH"),
                    isAny(),
                    isAny()
                )
            } runs {
                @Suppress("UNCHECKED_CAST")
                internalProgressState.value = it[1] as MutableStateFlow<FileTransferProgress?>
                delay(1.minutes)
                Result.success("test".encodeToByteArray().toByteArrayFlow())
            }
            val progress = MutableStateFlow<FileTransferProgressElement?>(null)
            val success = MutableStateFlow(false)

            val result = cut.startDownloadAsync(
                matrixClientMock,
                RoomMessageEventContent.FileBased.File("", url = "mxc://localhost/ABCDEFGH"),
                "file.pdf",
                progress,
                success
            )
            val internalProgress = internalProgressState.filterNotNull().first()

            internalProgress.value = FileTransferProgress(300, 1000)
            testCoroutineScheduler.advanceTimeBy(100.milliseconds)
            progress.value shouldBe FileTransferProgressElement(0.3f, "0,3kB / 1,0kB")

            result.cancelAndJoin()

            internalProgress.value = FileTransferProgress(600, 1000)
            testCoroutineScheduler.advanceTimeBy(100.milliseconds)
            progress.value shouldBe FileTransferProgressElement(0.3f, "0,3kB / 1,0kB")
        }
    }
}