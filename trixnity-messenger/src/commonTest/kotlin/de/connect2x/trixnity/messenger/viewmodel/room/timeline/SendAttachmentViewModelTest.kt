package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media
import de.connect2x.trixnity.client.media.MediaService
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.room.message.MessageBuilder
import de.connect2x.trixnity.client.store.ServerData
import de.connect2x.trixnity.clientserverapi.model.media.GetMediaConfig
import de.connect2x.trixnity.clientserverapi.model.server.GetVersions
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.runTestWithCoroutineScope
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.GetImageDimensions
import de.connect2x.trixnity.messenger.util.ProcessImageUpload
import de.connect2x.trixnity.messenger.util.mb
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.utils.ByteArrayFlow
import de.connect2x.trixnity.utils.toByteArrayFlow
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.ktor.http.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class SendAttachmentViewModelTest {
    private val roomId = RoomId("!room1")
    private val me = UserId("bob", "localhost")

    private val matrixClientMock: MatrixClient = mock()
    private val roomServiceMock: RoomService = mock()
    private val mediaServiceMock: MediaService = mock()
    private val serverData: MutableStateFlow<ServerData> =
        MutableStateFlow(
            ServerData(
                versions = GetVersions.Response(listOf()),
                capabilities = null,
                mediaConfig = GetMediaConfig.Response(50.mb()),
            )
        )
    private val imageProcessMock =
        object : ProcessImageUpload {
            override suspend fun invoke(imageBytes: ByteArray, mimeType: ContentType): ByteArray {
                return byteArrayOf(0, 1, 2, 3, 4, 5)
            }
        }
    private val getImageDimensionsMock =
        object : GetImageDimensions {
            override suspend fun invoke(
                byteArrayFlow: ByteArrayFlow,
                maxMediaSize: Long,
                mimeType: ContentType?,
            ): Pair<Int?, Int?> {
                return 0 to 0
            }
        }

    @BeforeTest
    fun beforeTest() {
        configureTestLogging()
        resetMocks(matrixClientMock, roomServiceMock, mediaServiceMock)
        every { matrixClientMock.serverData } returns serverData
        every { matrixClientMock.di } returns
            koinApplication {
                    modules(
                        module {
                            single<RoomService> { roomServiceMock }
                            single<MediaService> { mediaServiceMock }
                            single<ProcessImageUpload> { imageProcessMock }
                            single<GetImageDimensions> { getImageDimensionsMock }
                        }
                    )
                }
                .koin
        every { roomServiceMock.getById(any()) } returns flowOf(null)
    }

    @Test
    fun `should have no error when uploading file less than max file size`() = runTestWithCoroutineScope {
        val cut = sendAttachmentViewModel(fileSize = 40.mb())
        delay(500.milliseconds)
        assertTrue { cut.error.value == null }
    }

    @Test
    fun `should have error when uploading file more than max file size`() = runTestWithCoroutineScope {
        val cut = sendAttachmentViewModel(fileSize = 60.mb())
        delay(500.milliseconds)
        assertTrue { cut.error.value != null }
    }

    @Test
    fun `should not treat SVG as image`() = runTestWithCoroutineScope {
        val cut = sendAttachmentViewModel(fileSize = 60.mb(), contentType = ContentType.Image.SVG)
        assertFalse { cut.isImage }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `should send correct image size metadata after processing image`() = runTestWithCoroutineScope {
        val image = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).toByteArrayFlow()
        val fileContent =
            object : FileDescriptor {
                override val fileName: String = "numbers.jpg"
                override val fileSize: Long = 10
                override val mimeType: ContentType = ContentType.Image.JPEG
                override val content: ByteArrayFlow = image
            }
        val builder = MessageBuilder(roomId, matrixClientMock.room, matrixClientMock.media, me)
        val progressedSize = MutableStateFlow<Long?>(null)
        everySuspend { mediaServiceMock.prepareUploadMedia(any(), any()) } returns ""
        everySuspend { matrixClientMock.room.sendMessage(any(), any(), any()) } calls
            {
                val builderFunction = it.arg<suspend MessageBuilder.() -> Unit>(2)
                val messageContent = builder.build(builderFunction)
                assertIs<RoomMessageEventContent.FileBased.Image>(messageContent)
                progressedSize.value = messageContent.info?.size
                ""
            }
        val cut = sendAttachmentViewModel(fileContent, 60.mb())
        cut.sendEnabled.first { it }
        cut.send()
        delay(500.milliseconds)
        assertEquals(6, progressedSize.value)
    }

    private fun TestScope.sendAttachmentViewModel(
        content: FileDescriptor? = null,
        fileSize: Long,
        contentType: ContentType = ContentType.Any,
    ): SendAttachmentViewModelImpl =
        SendAttachmentViewModelImpl(
            viewModelContext =
                MatrixClientViewModelContextImpl(
                    componentContext = DefaultComponentContext(LifecycleRegistry()),
                    coroutineContext = backgroundScope.coroutineContext,
                    userId = me,
                    di =
                        koinApplication {
                                modules(
                                    createTestDefaultTrixnityMessengerModules(mapOf(me to matrixClientMock)) +
                                        module {
                                            single<MatrixMessengerConfiguration> { MatrixMessengerConfiguration() }
                                            single<RoomService> { roomServiceMock }
                                            single<MediaService> { mediaServiceMock }
                                            single<ProcessImageUpload> { imageProcessMock }
                                            single<GetImageDimensions> { getImageDimensionsMock }
                                            single<MatrixClient> { matrixClientMock }
                                        }
                                )
                            }
                            .koin,
                    name = "MatrixClient",
                ),
            file =
                content
                    ?: object : FileDescriptor {
                        override val content: ByteArrayFlow =
                            ByteArray(fileSize.toInt()) { it.toByte() }.toByteArrayFlow()
                        override val fileName: String = "test.txt"
                        override val fileSize: Long = fileSize
                        override val mimeType: ContentType = contentType
                    },
            selectedRoomId = roomId,
            onCloseAttachmentSendView = {},
        )
}
