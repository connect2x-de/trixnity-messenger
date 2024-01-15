package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.beOfType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.media.MediaService
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.toByteArray
import net.folivo.trixnity.utils.toByteArrayFlow
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

    @Mock
    lateinit var mediaServiceMock: MediaService

    init {
        Dispatchers.setMain(Dispatchers.Unconfined)
        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { mediaServiceMock }
                        }
                    )
                }.koin
                every { matrixClientMock.media } returns mediaServiceMock
            }
        }

        should("download a file and return the result if successful") {
            val file = "download".encodeToByteArray()
            mocker.every {
                downloadManagerMock.startDownloadAsync(isEqual(matrixClientMock), isAny(), isAny(), isAny(), isAny())
            } returns async { Result.success(file.toByteArrayFlow()) }

            val cut = fileBasedMessageViewModel()
            cut.openSaveFileDialog()

            val downloadFile = cut.downloadFile()

            downloadFile.getFileResult().getOrNull()?.toByteArray() shouldBe file
            cut.saveFileDialogOpen.value shouldBe false
        }

        should("download a file and set Result to 'failure' if not successful") {
            mocker.every {
                downloadManagerMock.startDownloadAsync(isEqual(matrixClientMock), isAny(), isAny(), isAny(), isAny())
            } returns async { Result.failure(RuntimeException("Oh no!")) }


            val cut = fileBasedMessageViewModel()
            cut.openSaveFileDialog()

            val downloadFile = cut.downloadFile()

            downloadFile.getFileResult().exceptionOrNull() should beOfType<RuntimeException>()
            cut.saveFileDialogOpen.value shouldBe true // to show error message
        }

        should("download a file and return 'null' if the download is cancelled") {
            val scope = CoroutineScope(Dispatchers.Default)

            mocker.every {
                downloadManagerMock.startDownloadAsync(isEqual(matrixClientMock), isAny(), isAny(), isAny(), isAny())
            } returns scope.async {
                delay(5.seconds)
                Result.success("download".encodeToByteArray().toByteArrayFlow())
            }

            val cut = fileBasedMessageViewModel()
            cut.openSaveFileDialog()

            val downloadFile = cut.downloadFile()

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
            sender = MutableStateFlow(UserInfoElement("")),
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
    ) : AbstractFileBasedMessageViewModel(
        viewModelContext,
        RoomMessageEventContent.FileBased.File("", fileName = "test.pdf", url = url, file = encryptedFile)
    ), ViewModelContext by viewModelContext {
        override val invitation: StateFlow<String?> =
            invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    }
}