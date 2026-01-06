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
import de.connect2x.messenger.compose.view.theme.components.ThemedSlider
import de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerViewModel
import kotlin.time.Duration.Companion.milliseconds

@Stable
interface AudioPlayerView {
    @Composable
    fun Create(viewModel: MediaPlayerViewModel, fallbackView: @Composable () -> Unit)
}

class AudioPlayerViewImpl : AudioPlayerView {
    @Composable
    override fun Create(viewModel: MediaPlayerViewModel, fallbackView: @Composable () -> Unit) {
        when (viewModel.state.collectAsState().value) {
            is MediaPlayerViewModel.State.Ready -> PlayableAudioMessage(viewModel)
            is MediaPlayerViewModel.State.Playing -> PlayableAudioMessage(viewModel)
            is MediaPlayerViewModel.State.Failed, is MediaPlayerViewModel.State.NotAvailable -> fallbackView()
            is MediaPlayerViewModel.State.Loading -> fallbackView() // TODO
        }
    }
}

@Composable
private fun PlayableAudioMessage(viewModel: MediaPlayerViewModel) {
    val isPlaying = viewModel.state.collectAsState().value is MediaPlayerViewModel.State.Playing
    val duration = viewModel.duration.collectAsState().value.inWholeMilliseconds
    val elapsedTime = viewModel.elapsedTime.collectAsState().value.inWholeMilliseconds

    Row(verticalAlignment = Alignment.CenterVertically) {
        ThemedIconButton(
            onClick = {
                if (isPlaying) {
                    viewModel.stop()
                } else {
                    viewModel.start()
                }
            }
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                modifier = Modifier.size(50.dp),
                contentDescription = null, // TODO: Accessibility
            )
        }

        Spacer(Modifier.width(5.dp)) // TODO
        ThemedSlider(
            value = elapsedTime.toFloat(),
            onValueChange = { viewModel.seekTo(it.toLong().milliseconds) },
            valueRange = 0F..duration.toFloat(),
            modifier = Modifier.width(250.dp)
        )
    }
}
