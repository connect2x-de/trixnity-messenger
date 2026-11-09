package de.connect2x.livekitnity.core.room.participant

import de.connect2x.livekitnity.api.room.participant.WebLocalParticipant
import de.connect2x.livekitnity.api.room.participant.WebParticipant
import de.connect2x.livekitnity.api.room.track.WebLocalTrackPublication
import de.connect2x.livekitnity.core.mapState
import de.connect2x.livekitnity.core.platform.Converter
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

internal fun WebLocalParticipant(
    localParticipant: livekit.LocalParticipant,
    converter: Converter,
): WebLocalParticipant {
  return WebLocalParticipantImpl(
      localParticipant = localParticipant,
      converter = converter,
  )
}

private class WebLocalParticipantImpl
private constructor(
    private val localParticipant: livekit.LocalParticipant,
    private val base: WebParticipant,
) : WebLocalParticipant, WebParticipant by base {

  constructor(
      localParticipant: livekit.LocalParticipant,
      converter: Converter,
  ) : this(
      localParticipant = localParticipant,
      base =
          WebParticipant(
              participant = localParticipant,
              converter = converter,
          ),
  )

  override val trackPublications: StateFlow<Map<String, WebLocalTrackPublication>> =
      base.trackPublications.mapState {
        it.mapValues { (_, trackPublication) -> trackPublication as WebLocalTrackPublication }
      }

  override suspend fun setCameraEnabled(enabled: Boolean): Boolean {
    return withContext(NonCancellable) {
      try {
        Converter.await(localParticipant.setCameraEnabled(enabled))
        localParticipant.isCameraEnabled == enabled
      } catch (_: Throwable) {
        false
      }
    }
  }

  override suspend fun setMicrophoneEnabled(enabled: Boolean): Boolean {
    return withContext(NonCancellable) {
      try {
        Converter.await(localParticipant.setMicrophoneEnabled(enabled))
        localParticipant.isMicrophoneEnabled == enabled
      } catch (_: Throwable) {
        false
      }
    }
  }

  override suspend fun setScreenShareEnabled(
      enabled: Boolean,
  ): Boolean {
    return withContext(NonCancellable) {
      try {
        Converter.await(localParticipant.setScreenShareEnabled(enabled))
        localParticipant.isScreenShareEnabled == enabled
      } catch (_: Throwable) {
        false
      }
    }
  }
}
