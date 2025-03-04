package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.capture.Capture
import dev.mokkery.matcher.capture.capture
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verify
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.MediaService
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.ThumbnailInfo
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArray
import okio.fakefilesystem.FakeFileSystem
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext


@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class AvatarCutterViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val matrixClientMock = mock<MatrixClient>()

    val mediaServiceMock = mock<MediaService>()

    val fileDescriptorMock = mock<FileDescriptor>()

    private val onCloseMock = mock<Function0<Unit>>()

    private lateinit var fakeFileSystem: FakeFileSystem


    init {
        coroutineTestScope = true

        beforeTest {
            resetMocks(matrixClientMock, mediaServiceMock, fileDescriptorMock, onCloseMock)
            fakeFileSystem = FakeFileSystem()

            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { mediaServiceMock }
                    }
                )
            }.koin

            should("try to upload image") {
                val thumbnailCapture = Capture.slot<ByteArrayFlow>()
                everySuspend {
                    mediaServiceMock.prepareUploadMedia(
                        capture(thumbnailCapture),
                        any()
                    )
                } returns "cache://localhost/123456"
                everySuspend {
                    mediaServiceMock.uploadMedia(eq("cache://localhost/123456"), any(), any())
                } returns Result.success("mxc://localhost/123456")
                everySuspend {
                    matrixClientMock.setAvatarUrl("mxc://localhost/123456")
                } returns Result.success(Unit)
                every { onCloseMock.invoke() } returns Unit

                val cut = avatarCutterViewModel(coroutineContext)
                cut.upload.value shouldBe false
                cut.accept()
                testCoroutineScheduler.advanceUntilIdle()

                cut.upload.value shouldBe true
                cut.error.value shouldBe null
                verify { onCloseMock.invoke() }
                thumbnailCapture.values.first().toByteArray() shouldBe "image".encodeToByteArray()
                cancelNeverEndingCoroutines()
            }

            should("display error message when uploading fails") {
                var onCloseWasCalled = false
                val thumbnailCapture = mutableListOf<ByteArrayFlow>()
                everySuspend {
                    mediaServiceMock.prepareUploadMedia(
                        capture(thumbnailCapture),
                        any()
                    )
                } returns "cache://localhost/123456"
                everySuspend {
                    mediaServiceMock.uploadMedia(eq("cache://localhost/123456"), any(), any())
                } returns Result.failure(RuntimeException("Oh no!"))
                every { onCloseMock.invoke() } calls {
                    onCloseWasCalled = true
                }

                val cut = avatarCutterViewModel(coroutineContext)
                cut.upload.value shouldBe false
                cut.accept()
                testCoroutineScheduler.advanceUntilIdle()

                cut.upload.value shouldBe false
                cut.error.value shouldNotBe null
                onCloseWasCalled shouldBe false

                thumbnailCapture.single().toByteArray() shouldBe "image".encodeToByteArray()

                cancelNeverEndingCoroutines()
            }
        }
    }

    private suspend fun avatarCutterViewModel(coroutineContext: CoroutineContext): AvatarCutterViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        return AvatarCutterViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(
                                UserId(
                                    "test",
                                    "server"
                                ) to matrixClientMock
                            )
                        )
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext
            ),
            file = fileDescriptorMock,
            onClose = onCloseMock,
            roomId = null,
        )
    }
}
