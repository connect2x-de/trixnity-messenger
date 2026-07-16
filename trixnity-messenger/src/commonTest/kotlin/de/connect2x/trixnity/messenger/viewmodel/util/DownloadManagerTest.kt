package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media.MediaService
import de.connect2x.trixnity.clientserverapi.model.media.FileTransferProgress
import de.connect2x.trixnity.core.model.events.m.room.EncryptedFile
import de.connect2x.trixnity.core.model.events.m.room.FileInfo
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.DownloadManagerImpl
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
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
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class DownloadManagerTest {
    val matrixClientMock = mock<MatrixClient>()

    val mediaServiceMock: MediaService = mock()

    val maxMediaSize = Long.MAX_VALUE

    init {
        resetMocks(mediaServiceMock)
        every { matrixClientMock.di } returns koinApplication { modules(module { single { mediaServiceMock } }) }.koin
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `return 'success' when download is finished successfully`() = runTest {
        val cut = DownloadManagerImpl(backgroundScope.coroutineContext)
        everySuspend { mediaServiceMock.getMedia("mxc://localhost/ABCDEFGH", any(), any(), any()) } returns
            Result.success(InMemoryPlatformMedia("test".encodeToByteArray().toByteArrayFlow()))
        val progress = MutableStateFlow<FileTransferProgressElement?>(null)

        val result =
            cut.startDownloadAsync(
                    matrixClientMock,
                    RoomMessageEventContent.FileBased.File("", url = "mxc://localhost/ABCDEFGH"),
                    "file.pdf",
                    progress,
                    maxMediaSize,
                )
                .await()
                .getOrThrow()

        result.toByteArray() shouldBe "test".encodeToByteArray()
    }

    @Test
    fun `download encrypted file`() = runTest {
        val cut = DownloadManagerImpl(backgroundScope.coroutineContext)
        val encryptedFile =
            EncryptedFile(
                url = "mxc://localhost/ABCDEFGH",
                key = EncryptedFile.JWK(key = "key"),
                initialisationVector = "vector",
                hashes = mapOf(),
            )
        everySuspend { mediaServiceMock.getEncryptedMedia(encryptedFile, any(), any(), any()) } returns
            Result.success(InMemoryPlatformMedia("test".encodeToByteArray().toByteArrayFlow()))
        val progress = MutableStateFlow<FileTransferProgressElement?>(null)

        val result =
            cut.startDownloadAsync(
                    matrixClientMock,
                    RoomMessageEventContent.FileBased.File("", file = encryptedFile),
                    "file.pdf",
                    progress,
                    maxMediaSize,
                )
                .await()
                .getOrThrow()

        result.toByteArray() shouldBe "test".encodeToByteArray()
    }

    @Test
    fun `track progress of download`() = runTest {
        val cut = DownloadManagerImpl(backgroundScope.coroutineContext)
        val internalProgressState: MutableStateFlow<MutableStateFlow<FileTransferProgress?>?> = MutableStateFlow(null)
        everySuspend { mediaServiceMock.getMedia("mxc://localhost/ABCDEFGH", any<Long>(), any()) } calls
            {
                @Suppress("UNCHECKED_CAST")
                internalProgressState.value = it.args[2] as MutableStateFlow<FileTransferProgress?>
                delay(1.minutes)
                Result.success(InMemoryPlatformMedia("test".encodeToByteArray().toByteArrayFlow()))
            }
        val progress = MutableStateFlow<FileTransferProgressElement?>(null)

        val result =
            cut.startDownloadAsync(
                matrixClientMock,
                RoomMessageEventContent.FileBased.File("", url = "mxc://localhost/ABCDEFGH"),
                "file.pdf",
                progress,
                maxMediaSize,
            )
        val internalProgress = internalProgressState.filterNotNull().first()

        internalProgress.value = FileTransferProgress(300, 1000)
        delay(100)
        progress.value shouldBe FileTransferProgressElement(0.3f, "0,3kB / 1,0kB")
        internalProgress.value = FileTransferProgress(600, 1000)
        delay(100)
        progress.value shouldBe FileTransferProgressElement(0.6f, "0,6kB / 1,0kB")
        internalProgress.value = FileTransferProgress(1000, 1000)
        delay(100)
        progress.value shouldBe FileTransferProgressElement(1.0f, "1,0kB / 1,0kB")

        result.await().getOrThrow()
    }

    @Test
    fun `stop tracking progress of download when download is cancelled`() = runTest {
        val cut = DownloadManagerImpl(backgroundScope.coroutineContext)
        val internalProgressState: MutableStateFlow<MutableStateFlow<FileTransferProgress?>?> = MutableStateFlow(null)
        everySuspend { mediaServiceMock.getMedia("mxc://localhost/ABCDEFGH", any<Long>(), any()) } calls
            {
                @Suppress("UNCHECKED_CAST")
                internalProgressState.value = it.args[2] as MutableStateFlow<FileTransferProgress?>
                delay(1.minutes)
                Result.success(InMemoryPlatformMedia("test".encodeToByteArray().toByteArrayFlow()))
            }
        val progress = MutableStateFlow<FileTransferProgressElement?>(null)

        val result =
            cut.startDownloadAsync(
                matrixClientMock,
                RoomMessageEventContent.FileBased.File("", url = "mxc://localhost/ABCDEFGH"),
                "file.pdf",
                progress,
                maxMediaSize,
            )
        val internalProgress = internalProgressState.filterNotNull().first()

        internalProgress.value = FileTransferProgress(300, 1000)
        delay(100)
        progress.value shouldBe FileTransferProgressElement(0.3f, "0,3kB / 1,0kB")

        result.cancelAndJoin()

        internalProgress.value = FileTransferProgress(600, 1000)
        delay(100)
        progress.value shouldBe FileTransferProgressElement(0.3f, "0,3kB / 1,0kB")
    }

    @Test
    fun `fallback on event size when no total download size is given`() = runTest {
        val cut = DownloadManagerImpl(backgroundScope.coroutineContext)
        val internalProgressState: MutableStateFlow<MutableStateFlow<FileTransferProgress?>?> = MutableStateFlow(null)
        everySuspend { mediaServiceMock.getMedia("mxc://localhost/ABCDEFGH", any<Long>(), any()) } calls
            {
                @Suppress("UNCHECKED_CAST")
                internalProgressState.value = it.args[2] as MutableStateFlow<FileTransferProgress?>
                delay(1.minutes)
                Result.success(InMemoryPlatformMedia("test".encodeToByteArray().toByteArrayFlow()))
            }
        val progress = MutableStateFlow<FileTransferProgressElement?>(null)

        val result =
            cut.startDownloadAsync(
                matrixClientMock,
                RoomMessageEventContent.FileBased.File(
                    "",
                    url = "mxc://localhost/ABCDEFGH",
                    info = FileInfo(size = 1000),
                ),
                "file.pdf",
                progress,
                maxMediaSize,
            )
        val internalProgress = internalProgressState.filterNotNull().first()

        internalProgress.value = FileTransferProgress(300, null)
        delay(100)
        progress.value shouldBe FileTransferProgressElement(0.3f, "0,3kB / 1,0kB")
        internalProgress.value = FileTransferProgress(600, null)
        delay(100)
        progress.value shouldBe FileTransferProgressElement(0.6f, "0,6kB / 1,0kB")
        internalProgress.value = FileTransferProgress(1000, null)
        delay(100)
        progress.value shouldBe FileTransferProgressElement(1.0f, "1,0kB / 1,0kB")

        result.await().getOrThrow()
    }
}
