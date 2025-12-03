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
import de.connect2x.messenger.compose.view.common.modifier.customClickable
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.util.AudioWaveform
import de.connect2x.trixnity.messenger.viewmodel.media.AudioPlayerViewModel
import kotlin.time.Duration.Companion.milliseconds

@Stable
interface AudioPlayerView {
    @Composable
    fun Create(audioPlayerViewModel: AudioPlayerViewModel, fallbackView: @Composable () -> Unit)
}

class AudioPlayerViewImpl : AudioPlayerView {
    @Composable
    override fun Create(audioPlayerViewModel: AudioPlayerViewModel, fallbackView: @Composable () -> Unit) {
        when (val playerState = audioPlayerViewModel.state.collectAsState().value) {
            is AudioPlayerViewModel.State.Ready -> PlayableAudioMessage(audioPlayerViewModel, playerState.amplitudes)
            is AudioPlayerViewModel.State.Playing -> PlayableAudioMessage(audioPlayerViewModel, playerState.amplitudes)
            is AudioPlayerViewModel.State.Failed, is AudioPlayerViewModel.State.NotAvailable -> fallbackView()
            is AudioPlayerViewModel.State.Loading -> fallbackView() // TODO
        }
    }
}

@Composable
private fun PlayableAudioMessage(audioPlayerViewModel: AudioPlayerViewModel, amplitudes: List<Float>) {
    val isPlaying = audioPlayerViewModel.state.collectAsState().value is AudioPlayerViewModel.State.Playing
    val duration = audioPlayerViewModel.duration.collectAsState().value.inWholeMilliseconds
    val elapsedTime = audioPlayerViewModel.elapsedTime.collectAsState().value.inWholeMilliseconds
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
                    audioPlayerViewModel.stop()
                } else {
                    audioPlayerViewModel.start()
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
                audioPlayerViewModel.seekTo((duration * percentage).toLong().milliseconds)
            },
            progress = progress,
            amplitudes = amplitudes,
            width = 400.dp,
            height = 75.dp
        )
    }
}
