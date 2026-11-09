package de.connect2x.livekitnity.core.room.participant

import de.connect2x.livekitnity.api.room.participant.AndroidParticipant
import de.connect2x.livekitnity.api.room.participant.AndroidRemoteParticipant
import de.connect2x.livekitnity.api.room.track.AndroidRemoteTrackPublication
import de.connect2x.livekitnity.core.mapState
import de.connect2x.livekitnity.core.platform.Converter
import de.connect2x.livekitnity.core.room.track.AndroidRemoteTrackPublication
import kotlinx.coroutines.flow.StateFlow

internal fun AndroidRemoteParticipant(
    delegate: io.livekit.android.room.participant.RemoteParticipant,
    converter: Converter,
): AndroidRemoteParticipant {
  return AndroidRemoteParticipantImpl(AndroidParticipant(delegate, converter))
}

private class AndroidRemoteParticipantImpl(
    private val base: AndroidParticipant,
) : AndroidRemoteParticipant, AndroidParticipant by base {
  override val trackPublications: StateFlow<Map<String, AndroidRemoteTrackPublication>> =
      base.trackPublications.mapState {
        it.mapValues { (_, trackPublication) -> trackPublication as AndroidRemoteTrackPublication }
      }
}
