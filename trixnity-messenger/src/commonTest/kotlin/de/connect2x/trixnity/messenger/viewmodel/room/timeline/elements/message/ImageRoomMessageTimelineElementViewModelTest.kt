package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
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
import io.kotest.core.test.TestScope
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ImageRoomMessageTimelineElementViewModelTest : ShouldSpec() {
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
                    any<RoomMessageEventContent.FileBased.Image>(),
                    any(),
                    any()
                )
            } returns "thumbnail".encodeToByteArray()

            val cut = imageMessageViewModel()
            launch { cut.thumbnail.collect {} }

            eventually(2.seconds) {
                cut.thumbnail.value shouldBe "thumbnail".encodeToByteArray()
            }

            cancelNeverEndingCoroutines()
        }

        should("load a thumbnail that takes a while to load") {
            everySuspend {
                thumbnailsMock.loadThumbnail(
                    eq(matrixClientMock),
                    any<RoomMessageEventContent.FileBased.Image>(),
                    any(),
                    any()
                )
            } calls {
                withContext(Dispatchers.Default) {
                    delay(500.milliseconds)
                    println(" ---- RETURN")
                    "thumbnail".encodeToByteArray()
                }
            }

            val cut = imageMessageViewModel()
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
                    any<RoomMessageEventContent.FileBased.Image>(),
                    any(),
                    any()
                )
            } returns null

            val cut = imageMessageViewModel()
            launch { cut.thumbnail.collect {} }

            eventually(1.seconds) {
                cut.thumbnail.value shouldBe null
            }

            cancelNeverEndingCoroutines()
        }
    }

    private fun TestScope.imageMessageViewModel(): ImageRoomMessageTimelineElementViewModelImpl {
        return ImageRoomMessageTimelineElementViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock))
                                + module {
                            single { thumbnailsMock }
                        })
                }.koin,
                userId = UserId("test", "server"),
            ),
            content = RoomMessageEventContent.FileBased.Image(""),
        )
    }
}
