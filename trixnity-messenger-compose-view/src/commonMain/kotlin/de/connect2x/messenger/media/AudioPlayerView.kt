package de.connect2x.messenger.media

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.util.AudioWaveform
import de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerViewModel
import kotlin.time.Duration.Companion.milliseconds

@Stable
interface AudioPlayerView {
    @Composable
    fun Create(mediaPlayerViewModel: MediaPlayerViewModel, fallbackView: @Composable () -> Unit)
}

class AudioPlayerViewImpl : AudioPlayerView {
    @Composable
    override fun Create(mediaPlayerViewModel: MediaPlayerViewModel, fallbackView: @Composable () -> Unit) {
        when (val playerState = mediaPlayerViewModel.state.collectAsState().value) {
            is MediaPlayerViewModel.State.Ready -> PlayableAudioMessage(mediaPlayerViewModel, playerState.amplitudes)
            is MediaPlayerViewModel.State.Playing -> PlayableAudioMessage(mediaPlayerViewModel, playerState.amplitudes)
            is MediaPlayerViewModel.State.Failed, is MediaPlayerViewModel.State.NotAvailable -> fallbackView()
            is MediaPlayerViewModel.State.Loading -> fallbackView() // TODO
        }
    }
}

@Composable
private fun PlayableAudioMessage(mediaPlayerViewModel: MediaPlayerViewModel, amplitudes: List<Float>) {
    val isPlaying = mediaPlayerViewModel.state.collectAsState().value is MediaPlayerViewModel.State.Playing
    val duration = mediaPlayerViewModel.duration.collectAsState().value.inWholeMilliseconds
    val elapsedTime = mediaPlayerViewModel.elapsedTime.collectAsState().value.inWholeMilliseconds
    val progress = (elapsedTime.toDouble() / duration.toDouble()).toFloat().let {
        if (it.isInfinite() || it.isNaN()) 0F else it
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(5.dp)
    ) {
        ThemedIconButton(
            onClick = {
                if (isPlaying) {
                    mediaPlayerViewModel.stop()
                } else {
                    mediaPlayerViewModel.start()
                }
            }
        ) {
            Icon(
                modifier = Modifier.size(50.dp),
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null, // TODO: Accessibility
            )
        }

        Spacer(Modifier.width(5.dp)) // TODO
        AudioWaveform(
            onPeek = { percentage ->
                mediaPlayerViewModel.seekTo((duration * percentage).toLong().milliseconds)
            },
            progress = progress,
            amplitudes = amplitudes,
            width = 400.dp,
            height = 75.dp
        )
    }
}
