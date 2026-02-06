package de.connect2x.messenger.compose.view.media

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.theme.components.ThemedSlider
import de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.formatDuration
import kotlin.time.Duration
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
        when (val state = viewModel.state.collectAsState().value) {
            is MediaPlayerViewModel.State.Ready -> PlayableAudioMessage(audio, viewModel)
            is MediaPlayerViewModel.State.Playing -> PlayableAudioMessage(audio, viewModel)
            is MediaPlayerViewModel.State.NotReady -> fallbackView()
            is MediaPlayerViewModel.State.Failure -> Text(state.cause)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayableAudioMessage(
    audio: RoomMessageTimelineElementViewModel.FileBased.Audio,
    viewModel: MediaPlayerViewModel
) {
    val isPlaying = viewModel.state.collectAsState().value is MediaPlayerViewModel.State.Playing
    val duration = audio.duration ?: viewModel.duration.collectAsState().value
    val elapsedTime = viewModel.elapsedTime.collectAsState().value

    Row(
        modifier = Modifier.padding(4.dp).padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(5.dp))
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
            val playedColor = MaterialTheme.colorScheme.onBackground
            val sliderInteractionSource = remember { MutableInteractionSource() }
            val sliderColors = MaterialTheme.components.slider.colors.copy(
                thumbColor = playedColor,
                activeTrackColor = playedColor
            )
            ThemedSlider(
                modifier = Modifier.width(250.dp),
                valueRange = 0F..1F,
                value = (elapsedTime / duration).let {
                    if (it.isNaN()) 0 else it
                }.toFloat(),
                onValueChange = {
                    val elapsedTime = duration.inWholeMilliseconds * it
                    viewModel.seekTo(elapsedTime.toLong().milliseconds)
                },

                // Slider theming
                interactionSource = sliderInteractionSource,
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = sliderInteractionSource,
                        colors = sliderColors,
                        thumbSize = DpSize(4.dp, 25.dp)
                    )
                },
                style = MaterialTheme.components.slider.copy(
                    colors = sliderColors
                )
            )

            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = formatDuration(elapsedTime),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (duration == Duration.ZERO) "--:--" else formatDuration(duration),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
