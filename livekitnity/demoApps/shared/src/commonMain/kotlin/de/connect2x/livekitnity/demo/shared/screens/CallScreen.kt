package de.connect2x.livekitnity.demo.shared.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import de.connect2x.livekitnity.api.RoomOptions
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.compose.local.rememberLiveKitRoom
import de.connect2x.livekitnity.compose.state.rememberTrackMuted
import de.connect2x.livekitnity.compose.state.rememberTracks
import de.connect2x.livekitnity.compose.types.TrackReference
import de.connect2x.livekitnity.compose.ui.VideoTrackView
import de.connect2x.livekitnity.demo.shared.components.CallControls
import de.connect2x.livekitnity.demo.shared.viewmodels.EntryViewModel

private val roomOptions =
    RoomOptions(
        e2eeOptions = null,
    )

@Composable
internal fun CallScreen(
    connectionInfo: EntryViewModel.ConnectionInfo,
) {
  val rooms = rememberRooms(connectionInfo)
  val localParticipant = checkNotNull(rooms[connectionInfo.ownPeer]).localParticipant
  val cameraTracks = flattenedCameraTracks(rooms)

  Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    val itemSize = 400.dp
    val spacing = 8.dp
    val itemCount = cameraTracks.size.coerceAtLeast(1)

    val maxGridWidth = itemSize * itemCount + spacing * (itemCount - 1)

    LazyVerticalGrid(
        columns = GridCells.FixedSize(itemSize),
        modifier = Modifier.weight(1f).widthIn(max = maxGridWidth).fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(
                spacing,
                Alignment.CenterVertically,
            ),
        horizontalArrangement =
            Arrangement.spacedBy(
                spacing,
                Alignment.CenterHorizontally,
            ),
    ) {
      items(cameraTracks) { (trackReference, peer) ->
        val room = checkNotNull(rooms[peer])
        val borderShape = MaterialTheme.shapes.medium
        val borderColor = MaterialTheme.colorScheme.outline
        val isOwnTrack = trackReference.participant === localParticipant

        VideoTrackView(
            trackReference = trackReference,
            modifier =
                Modifier.size(
                        size = itemSize,
                    )
                    .border(
                        width = 4.dp,
                        color = borderColor,
                        shape = borderShape,
                    )
                    .clip(
                        shape = borderShape,
                    ),
            room = room,
            mirror = isOwnTrack,
        )
      }
    }

    CallControls(
        localParticipant = localParticipant,
    )
  }
}

@Composable
private fun rememberRooms(
    connectionInfo: EntryViewModel.ConnectionInfo,
): Map<EntryViewModel.Peer, Room> {
  val allPeers = remember(connectionInfo) { connectionInfo.otherPeers + connectionInfo.ownPeer }

  return allPeers.associateWith { peer ->
    key(peer) {
      val room =
          rememberLiveKitRoom(
              url = peer.url,
              token = checkNotNull(connectionInfo.tokens[peer]),
              roomOptions = roomOptions,
          )

      room
    }
  }
}

@Composable
private fun flattenedCameraTracks(
    rooms: Map<EntryViewModel.Peer, Room>,
): List<Pair<TrackReference, EntryViewModel.Peer>> {
  return rooms.flatMap { (peer, room) ->
    key(peer) {
      val trackReferences =
          rememberTracks(
                  sources = listOf(Track.Source.CAMERA, Track.Source.SCREEN_SHARE),
                  passedRoom = room,
              )
              .value

      trackReferences
          .filter { trackReference ->
            key(trackReference) { !rememberTrackMuted(trackReference).value }
          }
          .map { it to peer }
    }
  }
}
