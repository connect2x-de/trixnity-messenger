package de.connect2x.trixnity.messenger.viewmodel.media

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import com.arkivanov.essenty.lifecycle.stop
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.media.MediaPlayer
import de.connect2x.trixnity.messenger.testDispatcher
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.collections.plus
import kotlin.coroutines.CoroutineContext
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MediaPlayerViewModelTest {

    private val me: UserId = UserId("alice", "server")
    private val matrixClientMock: MatrixClient = mock()

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `should correctly transition states when play and pause`() = runTest {
        val mediaPlayer = MediaPlayerMock(coroutineContext)
        val cut = mediaPlayerViewModel("a", "", mediaPlayer)

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

    @Test
    fun `should correctly transition states when playing another item`() = runTest {
        val mediaPlayer = MediaPlayerMock(coroutineContext)
        val cut1 = mediaPlayerViewModel("a", "", mediaPlayer)
        val cut2 = mediaPlayerViewModel("b", "", mediaPlayer)

        // Start first item and check state of item and view models
        cut1.play()
        delay(100.milliseconds)
        mediaPlayer.playingItem.value?.id shouldBe "a"
        cut1.state.value shouldBe MediaPlayerViewModel.State.Playing
        cut2.state.value shouldBe MediaPlayerViewModel.State.Ready

        // Start second item which should result in the second getting paused
        cut2.play()
        delay(100.milliseconds)
        mediaPlayer.playingItem.value?.id shouldBe "b"
        cut1.state.value shouldBe MediaPlayerViewModel.State.Ready
        cut2.state.value shouldBe MediaPlayerViewModel.State.Playing
    }

    @Test
    fun `should stop media playback when closing item while playing`() = runTest {
        val mediaPlayer = MediaPlayerMock(coroutineContext)
        val cut = mediaPlayerViewModel("a", "", mediaPlayer)

        cut.play()
        delay(100.milliseconds)
        mediaPlayer.playingItem.value?.id shouldBe "a"
        cut.state.value shouldBe MediaPlayerViewModel.State.Playing

        mediaPlayer.playingItem.value?.close()
        delay(100.seconds)
        mediaPlayer.playingItem.value?.id shouldBe null
        cut.state.value shouldBe MediaPlayerViewModel.State.Ready
    }

    @Test
    fun `should not start playing media when failed to acquire file`() = runTest {
        val mediaPlayer = MediaPlayerMock(coroutineContext)
        val cut = mediaPlayerViewModel("a", "", mediaPlayer) { Result.failure(Exception("Failure")) }

        cut.play()
        delay(100.milliseconds)
        mediaPlayer.playingItem.value shouldBe null
        cut.state.value::class shouldBe MediaPlayerViewModel.State.Failure::class
    }

    @Test
    fun `should successfully transition between view models when updating lifecycle`() = runTest {
        val mediaPlayer = MediaPlayerMock(backgroundScope.coroutineContext)
        val viewModelContext = viewModelContext(mediaPlayer, coroutineContext + Job())
        val firstLifecycle = LifecycleRegistry().also { it.start() }
        val firstViewModelContext = viewModelContext.childContextWithOwnLifecycle("alpha", firstLifecycle)
        val cut1 = mediaPlayerViewModel("a", "", mediaPlayer, firstViewModelContext)

        // Start playback of media item and validate
        cut1.play()
        delay(100.milliseconds)
        mediaPlayer.playingItem.value?.id shouldBe "a"
        cut1.state.value shouldBe MediaPlayerViewModel.State.Playing
        val playingItem = mediaPlayer.playingItem.value as MediaPlayerMock.MediaItemMock

        // First viewmodel dies and playback still continues
        firstLifecycle.destroy()
        delay(100.milliseconds)
        mediaPlayer.playingItem.value?.id shouldBe "a"
        cut1.state.value shouldBe MediaPlayerViewModel.State.Playing

        // Initialize second view model
        val secondLifecycle = LifecycleRegistry().also { it.start() }
        val cut2Context = viewModelContext.childContextWithOwnLifecycle("beta", secondLifecycle)
        val cut2 = mediaPlayerViewModel("a", "", mediaPlayer, cut2Context)
        delay(100.milliseconds)

        playingItem.isClosed.load() shouldBe false
        cut2.state.value shouldBe MediaPlayerViewModel.State.Playing

        // Pause the playback and validate (with close test)
        cut2.pause()
        delay(100.milliseconds)
        mediaPlayer.playingItem.value shouldNotBe playingItem
        cut2.state.value shouldBe MediaPlayerViewModel.State.Ready

        secondLifecycle.destroy()
        delay(100.milliseconds)
        playingItem.isClosed.load() shouldBe true
    }

    @Test
    fun `should go into NotReady state when media player is not present`() = runTest {
        val cut = mediaPlayerViewModel("a", "", null)
        cut.state.value shouldBe MediaPlayerViewModel.State.NotReady
    }

    private fun TestScope.viewModelContext(
        mediaPlayer: MediaPlayer?,
        coroutineContext: CoroutineContext
    ): MatrixClientViewModelContext =
        testMatrixClientViewModelContext(
            coroutineContext = coroutineContext,
            userId = me,
            di = koinApplication {
                modules(
                    createTestDefaultTrixnityMessengerModules(
                        mapOf(me to matrixClientMock)
                    ) + module {
                        mediaPlayer?.let { player ->
                            single<MediaPlayer> { player }
                        }
                    }
                )
            }.koin
        )

    private fun TestScope.mediaPlayerViewModel(
        id: String,
        mimeType: String,
        mediaPlayer: MediaPlayer?,
        viewModelContext: MatrixClientViewModelContext = viewModelContext(
            mediaPlayer,
            backgroundScope.coroutineContext
        ),
        acquireFile: suspend () -> Result<PlatformMedia> = {
            Result.success(InMemoryPlatformMedia(flowOf(ByteArray(0))))
        }
    ): MediaPlayerViewModel = MediaPlayerViewModelImpl(
        viewModelContext = viewModelContext,
        id = id,
        mimeType = mimeType,
        initialDurationOptional = null,
        acquireFile = acquireFile
    )

}
