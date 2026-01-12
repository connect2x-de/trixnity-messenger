package de.connect2x.messenger.media

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.theme.components.ThemedSlider
import de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.formatDuration
import kotlin.time.Duration.Companion.milliseconds

@Stable
interface AudioPlayerView {
    @Composable
    fun Create(
        audio: RoomMessageTimelineElementViewModel.FileBased.Audio,
        viewModel: MediaPlayerViewModel,
        fallbackView: @Composable () -> Unit
    )
}

class AudioPlayerViewImpl : AudioPlayerView {
    @Composable
    override fun Create(
        audio: RoomMessageTimelineElementViewModel.FileBased.Audio,
        viewModel: MediaPlayerViewModel,
        fallbackView: @Composable () -> Unit
    ) {
        when (viewModel.state.collectAsState().value) {
            is MediaPlayerViewModel.State.Ready -> PlayableAudioMessage(audio, viewModel)
            is MediaPlayerViewModel.State.Playing -> PlayableAudioMessage(audio, viewModel)
            is MediaPlayerViewModel.State.Failed, is MediaPlayerViewModel.State.NotAvailable -> fallbackView()
            is MediaPlayerViewModel.State.Loading -> fallbackView() // TODO
        }
    }
}

@Composable
private fun PlayableAudioMessage(
    audio: RoomMessageTimelineElementViewModel.FileBased.Audio,
    viewModel: MediaPlayerViewModel
) {
    val isPlaying = viewModel.state.collectAsState().value is MediaPlayerViewModel.State.Playing
    val duration = viewModel.duration.collectAsState().value
    val elapsedTime = viewModel.elapsedTime.collectAsState().value

    Column(modifier = Modifier.padding(4.dp)) {
        Row {
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
            Spacer(Modifier.width(5.dp))
            Column {
                Text(
                    text = audio.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatDuration(duration)}${audio.size ?: ""}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        ThemedSlider(
            modifier = Modifier.width(250.dp),
            valueRange = 0F..1F,
            value = (elapsedTime / duration).let {
                if (it.isNaN()) 0 else it
            }.toFloat(),
            onValueChange = {
                val elapsedTime = duration.inWholeMilliseconds * it
                viewModel.seekTo(elapsedTime.toLong().milliseconds)
            }
        )
    }
}
