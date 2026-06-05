package de.connect2x.trixnity.messenger.compose.view.room.timeline

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.media.AudioPlayerView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.media.AudioRecorder
import de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.AudioRecordingAreaViewModel
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlinx.datetime.toDateTimePeriod

@TrixnityMessengerPrivateApi
interface AudioRecordingAreaView {
    @Composable fun RowScope.create(audioRecordingAreaViewModel: AudioRecordingAreaViewModel)
}

@Composable
fun RowScope.AudioRecordingArea(audioRecordingAreaViewModel: AudioRecordingAreaViewModel) {
    with(DI.get<AudioRecordingAreaView>()) { create(audioRecordingAreaViewModel) }
}

class AudioRecordingAreaViewImpl : AudioRecordingAreaView {
    @Composable
    override fun RowScope.create(audioRecordingAreaViewModel: AudioRecordingAreaViewModel) {
        val player = audioRecordingAreaViewModel.capturePlayer.collectAsState().value
        val recorderState = audioRecordingAreaViewModel.recorder?.state?.collectAsState()?.value
        val i18n = DI.get<I18nView>()

        @Composable
        fun LoudnessAnimationCircle(currentLoudness: Float) {
            Row(Modifier.fillMaxWidth().weight(1.0f, fill = true), horizontalArrangement = Arrangement.Center) {
                val nonZeroMinimumValue = 0.0001f
                var maxLoudness by remember { mutableStateOf(nonZeroMinimumValue) }
                maxLoudness = max(currentLoudness, maxLoudness)

                fun adjustedRelativeLoudness(): Float {
                    val relativeLoudness = currentLoudness / maxLoudness
                    val amplified = 1.5f * relativeLoudness
                    val bounded = min(1.0f, max(0.2f, amplified))
                    return bounded
                }

                val relativeLoudnessAnimated by
                    animateFloatAsState(
                        adjustedRelativeLoudness(),
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    )
                val color = MaterialTheme.colorScheme.tertiary
                Canvas(Modifier.fillMaxHeight().align(Alignment.CenterVertically)) {
                    val maxRadius = size.height / 2
                    drawCircle(radius = maxRadius * relativeLoudnessAnimated, color = color)
                }
            }
        }

        @Composable
        fun AudioRecorder(recordingState: AudioRecorder.State.Recording) {
            @Composable
            fun Duration(duration: Duration) {
                val secondsAfterWholeMinute = duration.toDateTimePeriod().seconds
                val zeroPrefix = if (secondsAfterWholeMinute < 10) "0" else ""
                Text(duration.inWholeMinutes.toString() + ":" + zeroPrefix + secondsAfterWholeMinute.toString())
            }

            val pulsatingRed =
                rememberInfiniteTransition()
                    .animateColor(
                        Color.Red,
                        Color.Red.copy(alpha = 0f),
                        animationSpec =
                            infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse,
                            ),
                    )
            Icon(
                Icons.Default.Circle,
                i18n.audioRecordingInProgress(),
                tint = pulsatingRed.value,
                modifier = Modifier.padding(start = 7.dp),
            )

            Duration(recordingState.duration)

            LoudnessAnimationCircle(recordingState.loudness)

            Tooltip({ Text(i18n.audioRecordingStop()) }) {
                ThemedIconButton(
                    style = MaterialTheme.components.primaryIconButton,
                    onClick = { audioRecordingAreaViewModel.recorder?.complete() },
                ) {
                    Icon(Icons.Default.Stop, i18n.audioRecordingStop())
                }
            }
        }

        @Composable
        fun AudioPlayerFallback() {
            Text(i18n.audioRecordingPreviewUnavailable())
        }

        @Composable
        fun AudioCapturePreview() {
            @Composable
            fun AudioPlayer(player: MediaPlayerViewModel) {
                DI.current
                    .get<AudioPlayerView>()
                    .CreateWithViewModelDuration(viewModel = player, fallbackView = { AudioPlayerFallback() })
            }

            @Composable
            fun SendButton(audioRecordingAreaViewModel: AudioRecordingAreaViewModel) {
                Tooltip({ Text(i18n.audioRecordingSend()) }) {
                    ThemedIconButton(
                        style = MaterialTheme.components.primaryIconButton,
                        onClick = { audioRecordingAreaViewModel.sendAudioMessage() },
                        modifier = Modifier.padding(start = 15.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, i18n.audioRecordingSend())
                    }
                }
            }

            Tooltip({ Text(i18n.audioRecordingDelete()) }) {
                ThemedIconButton(
                    style = MaterialTheme.components.destructiveIconButton,
                    onClick = { audioRecordingAreaViewModel.deleteAudioMessage() },
                ) {
                    Icon(Icons.Default.Delete, i18n.audioRecordingDelete())
                }
            }

            Box(modifier = Modifier.weight(1.0f, fill = true).height(40.dp)) {
                if (player != null) {
                    AudioPlayer(player)
                } else {
                    AudioPlayerFallback()
                }
            }

            SendButton(audioRecordingAreaViewModel)
        }

        when (recorderState) {
            is AudioRecorder.State.Recording -> AudioRecorder(recorderState)

            is AudioRecorder.State.Completed -> AudioCapturePreview()

            null,
            AudioRecorder.State.Ready -> Unit
        }
    }
}
