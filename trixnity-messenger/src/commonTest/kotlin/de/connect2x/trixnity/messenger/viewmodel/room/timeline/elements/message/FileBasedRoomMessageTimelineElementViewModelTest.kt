package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.MediaService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Suppress("NonAsciiCharacters")
class FileBasedRoomMessageTimelineElementViewModelTest {
    val matrixClientMock = mock<MatrixClient>()
    val downloadManagerMock = mock<DownloadManager>()
    val mediaServiceMock = mock<MediaService>()

    val file = "download".encodeToByteArray()

    init {
        resetMocks(matrixClientMock, downloadManagerMock, mediaServiceMock)
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { mediaServiceMock }
                })
        }.koin
    }

    @Test
    fun `downloading » download a file and process result`() = runTest {
        every {
            downloadManagerMock.startDownloadAsync(eq(matrixClientMock), any(), any(), any())
        } returns async { Result.success(InMemoryPlatformMedia(file)) }

        val cut = fileBasedMessageViewModel()
        var downloadResult: ByteArray? = null
        cut.downloadMedia { download ->
            downloadResult = download.toByteArray()
        }

        eventually(1.seconds) {
            downloadResult shouldBe file
            cut.downloadMediaError.value shouldBe null
            cut.downloadMediaResult shouldNotBe null
        }
    }

    @Test
    fun `downloading » download a file and set Result to 'failure' if not successful`() = runTest {
        every {
            downloadManagerMock.startDownloadAsync(eq(matrixClientMock), any(), any(), any())
        } returns async { Result.failure(RuntimeException("Oh no!")) }

        val cut = fileBasedMessageViewModel()
        var downloadResult: ByteArray? = null
        cut.downloadMedia { download ->
            downloadResult = download.toByteArray()
        }

        eventually(1.seconds) {
            downloadResult shouldBe null
            cut.downloadMediaError.value shouldBe "Download failed: Oh no!"
            cut.downloadMediaResult.value shouldBe null
        }
    }

    @Test
    fun `downloading » download a file and reset everything if the download is cancelled`() = runTest {
        every {
            downloadManagerMock.startDownloadAsync(eq(matrixClientMock), any(), any(), any())
        } returns async {
            delay(5.seconds)
            Result.failure(RuntimeException("Oh no!"))
        }

        val cut = fileBasedMessageViewModel()
        var downloadResult: ByteArray? = null
        cut.downloadMedia { download ->
            downloadResult = download.toByteArray()
        }

        delay(100.milliseconds)
        cut.cancelDownloadMedia()
        eventually(1.seconds) {
            downloadResult shouldBe null
            cut.downloadMediaError.value shouldBe null
            cut.downloadMediaResult.value shouldBe null
            cut.downloadMediaProgress.value shouldBe null
        }
    }

    @Test
    fun `loading » load a file into memory`() = runTest {
        every {
            downloadManagerMock.startDownloadAsync(eq(matrixClientMock), any(), any(), any())
        } returns async { Result.success(InMemoryPlatformMedia(file)) }
        val cut = fileBasedMessageViewModel()

        cut.loadMedia()
        delay(500.milliseconds)

        cut.loadMediaResult.value shouldBe file
        cut.loadMediaError.value shouldBe null
    }

    @Test
    fun `caption » show body as caption`() = runTest {
        fileBasedMessageViewModel(caption = "Amazing File!").showCaption shouldBe true
    }

    @Test
    fun `caption » don't show body as caption`() = runTest {
        fileBasedMessageViewModel(caption = null).showCaption shouldBe false
    }

    private fun TestScope.fileBasedMessageViewModel(caption: String? = null): FileBasedRoomMessageTimelineElementViewModel<RoomMessageEventContent.FileBased.File> =
        object : FileBasedRoomMessageTimelineElementViewModel<RoomMessageEventContent.FileBased.File>(
            testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("test", "server") to matrixClientMock)
                        ) + module {
                            single { downloadManagerMock }
                        })
                }.koin,
                userId = UserId("test", "server"),
            ),
            RoomMessageEventContent.FileBased.File(
                caption ?: "test.pdf",
                fileName = "test.pdf",
                url = "mxc://localhost/unencrypted123456",
                file = EncryptedFile(url = "mxc://localhost/123456", key = EncryptedFile.JWK(""), "", mapOf()),
            ),
            roomId = RoomId("!testpdf:server"),
            eventIdOrTransactionId = EventIdOrTransactionId.EventIdOrTransactionId(EventId("\$very1demure1event")),
            onOpenMention = { _, _ -> }
        ) {}
}
