package de.connect2x.trixnity.messenger.viewmodel.media

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.media.MediaPlayer
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class MediaPlayerViewModelTest {

    private val me: UserId = UserId("alice", "server")
    private val matrixClientMock: MatrixClient = mock()

    @Test
    fun `should correctly transition states when play and pause`() = runTest {
        val mediaPlayer = MediaPlayerMock(coroutineContext)
        val cut = mediaPlayerViewModel("a", "audio/mp4", mediaPlayer)

        cut.state.value shouldBe MediaPlayerViewModel.State.Ready
        mediaPlayer.playingItem.value shouldBe null

        cut.play()
        delay(100.milliseconds)
        cut.state.value shouldBe MediaPlayerViewModel.State.Playing
        mediaPlayer.playingItem.value shouldNotBe null

        cut.pause()
        delay(100.milliseconds)
        cut.state.value shouldBe MediaPlayerViewModel.State.Ready
        mediaPlayer.playingItem.value shouldBe null
    }

    private fun TestScope.mediaPlayerViewModel(
        id: String,
        mimeType: String,
        mediaPlayer: MediaPlayer
    ): MediaPlayerViewModel = MediaPlayerViewModelImpl(
        viewModelContext = testMatrixClientViewModelContext(
            userId = me,
            di = koinApplication {
                modules(
                    createTestDefaultTrixnityMessengerModules(
                        mapOf(me to matrixClientMock)
                    ) + module {
                        single<MediaPlayer> { mediaPlayer }
                    }
                )
            }.koin
        ),
        id = id,
        mimeType = mimeType,
        initialDurationOptional = null,
        acquireFile = {
            Result.success(InMemoryPlatformMedia(flowOf(ByteArray(0))))
        }
    )

}
