package de.connect2x.livekitnity.api.room.participant

import de.connect2x.livekitnity.api.room.track.AndroidLocalTrackPublication
import kotlinx.coroutines.flow.StateFlow

public interface AndroidLocalParticipant : LocalParticipant, AndroidParticipant {
  override val trackPublications: StateFlow<Map<String, AndroidLocalTrackPublication>>
}
