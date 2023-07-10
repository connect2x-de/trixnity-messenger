package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.VideoMessageEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction4
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class VideoMessageViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var thumbnailsMock: Thumbnails

    init {
        Dispatchers.setMain(testMainDispatcher)
        coroutineTestScope = true

        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            mocker.every { thumbnailsMock.mapProgressToProgressElement(isAny()) } returns flowOf(null)
        }

        should("load a thumbnail successfully") {
            mocker.everySuspending {
                thumbnailsMock.loadThumbnail(
                    isEqual(matrixClientMock),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny()
                )
            } returns "thumbnail".encodeToByteArray()

            val cut = videoMessageViewModel(coroutineContext)
            val subscriberJob = launch { cut.thumbnail.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.thumbnail.value shouldBe "thumbnail".encodeToByteArray()

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("load a thumbnail that takes a while to load") {
            mocker.everySuspending {
                thumbnailsMock.loadThumbnail(
                    isEqual(matrixClientMock),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny()
                )
            } runs {
                withContext(Dispatchers.Default) {
                    delay(500.milliseconds)
                    "thumbnail".encodeToByteArray()
                }
            }

            val cut = videoMessageViewModel(coroutineContext)
            val subscriberJob = launch { cut.thumbnail.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.thumbnail.value shouldBe null

            eventually(1.seconds) {
                cut.thumbnail.value shouldBe "thumbnail".encodeToByteArray()
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("return 'null' for a thumbnail that cannot be loaded") {
            mocker.everySuspending {
                thumbnailsMock.loadThumbnail(
                    isEqual(matrixClientMock),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny()
                )
            } returns null

            val cut = videoMessageViewModel(coroutineContext)
            val subscriberJob = launch { cut.thumbnail.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.thumbnail.value shouldBe null

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }

    private fun videoMessageViewModel(coroutineContext: CoroutineContext) = VideoMessageViewModelImpl(
        viewModelContext = MatrixClientViewModelContextImpl(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            di = koinApplication {
                modules(trixnityMessengerModule(), testMatrixClientModule(matrixClientMock), module {
                    single { thumbnailsMock }
                })
            }.koin,
            accountName = "test",
            coroutineContext = coroutineContext
        ),
        formattedDate = "21.11.2021",
        showDateAbove = true,
        formattedTime = null,
        isByMe = false,
        showChatBubbleEdge = true,
        showBigGap = true,
        showSender = MutableStateFlow(true),
        sender = MutableStateFlow("User1"),
        invitation = flowOf(null),
        content = VideoMessageEventContent(""),
        onOpenModal = mockFunction4(mocker),
    )
}