package de.connect2x.livekitnity.core.room.participant

import de.connect2x.livekitnity.api.room.participant.WebParticipant
import de.connect2x.livekitnity.api.room.participant.WebRemoteParticipant
import de.connect2x.livekitnity.api.room.track.WebRemoteTrackPublication
import de.connect2x.livekitnity.core.mapState
import de.connect2x.livekitnity.core.platform.Converter
import kotlinx.coroutines.flow.StateFlow

internal fun WebRemoteParticipant(
    remoteParticipant: livekit.RemoteParticipant,
    converter: Converter,
): WebRemoteParticipant {
  return WebRemoteParticipantImpl(
      remoteParticipant = remoteParticipant,
      converter = converter,
  )
}

private class WebRemoteParticipantImpl(
    private val base: WebParticipant,
) : WebRemoteParticipant, WebParticipant by base {

  constructor(
      remoteParticipant: livekit.RemoteParticipant,
      converter: Converter,
  ) : this(
      base =
          WebParticipant(
              participant = remoteParticipant,
              converter = converter,
          ),
  )

  override val trackPublications: StateFlow<Map<String, WebRemoteTrackPublication>> =
      base.trackPublications.mapState {
        it.mapValues { (_, trackPublication) -> trackPublication as WebRemoteTrackPublication }
      }
}
