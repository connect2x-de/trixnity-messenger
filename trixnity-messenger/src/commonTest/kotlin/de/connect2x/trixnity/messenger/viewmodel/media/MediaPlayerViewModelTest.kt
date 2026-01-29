package de.connect2x.trixnity.messenger.viewmodel.media

import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.media.MediaPlayer
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.util.html.HtmlNode
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration

class MediaPlayerViewModelTest {

    val mediaPlayerMock = mock<MediaPlayer>()
    val matrixClientMock = mock<MatrixClient>()

    @Test
    fun `Audio - State should be NotReady when no media player is present`() = runTest {
        val cut = requireNotNull(audioWithMediaPlayerViewModel().audioPlayer)
        cut.state.value shouldBe MediaPlayerViewModel.State.NotReady
    }

    @Test
    fun `Audio - State should be Ready when media player is present`() = runTest {
        mockMediaPlayer()
        val cut = requireNotNull(audioWithMediaPlayerViewModel(mediaPlayer = mediaPlayerMock).audioPlayer)
        cut.state.value shouldBe MediaPlayerViewModel.State.Ready
    }

    @Test
    fun `Audio - ViewModel should be able to play and stop media item`() = runTest {
        mockMediaPlayer()
        val cut = requireNotNull(audioWithMediaPlayerViewModel(mediaPlayer = mediaPlayerMock).audioPlayer)
        cut.state.value shouldBe MediaPlayerViewModel.State.Ready
        cut.start()
        delay(100)

        cut.state.value shouldBe MediaPlayerViewModel.State.Playing
        cut.stop()
        delay(100)

        cut.state.value shouldBe MediaPlayerViewModel.State.Ready
    }

    private fun TestScope.audioWithMediaPlayerViewModel(
        initialDuration: Duration? = null,
        mimeType: String = "audio/raw",
        mediaPlayer: MediaPlayer? = null
    ): RoomMessageTimelineElementViewModel.FileBased.Audio =
        object : RoomMessageTimelineElementViewModel.FileBased.Audio {
            override val duration: Duration? = initialDuration
            override val name: String = "media_to_test"
            override val size: String? = null
            override val mimeType: String = mimeType
            override val hasCaption: Boolean = false
            override val loadMediaResult: StateFlow<ByteArray?> = MutableStateFlow(null)
            override val loadMediaResultPlatformMedia: StateFlow<PlatformMedia?> = MutableStateFlow(null)
            override val loadMediaResultBytes: StateFlow<ByteArray?> = MutableStateFlow(null)
            override val loadMediaProgress: StateFlow<FileTransferProgressElement?> = MutableStateFlow(null)
            override val loadMediaError: StateFlow<String?> = MutableStateFlow(null)
            override val downloadMediaResult: StateFlow<PlatformMedia?> = MutableStateFlow(null)
            override val downloadMediaProgress: StateFlow<FileTransferProgressElement?> = MutableStateFlow(null)
            override val downloadMediaError: StateFlow<String?> = MutableStateFlow(null)
            override val body: String = "some_content"
            override val formattedBody: String? = null
            override val formattedBodyContent: HtmlNode.HtmlElement? = null
            override val mentionsInBody: Map<IntRange, StateFlow<TimelineElementMention?>> = emptyMap()
            override val mentionsInFormattedBody: StateFlow<Map<String, TimelineElementMention?>> =
                MutableStateFlow(mapOf())

            override val audioPlayer: MediaPlayerViewModel = MediaPlayerViewModelImpl(
                viewModelContext = testMatrixClientViewModelContext(
                    userId = UserId("test", "server"),
                    di = koinApplication {
                        modules(
                            createTestDefaultTrixnityMessengerModules(
                                mapOf(UserId("test", "server") to matrixClientMock)
                            ) + listOf(
                                module {
                                    if (mediaPlayer != null) {
                                        single<MediaPlayer> { mediaPlayer }
                                    }
                                }
                            )
                        )
                    }.koin
                ),
                media = this,
                initialDurationOptional = initialDuration,
            )

            override fun loadMedia() {}
            override fun cancelLoadMedia() {}
            override fun downloadMedia(processFile: suspend (PlatformMedia) -> Unit, onDownloadCancelled: () -> Unit) {}
            override fun cancelDownloadMedia() {}
            override fun openMention(mention: TimelineElementMention) {}
        }

    private fun mockMediaPlayer() {
        val currentItem = MutableStateFlow<MediaPlayerItem?>(null)
        every { mediaPlayerMock.playingItem } returns currentItem
        every { mediaPlayerMock.open(any()) } returns MediaPlayerItem(currentItem)
    }

    private class MediaPlayerItem(private val currentItem: MutableStateFlow<MediaPlayerItem?>) : MediaPlayer.Item {
        override val isPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
        override val duration: MutableStateFlow<Duration?> = MutableStateFlow(null)
        override val elapsedTime: MutableStateFlow<Duration?> = MutableStateFlow(null)
        override val state: StateFlow<MediaPlayer.State> = MutableStateFlow(MediaPlayer.State.Ready)

        override suspend fun play(startPosition: Duration?) {
            currentItem.value?.pause()
            isPlaying.value = true
            currentItem.value = this
        }

        override suspend fun pause() {
            isPlaying.value = false
            currentItem.value = null
        }

        override suspend fun seekTo(position: Duration) {
            elapsedTime.value = position
        }

        override fun close() {}
    }
}