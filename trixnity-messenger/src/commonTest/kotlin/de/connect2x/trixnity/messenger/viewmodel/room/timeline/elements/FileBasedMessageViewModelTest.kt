package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.files.Download
import de.connect2x.trixnity.messenger.viewmodel.files.DownloadManager
import de.connect2x.trixnity.messenger.viewmodel.files.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.beOfType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import org.kodein.mock.*
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class FileBasedMessageViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var downloadManagerMock: DownloadManager

    @Fake
    lateinit var encryptedFile: EncryptedFile

    init {
        Dispatchers.setMain(testMainDispatcher)
        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
                every { downloadManagerMock.getProgressElement(isAny()) } returns MutableStateFlow(null)
                every { downloadManagerMock.getSuccess(isAny()) } returns MutableStateFlow(false)
            }
        }

        should("download a file and return the result if successful") {
            val file = "download".encodeToByteArray()
            mocker.every {
                downloadManagerMock.startDownloadAsync(isEqual(matrixClientMock), isDownload(), isAny())
            } returns async { Result.success(file) }

            val cut = fileBasedMessageViewModel()
            cut.openSaveFileDialog()

            val progressElement = MutableStateFlow<FileTransferProgressElement?>(FileTransferProgressElement(0f, ""))
            val downloadFile = cut.downloadFile(progressElement)

            downloadFile.getFileResult().getOrNull() shouldBe file
            cut.saveFileDialogOpen.value shouldBe false
        }

        should("download a file and set Result to 'failure' if not successful")
        {
            mocker.every {
                downloadManagerMock.startDownloadAsync(isEqual(matrixClientMock), isDownload(), isAny())
            } returns async { Result.failure(RuntimeException("Oh no!")) }


            val cut = fileBasedMessageViewModel()
            cut.openSaveFileDialog()

            val progressElement = MutableStateFlow<FileTransferProgressElement?>(FileTransferProgressElement(0f, ""))
            val downloadFile = cut.downloadFile(progressElement)

            downloadFile.getFileResult().exceptionOrNull() should beOfType<RuntimeException>()
            cut.saveFileDialogOpen.value shouldBe true // to show error message
        }

        should("download a file and return 'null' if the download is cancelled")
        {
            val scope = CoroutineScope(Dispatchers.Default)

            mocker.every {
                downloadManagerMock.startDownloadAsync(isEqual(matrixClientMock), isDownload(), isAny())
            } returns scope.async {
                delay(5.seconds)
                Result.success("download".encodeToByteArray())
            }

            val cut = fileBasedMessageViewModel()
            cut.openSaveFileDialog()

            val progressElement =
                MutableStateFlow<FileTransferProgressElement?>(FileTransferProgressElement(0f, ""))
            val downloadFile = cut.downloadFile(progressElement)

            val job = scope.launch(Dispatchers.Default) {
                delay(100.milliseconds)
                val result = downloadFile.getFileResult()
                result.getOrNull() shouldBe null
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
        }
    }

    private fun fileBasedMessageViewModel() = FileBasedMessageViewModelInstance(
        viewModelContext = MatrixClientViewModelContextImpl(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            di = koinApplication {
                modules(trixnityMessengerModule(), testMatrixClientModule(matrixClientMock), module {
                    single { downloadManagerMock }
                })
            }.koin,
            accountName = "test",
        ),
        url = "mxc://localhost/123456",
        encryptedFile = encryptedFile,
        invitation = flowOf(""),
        formattedDate = "",
        showDateAbove = false,
        formattedTime = "",
        isByMe = false,
        showChatBubbleEdge = false,
        showBigGap = false,
        showSender = MutableStateFlow(false),
        sender = MutableStateFlow(""),
    )

    private class FileBasedMessageViewModelInstance(
        viewModelContext: MatrixClientViewModelContext,
        override val url: String?,
        override val encryptedFile: EncryptedFile?,
        override val invitation: Flow<String?>,
        override val formattedDate: String,
        override val showDateAbove: Boolean,
        override val formattedTime: String?,
        override val isByMe: Boolean,
        override val showChatBubbleEdge: Boolean,
        override val showBigGap: Boolean,
        override val showSender: StateFlow<Boolean>,
        override val sender: StateFlow<String>,
    ) : AbstractFileBasedMessageViewModel(viewModelContext), ViewModelContext by viewModelContext {
        override fun getFileNameWithExtension(): String = "test.jpg"
    }

    private fun ArgConstraintsBuilder.isDownload(
        capture: MutableList<Download>? = null
    ): Download =
        isValid(ArgConstraint(capture, { "isDownload" }) {
            if (it.fileUrl == "mxc://localhost/123456" && it.fileName == "test.jpg") ArgConstraint.Result.Success
            else ArgConstraint.Result.Failure { "Expected download 'mxc://localhost/123456' with 'test.jpg', but got $it." }
        })
}