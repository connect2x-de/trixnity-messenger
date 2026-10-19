package de.connect2x.livekitnity.core.room.participant

import de.connect2x.livekitnity.api.events.EventListenable
import de.connect2x.livekitnity.api.events.ParticipantEvent
import de.connect2x.livekitnity.api.room.participant.ConnectionQuality
import de.connect2x.livekitnity.api.room.participant.Participant
import de.connect2x.livekitnity.api.room.participant.ParticipantPermission
import de.connect2x.livekitnity.api.room.participant.WebParticipant
import de.connect2x.livekitnity.api.room.track.WebTrackPublication
import de.connect2x.livekitnity.core.SimpleEventListenable
import de.connect2x.livekitnity.core.platform.Converter
import de.connect2x.livekitnity.core.platform.EventDescriptors
import de.connect2x.livekitnity.core.platform.EventSharedFlow
import de.connect2x.livekitnity.core.platform.EventStateFlow
import de.connect2x.livekitnity.core.platform.ParticipantCtx
import de.connect2x.livekitnity.core.platform.StateDescriptors
import kotlinx.coroutines.flow.StateFlow

internal fun WebParticipant(
    participant: livekit.Participant,
    converter: Converter,
): WebParticipant {
  return WebParticipantImpl(
      participant = participant,
      converter = converter,
  )
}

private class WebParticipantImpl(
    participant: livekit.Participant,
    private val converter: Converter,
) : WebParticipant {

  override val events: EventListenable<ParticipantEvent> =
      SimpleEventListenable(
          EventSharedFlow(
              makeCtx = { onEvent ->
                ParticipantCtx(
                    participant = this,
                    eventEmitter = participant,
                    converter = converter,
                    emit = onEvent,
                )
              },
              descriptors = EventDescriptors.Participant.all,
          ),
      )

  override val sid: Participant.Sid = Converter.toParticipantSid(participant.sid)

  override val identity: StateFlow<Participant.Identity?> =
      EventStateFlow(
          eventEmitter = participant,
          converter = converter,
          descriptor = StateDescriptors.Participant.identity,
      )

  override val audioLevel: StateFlow<Float> =
      EventStateFlow(
          eventEmitter = participant,
          converter = converter,
          descriptor = StateDescriptors.Participant.audioLevel,
      )

  override val isSpeaking: StateFlow<Boolean> =
      EventStateFlow(
          eventEmitter = participant,
          converter = converter,
          descriptor = StateDescriptors.Participant.isSpeaking,
      )

  override val name: StateFlow<String?> =
      EventStateFlow(
          eventEmitter = participant,
          converter = converter,
          descriptor = StateDescriptors.Participant.name,
      )

  override val metadata: StateFlow<String?> =
      EventStateFlow(
          eventEmitter = participant,
          converter = converter,
          descriptor = StateDescriptors.Participant.metadata,
      )

  override val attributes: StateFlow<Map<String, String>> =
      EventStateFlow(
          eventEmitter = participant,
          converter = converter,
          descriptor = StateDescriptors.Participant.attributes,
      )

  override val permissions: StateFlow<ParticipantPermission?> =
      EventStateFlow(
          eventEmitter = participant,
          converter = converter,
          descriptor = StateDescriptors.Participant.permissions,
      )

  override val connectionQuality: StateFlow<ConnectionQuality> =
      EventStateFlow(
          eventEmitter = participant,
          converter = converter,
          descriptor = StateDescriptors.Participant.connectionQuality,
      )

  override val lastSpokeAt: StateFlow<Long?> =
      EventStateFlow(
          eventEmitter = participant,
          converter = converter,
          descriptor = StateDescriptors.Participant.lastSpokeAt,
      )

  override val kind: StateFlow<Participant.Kind> =
      EventStateFlow(
          eventEmitter = participant,
          converter = converter,
          descriptor = StateDescriptors.Participant.kind,
      )

  override val trackPublications: StateFlow<Map<String, WebTrackPublication>> =
      EventStateFlow(
          eventEmitter = participant,
          converter = converter,
          descriptor = StateDescriptors.Participant.trackPublications,
      )

  override val isMicrophoneEnabled: StateFlow<Boolean> =
      EventStateFlow(
          eventEmitter = participant,
          converter = converter,
          descriptor = StateDescriptors.Participant.isMicrophoneEnabled,
      )

  override val isCameraEnabled: StateFlow<Boolean> =
      EventStateFlow(
          eventEmitter = participant,
          converter = converter,
          descriptor = StateDescriptors.Participant.isCameraEnabled,
      )

  override val isScreenShareEnabled: StateFlow<Boolean> =
      EventStateFlow(
          eventEmitter = participant,
          converter = converter,
          descriptor = StateDescriptors.Participant.isScreenShareEnabled,
      )
}
