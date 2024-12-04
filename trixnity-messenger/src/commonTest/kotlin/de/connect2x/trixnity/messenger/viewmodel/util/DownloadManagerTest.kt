package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.DownloadManagerImpl
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
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
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class DownloadManagerTest : ShouldSpec() {
    val matrixClientMock = mock<MatrixClient>()

    val mediaServiceMock: MediaService = mock()

    init {
        coroutineTestScope = true

        beforeTest {
            resetMocks(mediaServiceMock)
            every { matrixClientMock.di } returns koinApplication {
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
            everySuspend {
                mediaServiceMock.getMedia(eq("mxc://localhost/ABCDEFGH"), any(), any())
            } returns Result.success(InMemoryPlatformMedia("test".encodeToByteArray().toByteArrayFlow()))
            val progress = MutableStateFlow<FileTransferProgressElement?>(null)

            val result = cut.startDownloadAsync(
                matrixClientMock,
                RoomMessageEventContent.FileBased.File("", url = "mxc://localhost/ABCDEFGH"),
                "file.pdf",
                progress,
            ).await().getOrThrow()

            result.toByteArray() shouldBe "test".encodeToByteArray()
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
            everySuspend {
                mediaServiceMock.getEncryptedMedia(eq(encryptedFile), any(), any())
            } returns Result.success(InMemoryPlatformMedia("test".encodeToByteArray().toByteArrayFlow()))
            val progress = MutableStateFlow<FileTransferProgressElement?>(null)

            val result = cut.startDownloadAsync(
                matrixClientMock,
                RoomMessageEventContent.FileBased.File("", file = encryptedFile),
                "file.pdf",
                progress
            ).await().getOrThrow()

            result.toByteArray() shouldBe "test".encodeToByteArray()
        }

        should("track progress of download") {
            Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
            val cut = DownloadManagerImpl(coroutineContext)
            val internalProgressState: MutableStateFlow<MutableStateFlow<FileTransferProgress?>?> =
                MutableStateFlow(null)
            everySuspend {
                mediaServiceMock.getMedia(
                    eq("mxc://localhost/ABCDEFGH"),
                    any(),
                    any()
                )
            } calls {
                @Suppress("UNCHECKED_CAST")
                internalProgressState.value = it.args[1] as MutableStateFlow<FileTransferProgress?>
                delay(1.minutes)
                Result.success(InMemoryPlatformMedia("test".encodeToByteArray().toByteArrayFlow()))
            }
            val progress = MutableStateFlow<FileTransferProgressElement?>(null)

            val result = cut.startDownloadAsync(
                matrixClientMock,
                RoomMessageEventContent.FileBased.File("", url = "mxc://localhost/ABCDEFGH"),
                "file.pdf",
                progress
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

            result.await().getOrThrow()
        }

        should("stop tracking progress of download when download is cancelled") {
            Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
            val cut = DownloadManagerImpl(coroutineContext)
            val internalProgressState: MutableStateFlow<MutableStateFlow<FileTransferProgress?>?> =
                MutableStateFlow(null)
            everySuspend {
                mediaServiceMock.getMedia(
                    eq("mxc://localhost/ABCDEFGH"),
                    any(),
                    any()
                )
            } calls {
                @Suppress("UNCHECKED_CAST")
                internalProgressState.value = it.args[1] as MutableStateFlow<FileTransferProgress?>
                delay(1.minutes)
                Result.success(InMemoryPlatformMedia("test".encodeToByteArray().toByteArrayFlow()))
            }
            val progress = MutableStateFlow<FileTransferProgressElement?>(null)

            val result = cut.startDownloadAsync(
                matrixClientMock,
                RoomMessageEventContent.FileBased.File("", url = "mxc://localhost/ABCDEFGH"),
                "file.pdf",
                progress,
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
