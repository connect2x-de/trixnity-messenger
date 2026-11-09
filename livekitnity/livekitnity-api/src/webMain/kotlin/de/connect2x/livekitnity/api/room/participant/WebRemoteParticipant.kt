package de.connect2x.livekitnity.api.room.participant

import de.connect2x.livekitnity.api.room.track.WebRemoteTrackPublication
import kotlinx.coroutines.flow.StateFlow

public interface WebRemoteParticipant : RemoteParticipant, WebParticipant {
  override val trackPublications: StateFlow<Map<String, WebRemoteTrackPublication>>
}
