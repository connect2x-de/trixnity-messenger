package de.connect2x.trixnity.messenger.viewmodel.media

import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.messenger.media.AbstractMediaItem
import de.connect2x.trixnity.messenger.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class MediaPlayerMock(private val coroutineContext: CoroutineContext) : MediaPlayer {
    override val playingItem: MutableStateFlow<AbstractMediaItem?> = MutableStateFlow(null)
    private val operationMutex: Mutex = Mutex()

    override suspend fun open(
        id: String,
        media: PlatformMedia,
        mimeType: String,
        lifecycleScope: CoroutineScope?
    ): Result<MediaPlayer.Item> = Result.success(MediaItemMock(
        id = id,
        coroutineScope = CoroutineScope(coroutineContext + SupervisorJob()),
        operationMutex = operationMutex,
        currentItemPlaying = playingItem
    ))

    override fun close() = Unit

    internal class MediaItemMock(
        override val id: String,
        coroutineScope: CoroutineScope,
        operationMutex: Mutex,
        currentItemPlaying: MutableStateFlow<AbstractMediaItem?>
    ) : AbstractMediaItem(coroutineScope, operationMutex, currentItemPlaying) {
        override val duration: Duration = 10.seconds

        override fun onPlay(duration: Duration): Result<Unit> = Result.success(Unit)
        override fun onSeekTo(position: Duration) = Unit
        override fun onPause() = Unit
        override suspend fun onClose() = Unit

    }

}
