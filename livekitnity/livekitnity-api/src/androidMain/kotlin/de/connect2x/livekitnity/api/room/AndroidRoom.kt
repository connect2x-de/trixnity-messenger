package de.connect2x.livekitnity.api.room

import de.connect2x.livekitnity.api.room.participant.AndroidLocalParticipant
import de.connect2x.livekitnity.api.room.participant.AndroidParticipant
import de.connect2x.livekitnity.api.room.participant.AndroidRemoteParticipant
import de.connect2x.livekitnity.api.room.participant.Participant
import kotlinx.coroutines.flow.StateFlow
import livekit.org.webrtc.EglBase

public interface AndroidRoom : Room {
  public val eglBaseContext: EglBase.Context

  override val localParticipant: AndroidLocalParticipant
  override val remoteParticipants: StateFlow<Map<Participant.Identity, AndroidRemoteParticipant>>
  override val activeSpeakers: StateFlow<List<AndroidParticipant>>
}
