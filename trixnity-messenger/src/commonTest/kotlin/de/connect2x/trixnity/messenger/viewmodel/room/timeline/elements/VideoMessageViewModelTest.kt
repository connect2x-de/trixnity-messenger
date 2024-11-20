package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class VideoMessageViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val matrixClientMock = mock<MatrixClient>()

    val thumbnailsMock = mock<Thumbnails>()

    init {
        beforeTest {
            Dispatchers.setMain(Dispatchers.Unconfined)
            resetMocks(matrixClientMock, thumbnailsMock)
            every { thumbnailsMock.mapProgressToProgressElement(any()) } returns flowOf(null)
        }

        should("load a thumbnail successfully") {
            everySuspend {
                thumbnailsMock.loadThumbnail(
                    eq(matrixClientMock),
                    any<RoomMessageEventContent.FileBased.Video>(),
                    any(),
                    any()
                )
            } returns "thumbnail".encodeToByteArray()

            val cut = videoMessageViewModel(coroutineContext)
            launch { cut.thumbnail.collect {} }

            eventually(1.seconds) {
                cut.thumbnail.value shouldBe "thumbnail".encodeToByteArray()
            }

            cancelNeverEndingCoroutines()
        }

        should("load a thumbnail that takes a while to load") {
            everySuspend {
                thumbnailsMock.loadThumbnail(
                    eq(matrixClientMock),
                    any<RoomMessageEventContent.FileBased.Video>(),
                    any(),
                    any(),
                )
            } calls {
                delay(500.milliseconds)
                "thumbnail".encodeToByteArray()
            }

            val cut = videoMessageViewModel(coroutineContext)
            launch { cut.thumbnail.collect {} }

            continually(400.milliseconds) {
                cut.thumbnail.value shouldBe null
                Unit
            }

            eventually(1.seconds) {
                cut.thumbnail.value shouldBe "thumbnail".encodeToByteArray()
            }

            cancelNeverEndingCoroutines()
        }

        should("return 'null' for a thumbnail that cannot be loaded") {
            everySuspend {
                thumbnailsMock.loadThumbnail(
                    eq(matrixClientMock),
                    any<RoomMessageEventContent.FileBased.Video>(),
                    any(),
                    any(),
                )
            } returns null

            val cut = videoMessageViewModel(coroutineContext)
            launch { cut.thumbnail.collect {} }

            eventually(2.seconds) {
                cut.thumbnail.value shouldBe null
            }

            cancelNeverEndingCoroutines()
        }
    }

    private suspend fun videoMessageViewModel(coroutineContext: CoroutineContext): VideoMessageViewModelImpl {
        return VideoMessageViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock)) +
                                module {
                                    single { thumbnailsMock }
                                })
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext
            ),
            timelineEvent = null,
            content = RoomMessageEventContent.FileBased.Video(""),
            formattedDate = "21.11.2021",
            showDateAbove = true,
            formattedTime = null,
            isByMe = false,
            showChatBubbleEdge = true,
            showBigGap = true,
            showSender = MutableStateFlow(true),
            sender = MutableStateFlow(UserInfoElement("User1", UserId("user1:localhost"))),
            invitation = flowOf(null),
            onOpenModal = mock(),
            mediaUploadProgress = MutableStateFlow(null)
        )
    }
}
