package de.connect2x.livekitnity.api.room.participant

import de.connect2x.livekitnity.api.room.track.AndroidRemoteTrackPublication
import kotlinx.coroutines.flow.StateFlow

public interface AndroidRemoteParticipant : RemoteParticipant, AndroidParticipant {
  override val trackPublications: StateFlow<Map<String, AndroidRemoteTrackPublication>>
}
