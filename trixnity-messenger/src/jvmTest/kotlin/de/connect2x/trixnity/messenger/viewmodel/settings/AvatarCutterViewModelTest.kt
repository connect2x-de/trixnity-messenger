package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.mock.MediaServiceMock
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
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
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class AvatarCutterViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    lateinit var mediaServiceMock: MediaService

    @Mock
    lateinit var fileDescriptorMock: FileDescriptor

    private val onCloseMock = mockFunction0<Unit>(mocker)

    private lateinit var fakeFileSystem: FakeFileSystem


    init {
        coroutineTestScope = true

        beforeTest {
            mocker.reset()
            injectMocks(mocker)
            mediaServiceMock = MediaServiceMock(mocker)
            fakeFileSystem = FakeFileSystem()

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { mediaServiceMock }
                        }
                    )
                }.koin
            }

            should("try to upload image") {
                val thumbnailCapture = mutableListOf<ByteArrayFlow>()
                with(mocker) {
                    everySuspending {
                        mediaServiceMock.prepareUploadThumbnail(
                            isAny(capture = thumbnailCapture),
                            isAny()
                        )
                    } returns Pair("cache://localhost/123456", ThumbnailInfo())
                    everySuspending {
                        mediaServiceMock.uploadMedia(isEqual("cache://localhost/123456"), isAny(), isAny())
                    } returns Result.success("mxc://localhost/123456")
                    everySuspending {
                        matrixClientMock.setAvatarUrl("mxc://localhost/123456")
                    } returns Result.success(Unit)
                    every { onCloseMock.invoke() } returns Unit
                }

                val cut = avatarCutterViewModel(coroutineContext)
                cut.upload.value shouldBe false
                cut.accept()
                testCoroutineScheduler.advanceUntilIdle()

                cut.upload.value shouldBe true
                cut.error.value shouldBe null
                mocker.verify(exhaustive = false) { onCloseMock.invoke() }
                thumbnailCapture.single().toByteArray() shouldBe "image".encodeToByteArray()
                cancelNeverEndingCoroutines()
            }

            should("display error message when uploading fails") {
                var onCloseWasCalled = false
                val thumbnailCapture = mutableListOf<ByteArrayFlow>()
                with(mocker) {
                    everySuspending {
                        mediaServiceMock.prepareUploadThumbnail(
                            isAny(capture = thumbnailCapture),
                            isAny()
                        )
                    } returns Pair("cache://localhost/123456", ThumbnailInfo())
                    everySuspending {
                        mediaServiceMock.uploadMedia(isEqual("cache://localhost/123456"), isAny(), isAny())
                    } returns Result.failure(RuntimeException("Oh no!"))
                    every { onCloseMock.invoke() } runs {
                        onCloseWasCalled = true
                    }
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
                        ) )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext
            ),
            file = fileDescriptorMock,
            onCloseMock,
            roomId = null
        )
    }
}
