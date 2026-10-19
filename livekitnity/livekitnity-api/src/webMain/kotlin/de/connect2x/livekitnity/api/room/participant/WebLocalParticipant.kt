package de.connect2x.livekitnity.api.room.participant

import de.connect2x.livekitnity.api.room.track.WebLocalTrackPublication
import kotlinx.coroutines.flow.StateFlow

public interface WebLocalParticipant : LocalParticipant, WebParticipant {
  override val trackPublications: StateFlow<Map<String, WebLocalTrackPublication>>
}
