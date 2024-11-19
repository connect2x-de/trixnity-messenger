package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenMediaCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenMediaType
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
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.MediaService
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArray
import net.folivo.trixnity.utils.toByteArrayFlow
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class FileBasedMessageViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 5_000

    val matrixClientMock = mock<MatrixClient>()

    val downloadManagerMock = mock<DownloadManager>()

    val mediaServiceMock = mock<MediaService>()

    init {
        Dispatchers.setMain(Dispatchers.Unconfined)
        beforeTest {
            resetMocks(matrixClientMock, downloadManagerMock, mediaServiceMock)
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { mediaServiceMock }
                    }
                )
            }.koin
            every { downloadManagerMock.scope } returns CoroutineScope(Dispatchers.Default)
        }

        should("download a file and return the result on success") {
            val file = "download".encodeToByteArray()
            every {
                downloadManagerMock.startDownloadAsync(eq(matrixClientMock), any(), any(), any(), any())
            } returns async { Result.success(file.toByteArrayFlow()) }

            val cut = fileBasedMessageViewModel()
            cut.openSaveFileDialog()

            val downloadCalls = MutableStateFlow(0)
            var downloadResult: ByteArrayFlow? = null
            cut.downloadFile {
                downloadResult = it
                downloadCalls.value++
            }

            eventually(3.seconds) {
                downloadResult?.toByteArray() shouldBe file
                downloadCalls.value shouldBe 1
                cut.saveFileDialogOpen.value shouldBe false
                cut.downloadError.value shouldBe null
            }

            cancelNeverEndingCoroutines()
        }

        should("download a file and set Result to 'failure' if not successful") {
            every {
                downloadManagerMock.startDownloadAsync(eq(matrixClientMock), any(), any(), any(), any())
            } returns async { Result.failure(RuntimeException("Oh no!")) }

            val cut = fileBasedMessageViewModel()
            cut.openSaveFileDialog()

            val downloadCalls = MutableStateFlow(0)
            var downloadResult: ByteArrayFlow? = null
            cut.downloadFile {
                downloadResult = it
                downloadCalls.value++
            }

            eventually(3.seconds) {
                downloadResult shouldBe null
                downloadCalls.value shouldBe 0
                cut.downloadError.value shouldNotBe null
                cut.saveFileDialogOpen.value shouldBe true // to show error message
            }

            cancelNeverEndingCoroutines()
        }

        should("download a file and return 'null' if the download is cancelled") {
            val scope = CoroutineScope(Dispatchers.Default)

            every {
                downloadManagerMock.startDownloadAsync(eq(matrixClientMock), any(), any(), any(), any())
            } returns scope.async {
                delay(5.seconds)
                Result.success("download".encodeToByteArray().toByteArrayFlow())
            }

            val cut = fileBasedMessageViewModel()
            cut.openSaveFileDialog()

            val downloadCalls = MutableStateFlow(0)
            var downloadResult: ByteArrayFlow? = null
            cut.downloadFile {
                downloadResult = it
                downloadCalls.value++
            }

            val job = scope.launch(Dispatchers.Default) {
                delay(100.milliseconds)
                downloadResult shouldBe null
                downloadCalls.value shouldBe 0
            }

            cut.saveFileDialogOpen.value shouldNotBe false // downloading has not yet begun
            withContext(Dispatchers.Default) {
                delay(150.milliseconds) // download in progress
                cut.saveFileDialogOpen.value shouldBe true // in case an error has to be reported
                cut.cancelDownload()
                delay(200.milliseconds) // download is cancelled
                job.isCancelled shouldBe false // assert in the job is OK
                cut.saveFileDialogOpen.value shouldBe false
            }

            cancelNeverEndingCoroutines()
        }
    }

    private fun fileBasedMessageViewModel(): FileBasedMessageViewModelInstance {

        val fileBasedMessageViewModelInstance = FileBasedMessageViewModelInstance(
            viewModelContext = MatrixClientViewModelContextImpl(
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
            url = "mxc://localhost/unencrypted123456",
            encryptedFile = EncryptedFile(url = "mxc://localhost/123456", key = EncryptedFile.JWK(""), "", mapOf()),
            invitation = flowOf(""),
            formattedDate = "",
            showDateAbove = false,
            formattedTime = "",
            isByMe = false,
            showChatBubbleEdge = false,
            showBigGap = false,
            showSender = MutableStateFlow(false),
            sender = MutableStateFlow(UserInfoElement("", UserId(""))),
            uploadProgress = MutableStateFlow(null),
            onOpenMedia = { content: RoomMessageEventContent.FileBased ->
            },
        )
        return fileBasedMessageViewModelInstance
    }

    private class FileBasedMessageViewModelInstance(
        viewModelContext: MatrixClientViewModelContext,
        url: String?,
        encryptedFile: EncryptedFile?,
        invitation: Flow<String?>,
        override val formattedDate: String,
        override val showDateAbove: Boolean,
        override val formattedTime: String?,
        override val isByMe: Boolean,
        override val showChatBubbleEdge: Boolean,
        override val showBigGap: Boolean,
        override val showSender: StateFlow<Boolean>,
        override val sender: StateFlow<UserInfoElement>,
        override val uploadProgress: StateFlow<FileTransferProgressElement?>, onOpenMedia: OpenMediaCallback,
    ) : AbstractFileBasedMessageViewModel(
        viewModelContext,
        RoomMessageEventContent.FileBased.File("", fileName = "test.pdf", url = url, file = encryptedFile), onOpenMedia
    ), ViewModelContext by viewModelContext {
        override val invitation: StateFlow<String?> =
            invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    }
}
