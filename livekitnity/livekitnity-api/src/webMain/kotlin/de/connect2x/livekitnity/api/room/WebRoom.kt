package de.connect2x.livekitnity.api.room

import de.connect2x.livekitnity.api.room.participant.Participant
import de.connect2x.livekitnity.api.room.participant.WebLocalParticipant
import de.connect2x.livekitnity.api.room.participant.WebParticipant
import de.connect2x.livekitnity.api.room.participant.WebRemoteParticipant
import kotlinx.coroutines.flow.StateFlow

public interface WebRoom : Room {
  override val localParticipant: WebLocalParticipant
  override val remoteParticipants: StateFlow<Map<Participant.Identity, WebRemoteParticipant>>
  override val activeSpeakers: StateFlow<List<WebParticipant>>
}
