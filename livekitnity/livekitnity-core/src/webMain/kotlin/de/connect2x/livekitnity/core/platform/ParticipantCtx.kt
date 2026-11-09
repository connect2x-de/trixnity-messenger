package de.connect2x.livekitnity.core.platform

import de.connect2x.livekitnity.api.events.ParticipantEvent
import de.connect2x.livekitnity.api.room.participant.LocalParticipant
import de.connect2x.livekitnity.api.room.participant.Participant
import de.connect2x.livekitnity.api.room.participant.RemoteParticipant
import livekit.JsRecord
import livekit.LocalTrackPublication
import livekit.ParticipantPermissions
import livekit.PermissionStatus
import livekit.RemoteTrack
import livekit.RemoteTrackPublication
import livekit.SubscriptionError
import livekit.SubscriptionErrors
import livekit.TrackPublication
import livekit.TrackStreamState

internal class ParticipantCtx(
    private val participant: Participant,
    override val eventEmitter: livekit.Participant,
    private val converter: Converter,
    private val emit: (ParticipantEvent) -> Unit,
) : EventCtx<livekit.Participant, ParticipantEvent> {

  override fun onEvent(event: ParticipantEvent): Unit {
    emit(event)
  }

  fun metadataChanged(
      prevMetadata: JsString?,
      source: JsAny,
  ): ParticipantEvent {
    return ParticipantEvent.MetadataChanged(
        participant,
        prevMetadata?.let(Converter::toKotlinString),
    )
  }

  fun nameChanged(name: JsString): ParticipantEvent {
    return ParticipantEvent.NameChanged(
        participant,
        Converter.toKotlinString(name),
    )
  }

  fun attributesChanged(
      changedAttributes: JsRecord<JsString>,
  ): ParticipantEvent {
    return ParticipantEvent.AttributesChanged(
        participant,
        Converter.toCommon(changedAttributes),
    )
  }

  fun speakingChanged(isSpeaking: JsBoolean): ParticipantEvent {
    return ParticipantEvent.SpeakingChanged(
        participant,
        isSpeaking.toBoolean(),
    )
  }

  fun trackMuted(publication: TrackPublication): ParticipantEvent {
    return ParticipantEvent.TrackMuted(
        participant,
        converter.toCommon(publication),
    )
  }

  fun trackUnmuted(publication: TrackPublication): ParticipantEvent {
    return ParticipantEvent.TrackUnmuted(
        participant,
        converter.toCommon(publication),
    )
  }

  fun localTrackSubscribed(
      publication: LocalTrackPublication,
  ): ParticipantEvent {
    return ParticipantEvent.LocalTrackSubscribed(
        participant as LocalParticipant,
        converter.toCommon(publication),
    )
  }

  fun localTrackPublished(
      publication: LocalTrackPublication,
  ): ParticipantEvent {
    return ParticipantEvent.LocalTrackPublished(
        participant as LocalParticipant,
        converter.toCommon(publication),
    )
  }

  fun localTrackUnpublished(
      publication: LocalTrackPublication,
  ): ParticipantEvent {
    return ParticipantEvent.LocalTrackUnpublished(
        participant as LocalParticipant,
        converter.toCommon(publication),
    )
  }

  fun trackPublished(
      publication: RemoteTrackPublication,
  ): ParticipantEvent {
    return ParticipantEvent.TrackPublished(
        participant as RemoteParticipant,
        converter.toCommon(publication),
    )
  }

  fun trackUnpublished(
      publication: RemoteTrackPublication,
  ): ParticipantEvent {
    return ParticipantEvent.TrackUnpublished(
        participant as RemoteParticipant,
        converter.toCommon(publication),
    )
  }

  fun trackSubscribed(
      track: RemoteTrack,
      publication: RemoteTrackPublication,
  ): ParticipantEvent {
    return ParticipantEvent.TrackSubscribed(
        participant as RemoteParticipant,
        converter.toCommon(track, publication.trackName),
        converter.toCommon(publication),
    )
  }

  fun trackSubscriptionFailed(
      sid: JsString,
      reason: SubscriptionError?,
  ): ParticipantEvent {
    return ParticipantEvent.TrackSubscriptionFailed(
        participant as RemoteParticipant,
        Converter.toKotlinString(sid),
        Exception(
            when (reason) {
              SubscriptionErrors.SE_UNKNOWN,
              null -> "SubscriptionError: unknown"

              SubscriptionErrors.SE_CODEC_UNSUPPORTED -> "SubscriptionError: codec unsupported"

              SubscriptionErrors.SE_TRACK_NOTFOUND -> "SubscriptionError: track not found"

              else -> "SubscriptionError: unrecognized reason"
            },
        ),
    )
  }

  fun trackUnsubscribed(
      track: RemoteTrack,
      publication: RemoteTrackPublication,
  ): ParticipantEvent {
    return ParticipantEvent.TrackUnsubscribed(
        participant as RemoteParticipant,
        converter.toCommon(track, publication.trackName),
        converter.toCommon(publication),
    )
  }

  fun trackStreamStateChanged(
      publication: RemoteTrackPublication,
      streamState: TrackStreamState,
  ): ParticipantEvent {
    return ParticipantEvent.TrackStreamStateChanged(
        participant,
        converter.toCommon(publication),
        Converter.toCommon(streamState),
    )
  }

  fun trackSubscriptionPermissionChanged(
      publication: RemoteTrackPublication,
      status: PermissionStatus,
  ): ParticipantEvent {
    return ParticipantEvent.TrackSubscriptionPermissionChanged(
        participant as RemoteParticipant,
        converter.toCommon(publication),
        Converter.toCommon(status),
    )
  }

  fun permissionsChanged(
      prevPermissions: ParticipantPermissions?,
  ): ParticipantEvent {
    return ParticipantEvent.ParticipantPermissionsChanged(
        participant,
        eventEmitter.permissions?.let(Converter::toCommon),
        prevPermissions?.let(Converter::toCommon),
    )
  }
}
