package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.client.media.opfs.OpfsPlatformMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import web.html.Audio
import web.html.play
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class WebPlayerItem(
    override val id: String,
    override val duration: Duration,
    private val tempFile: PlatformMedia.TemporaryFile,
    private val coroutineScope: CoroutineScope,
    private val player: WebMediaPlayer,
    private val audio: Audio,
) : AbstractMediaItem(coroutineScope, player.playerMutex, player.currentItemPlaying) {
    private var updateJob: Job? = null

    override suspend fun onPlay(duration: Duration): Result<Unit> {
        player.resumeAudioContext()

        onSeekTo(duration)
        updateJob = repeatedlyUpdateElapsedTime()
        audio.play()
        return Result.success(Unit)
    }

    override suspend fun onSeekTo(position: Duration) {
        player.resumeAudioContext()

        log.debug { "Seeking media playback to position $position" }
        audio.currentTime = position.toDouble(DurationUnit.SECONDS)
    }

    override suspend fun onPause() {
        player.resumeAudioContext()

        audio.pause()
        updateJob?.cancel()
        updateJob = null
    }

    override suspend fun onClose() {
        when (state.value) {
            MediaPlayer.Item.State.Playing -> pauseWithoutLock()
            is MediaPlayer.Item.State.Failed, MediaPlayer.Item.State.Ready -> Unit
        }
        tempFile.delete()
        coroutineScope.cancel()
    }

    private fun repeatedlyUpdateElapsedTime(): Job = coroutineScope.launch {
        while (isActive) {
            delay(150.milliseconds)
            elapsedTime.value = audio.currentTime.seconds
        }
    }
}
