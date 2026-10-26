package de.connect2x.livekitnity.api.room.participant

import de.connect2x.livekitnity.api.room.track.AndroidTrackPublication
import kotlinx.coroutines.flow.StateFlow

public interface AndroidParticipant : Participant {
  override val trackPublications: StateFlow<Map<String, AndroidTrackPublication>>
}
