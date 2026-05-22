package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media.MediaService
import de.connect2x.trixnity.clientserverapi.model.user.ProfileField
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.utils.ByteArrayFlow
import de.connect2x.trixnity.utils.toByteArray
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.capture.Capture
import dev.mokkery.matcher.capture.capture
import dev.mokkery.mock
import dev.mokkery.verify
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class AvatarCutterViewModelTest {
    val matrixClientMock = mock<MatrixClient>()

    val mediaServiceMock = mock<MediaService>()

    val fileDescriptorMock = mock<FileDescriptor>()

    private val onCloseMock = mock<Function0<Unit>>()

    init {
        resetMocks(matrixClientMock, mediaServiceMock, fileDescriptorMock, onCloseMock)

        every { matrixClientMock.di } returns koinApplication { modules(module { single { mediaServiceMock } }) }.koin

        every { fileDescriptorMock.fileSize } returns null
        every { fileDescriptorMock.content } returns flowOf("image".toByteArray())
        every { fileDescriptorMock.mimeType } returns null
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `try to upload image`() = runTest {
        val thumbnailCapture = Capture.slot<ByteArrayFlow>()
        everySuspend { mediaServiceMock.prepareUploadMedia(capture(thumbnailCapture), any()) } returns
            "cache://localhost/123456"
        everySuspend { mediaServiceMock.uploadMedia("cache://localhost/123456", any(), any()) } returns
            Result.success("mxc://localhost/123456")
        everySuspend { matrixClientMock.setProfileField(ProfileField.AvatarUrl("mxc://localhost/123456")) } returns
            Result.success(Unit)
        every { onCloseMock.invoke() } returns Unit

        val cut = avatarCutterViewModel()
        cut.upload.value shouldBe false
        cut.accept()
        delay(100)

        cut.error.value shouldBe null
        verify { onCloseMock.invoke() }
        thumbnailCapture.values.first().toByteArray() shouldBe "image".encodeToByteArray()
    }

    @Test
    fun `display error message when uploading fails`() = runTest {
        var onCloseWasCalled = false
        val thumbnailCapture = mutableListOf<ByteArrayFlow>()
        everySuspend { mediaServiceMock.prepareUploadMedia(capture(thumbnailCapture), any()) } returns
            "cache://localhost/123456"
        everySuspend { mediaServiceMock.uploadMedia("cache://localhost/123456", any(), any()) } returns
            Result.failure(RuntimeException("Oh no!"))
        every { onCloseMock.invoke() } calls { onCloseWasCalled = true }

        val cut = avatarCutterViewModel()
        cut.upload.value shouldBe false
        cut.accept()
        delay(100)

        cut.upload.value shouldBe false
        cut.error.value shouldNotBe null
        onCloseWasCalled shouldBe false

        thumbnailCapture.single().toByteArray() shouldBe "image".encodeToByteArray()
    }

    private fun TestScope.avatarCutterViewModel(): AvatarCutterViewModelImpl {
        return AvatarCutterViewModelImpl(
            viewModelContext =
                testMatrixClientViewModelContext(
                    di =
                        koinApplication {
                                modules(
                                    createTestDefaultTrixnityMessengerModules(
                                        mapOf(UserId("test", "server") to matrixClientMock)
                                    )
                                )
                            }
                            .koin,
                    userId = UserId("test", "server"),
                ),
            file = fileDescriptorMock,
            onClose = onCloseMock,
            roomId = null,
        )
    }
}
