package de.connect2x.livekitnity.api.room.participant

import de.connect2x.livekitnity.api.room.track.WebTrackPublication
import kotlinx.coroutines.flow.StateFlow

public interface WebParticipant : Participant {
  override val trackPublications: StateFlow<Map<String, WebTrackPublication>>
}
