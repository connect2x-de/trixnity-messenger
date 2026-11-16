package de.connect2x.trixnity.messenger.compose.view.room.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.livekitnity.compose.local.VideoTrackRendererLocal
import de.connect2x.livekitnity.compose.types.VideoTrackRenderer
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallParticipant
import de.connect2x.trixnity.messenger.call.CallSessionState
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView

@Composable internal expect fun rememberVideoTrackRenderer(): VideoTrackRenderer

@OptIn(TrixnityMessengerCallApi::class)
@Composable
internal actual fun LiveKitCall(
    state: CallSessionState,
    onLeave: () -> Unit,
    onMicrophone: () -> Unit,
    onCamera: () -> Unit,
) {
    CompositionLocalProvider(VideoTrackRendererLocal provides rememberVideoTrackRenderer()) {
        LiveKitCallImpl(state, onLeave, onMicrophone, onCamera)
    }
}

@TrixnityMessengerCallApi
@Composable
private fun LiveKitCallImpl(
    state: CallSessionState,
    onLeave: () -> Unit,
    onMicrophone: () -> Unit,
    onCamera: () -> Unit,
) {
    val presentation = state.toCallState(DI.get<I18nView>())
    val active = presentation as? CallState.Active
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(CallSpacing)) {
        when (presentation) {
            is CallState.Message ->
                Box(Modifier.weight(1f).padding(CallSpacing * 2)) { CallMessage(message = presentation) }
            is CallState.Active ->
                Column(
                    Modifier.weight(1f).padding(CallSpacing),
                    verticalArrangement = Arrangement.spacedBy(CallSpacing),
                ) {
                    presentation.messages.forEach { CallMessage(message = it) }
                    CallParticipants(presentation, Modifier.weight(1f))
                }
        }
        CallControls(
            active?.microphone,
            active?.camera,
            onLeave = onLeave,
            onMicrophone = onMicrophone,
            onCamera = onCamera,
        )
    }
}

@Composable
@TrixnityMessengerCallApi
private fun CallMessage(modifier: Modifier = Modifier, message: CallState.Message) {
    Text(message.text, modifier, color = if (message.isError) MaterialTheme.colorScheme.error else Color.Unspecified)
}

@TrixnityMessengerCallApi
@Composable
private fun CallParticipants(state: CallState.Active, modifier: Modifier) {
    val participants = rememberOrderedParticipants(state)
    if (participants.isEmpty()) return
    if (participants.size <= 2) {
        CallParticipantsOverlay(participants.first(), participants.getOrNull(1), modifier)
    } else {
        CallParticipantsGrid(participants, modifier)
    }
}

@Composable
@TrixnityMessengerCallApi
private fun rememberOrderedParticipants(state: CallState.Active): List<CallParticipant> =
    remember(state.featured, state.thumbnails) {
        (listOfNotNull(state.featured) + state.thumbnails).sortedWith(
            compareBy<CallParticipant> { it.isByMe }.thenBy { it.id }
        )
    }

@Composable
@TrixnityMessengerCallApi
private fun CallParticipantsOverlay(primary: CallParticipant, secondary: CallParticipant?, modifier: Modifier) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        key(primary.id) { CallParticipantTile(Modifier.fillMaxSize(), primary) }
        secondary?.let { participant ->
            key(participant.id) {
                CallParticipantTile(
                    Modifier.align(Alignment.TopEnd)
                        .padding(CallSpacing)
                        .width(minOf(maxWidth / 2, 330.dp))
                        .aspectRatio(CallTileAspectRatio),
                    participant,
                    elevation = 8.dp,
                )
            }
        }
    }
}

@Composable
@TrixnityMessengerCallApi
private fun CallParticipantsGrid(participants: List<CallParticipant>, modifier: Modifier) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val grid = calculateParticipantGrid(participants.size, maxWidth, maxHeight)
        LazyVerticalGrid(
            columns = GridCells.Fixed(grid.columns),
            modifier = Modifier.width(grid.width),
            horizontalArrangement = Arrangement.spacedBy(CallSpacing),
            verticalArrangement = Arrangement.spacedBy(CallSpacing),
        ) {
            items(participants, key = { it.id }) { participant ->
                CallParticipantTile(Modifier.aspectRatio(CallTileAspectRatio), participant)
            }
        }
    }
}

private data class ParticipantGrid(val columns: Int, val width: Dp)

private fun calculateParticipantGrid(participantCount: Int, availableWidth: Dp, availableHeight: Dp): ParticipantGrid {
    fun rowCount(columns: Int) = (participantCount + columns - 1) / columns
    fun cellExtent(available: Dp, cells: Int) = (available - CallSpacing * (cells - 1)) / cells

    val columns =
        (1..participantCount).maxBy { columns ->
            val heightFromWidth = cellExtent(availableWidth, columns) / CallTileAspectRatio
            val heightFromRows = cellExtent(availableHeight, rowCount(columns))
            minOf(heightFromWidth, heightFromRows)
        }
    val tileHeight = cellExtent(availableHeight, rowCount(columns)).coerceAtLeast(PreferredMinCallTileHeight)
    val gridWidth = tileHeight * CallTileAspectRatio * columns + CallSpacing * (columns - 1)
    return ParticipantGrid(columns, minOf(availableWidth, gridWidth))
}

private val PreferredMinCallTileHeight = 90.dp
private const val CallTileAspectRatio = 16f / 9f

internal val CallSpacing = 8.dp
