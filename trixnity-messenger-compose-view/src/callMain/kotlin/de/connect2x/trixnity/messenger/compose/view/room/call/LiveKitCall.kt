@file:OptIn(TrixnityMessengerCallApi::class)

package de.connect2x.trixnity.messenger.compose.view.room.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.connect2x.livekitnity.api.LiveKit
import de.connect2x.livekitnity.compose.local.LiveKitLocal
import de.connect2x.livekitnity.compose.local.VideoTrackRendererLocal
import de.connect2x.livekitnity.compose.types.VideoTrackRenderer
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.compose.view.room.call.livekit.rememberCallState
import de.connect2x.trixnity.messenger.viewmodel.room.call.RoomCallViewModel

@Composable internal expect fun rememberLiveKit(): LiveKit

@Composable internal expect fun rememberVideoTrackRenderer(): VideoTrackRenderer

@Composable
internal actual fun LiveKitCall(viewModel: RoomCallViewModel) {
    CompositionLocalProvider(
        LiveKitLocal provides rememberLiveKit(),
        VideoTrackRendererLocal provides rememberVideoTrackRenderer(),
    ) {
        LiveKitCallImpl(viewModel)
    }
}

@Composable
private fun LiveKitCallImpl(viewModel: RoomCallViewModel) {
    when (val state = rememberCallState(viewModel)) {
        is CallState.Message -> CallMessage(Modifier.padding(CallSpacing * 2), state)
        is CallState.Active -> {
            Column(
                Modifier.fillMaxSize().padding(CallSpacing),
                verticalArrangement = Arrangement.spacedBy(CallSpacing),
            ) {
                state.messages.forEach { CallMessage(message = it) }
                CallParticipants(state, Modifier.weight(1f))
                CallControls(state.microphone, state.camera)
            }
        }
    }
}

@Composable
private fun CallMessage(modifier: Modifier = Modifier, message: CallState.Message) {
    Text(message.text, modifier, color = if (message.isError) MaterialTheme.colorScheme.error else Color.Unspecified)
}

@Composable
private fun CallParticipants(state: CallState.Active, modifier: Modifier) {
    val speakerFraction = 0.6f
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CallSpacing)) {
        Box(Modifier.fillMaxWidth().weight(speakerFraction)) {
            state.featured?.let { CallParticipantTile(Modifier.fillMaxSize(), item = it) }
        }
        Box(Modifier.fillMaxWidth().weight(1 - speakerFraction), contentAlignment = Alignment.CenterStart) {
            LazyRow(
                Modifier.fillMaxWidth().heightIn(max = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(CallSpacing),
            ) {
                items(state.thumbnails, key = { it.id }) { participant ->
                    CallParticipantTile(
                        Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true),
                        item = participant,
                    )
                }
            }
        }
    }
}

internal val CallSpacing = 8.dp
