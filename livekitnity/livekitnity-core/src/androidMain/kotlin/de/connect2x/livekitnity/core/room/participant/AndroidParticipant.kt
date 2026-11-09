package de.connect2x.livekitnity.core.room.participant

import de.connect2x.livekitnity.api.events.EventListenable
import de.connect2x.livekitnity.api.events.ParticipantEvent
import de.connect2x.livekitnity.api.room.participant.AndroidParticipant
import de.connect2x.livekitnity.api.room.participant.ConnectionQuality
import de.connect2x.livekitnity.api.room.participant.Participant
import de.connect2x.livekitnity.api.room.participant.ParticipantPermission
import de.connect2x.livekitnity.api.room.track.AndroidTrackPublication
import de.connect2x.livekitnity.core.SimpleEventListenable
import de.connect2x.livekitnity.core.mapSharedNotNull
import de.connect2x.livekitnity.core.mapState
import de.connect2x.livekitnity.core.platform.Converter
import io.livekit.android.util.flow
import kotlinx.coroutines.flow.StateFlow

internal fun AndroidParticipant(
    delegate: io.livekit.android.room.participant.Participant,
    converter: Converter,
): AndroidParticipant {
  return AndroidParticipantImpl(delegate, converter)
}

private class AndroidParticipantImpl(
    private val delegate: io.livekit.android.room.participant.Participant,
    private val converter: Converter,
) : AndroidParticipant {
  override val sid: Participant.Sid
    get() = Converter.toCommon(delegate.sid)

  override val events: EventListenable<ParticipantEvent> =
      SimpleEventListenable(
          events = delegate.events.events.mapSharedNotNull(converter::toCommon),
      )

  override val identity: StateFlow<Participant.Identity?>
    get() = delegate::identity.flow.mapState { it?.let { Converter.toCommon(it) } }

  override val audioLevel: StateFlow<Float>
    get() = delegate::audioLevel.flow

  override val isSpeaking: StateFlow<Boolean>
    get() = delegate::isSpeaking.flow

  override val name: StateFlow<String?>
    get() = delegate::name.flow

  override val metadata: StateFlow<String?>
    get() = delegate::metadata.flow

  override val attributes: StateFlow<Map<String, String>>
    get() = delegate::attributes.flow

  override val permissions: StateFlow<ParticipantPermission?>
    get() = delegate::permissions.flow.mapState { it?.let { Converter.toCommon(it) } }

  override val connectionQuality: StateFlow<ConnectionQuality>
    get() = delegate::connectionQuality.flow.mapState { Converter.toCommon(it) }

  override val lastSpokeAt: StateFlow<Long?>
    get() = delegate::lastSpokeAt.flow

  override val kind: StateFlow<Participant.Kind>
    get() = delegate::kind.flow.mapState { Converter.toCommon(it) }

  override val trackPublications: StateFlow<Map<String, AndroidTrackPublication>>
    get() = delegate::trackPublications.flow.mapState { converter.toCommon(it) }

  override val isMicrophoneEnabled: StateFlow<Boolean>
    get() = delegate::isMicrophoneEnabled.flow

  override val isCameraEnabled: StateFlow<Boolean>
    get() = delegate::isCameraEnabled.flow

  override val isScreenShareEnabled: StateFlow<Boolean>
    get() = delegate::isScreenShareEnabled.flow
}
