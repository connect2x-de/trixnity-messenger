package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.MediaService
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.toByteArray
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class FileBasedRoomMessageTimelineElementViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 5_000

    val matrixClientMock = mock<MatrixClient>()
    val downloadManagerMock = mock<DownloadManager>()
    val mediaServiceMock = mock<MediaService>()

    val file = "download".encodeToByteArray()

    init {
        beforeTest {
            resetMocks(matrixClientMock, downloadManagerMock, mediaServiceMock)
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { mediaServiceMock }
                    }
                )
            }.koin
        }

        should("download a file and process result") {
            every {
                downloadManagerMock.startDownloadAsync(eq(matrixClientMock), any(), any(), any(), any())
            } returns async { Result.success(Unit) }

            val cut = fileBasedMessageViewModel()
            var downloadResult: ByteArray? = null
            cut.download { download ->
                downloadResult = download.toByteArray()
            }

            eventually(3.seconds) {
                downloadResult shouldBe file
                cut.downloadError.value shouldBe null
                cut.downloadSuccessful.value shouldBe true
            }

            cancelNeverEndingCoroutines()
        }
        should("download a file and set Result to 'failure' if not successful") {
            every {
                downloadManagerMock.startDownloadAsync(eq(matrixClientMock), any(), any(), any(), any())
            } returns async { Result.failure(RuntimeException("Oh no!")) }

            val cut = fileBasedMessageViewModel()
            var downloadResult: ByteArray? = null
            cut.download { download ->
                downloadResult = download.toByteArray()
            }

            eventually(3.seconds) {
                downloadResult shouldBe null
                cut.downloadError.value shouldBe "Download failed: Oh no!"
                cut.downloadSuccessful.value shouldBe false
            }

            cancelNeverEndingCoroutines()
        }
        should("download a file and reset everything if the download is cancelled") {
            every {
                downloadManagerMock.startDownloadAsync(eq(matrixClientMock), any(), any(), any(), any())
            } returns async {
                delay(5.seconds)
                Result.failure(RuntimeException("Oh no!"))
            }

            val cut = fileBasedMessageViewModel()
            var downloadResult: ByteArray? = null
            cut.download { download ->
                downloadResult = download.toByteArray()
            }

            delay(100.milliseconds)
            downloadResult shouldBe null
            cut.downloadError.value shouldBe null
            cut.downloadSuccessful.value shouldBe null
            cut.downloadProgress.value shouldBe null

            cancelNeverEndingCoroutines()
        }
    }

    private fun fileBasedMessageViewModel(): FileBasedRoomMessageTimelineElementViewModel<RoomMessageEventContent.FileBased.File> =
        object : FileBasedRoomMessageTimelineElementViewModel<RoomMessageEventContent.FileBased.File>(
            MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock)) +
                                module {
                                    single { downloadManagerMock }
                                })
                }.koin,
                userId = UserId("test", "server"),
            ),
            RoomMessageEventContent.FileBased.File(
                "",
                fileName = "test.pdf",
                url = "mxc://localhost/unencrypted123456",
                file = EncryptedFile(url = "mxc://localhost/123456", key = EncryptedFile.JWK(""), "", mapOf())
            ),
            onOpenMedia = { _, _ -> }
        ) {}
}
