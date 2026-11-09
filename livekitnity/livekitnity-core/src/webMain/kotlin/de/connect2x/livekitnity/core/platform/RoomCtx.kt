package de.connect2x.livekitnity.core.platform

import de.connect2x.livekitnity.api.events.RoomEvent
import de.connect2x.livekitnity.api.room.WebRoom
import livekit.ConnectionQuality
import livekit.DisconnectReason
import livekit.JsError
import livekit.JsRecord
import livekit.LocalParticipant
import livekit.LocalTrackPublication
import livekit.Participant
import livekit.ParticipantChoice
import livekit.ParticipantPermissions
import livekit.PermissionStatus
import livekit.RemoteParticipant
import livekit.RemoteTrack
import livekit.RemoteTrackPublication
import livekit.Room
import livekit.SubscriptionError
import livekit.SubscriptionErrors
import livekit.TrackPublication
import livekit.TrackStreamState

internal class RoomCtx(
    private val room: WebRoom,
    override val eventEmitter: Room,
    private val converter: Converter,
    private val emit: (RoomEvent) -> Unit,
) : EventCtx<Room, RoomEvent> {

  override fun onEvent(event: RoomEvent): Unit {
    emit(event)
  }

  fun connected(): RoomEvent {
    return RoomEvent.Connected(room)
  }

  fun reconnecting(): RoomEvent {
    return RoomEvent.Reconnecting(room)
  }

  fun reconnected(): RoomEvent {
    return RoomEvent.Reconnected(room)
  }

  fun disconnected(reason: DisconnectReason?): RoomEvent {
    return RoomEvent.Disconnected(
        room,
        null,
        reason?.let(Converter::toCommon)
            ?: de.connect2x.livekitnity.api.room.DisconnectReason.UNKNOWN_REASON,
    )
  }

  fun participantConnected(participant: RemoteParticipant): RoomEvent {
    return RoomEvent.ParticipantConnected(
        room,
        converter.toCommon(participant),
    )
  }

  fun participantDisconnected(
      participant: RemoteParticipant,
      reason: DisconnectReason?,
  ): RoomEvent {
    return RoomEvent.ParticipantDisconnected(
        room,
        converter.toCommon(participant),
    )
  }

  fun activeSpeakersChanged(speakers: JsArray<Participant>): RoomEvent {
    return RoomEvent.ActiveSpeakersChanged(
        room,
        Converter.asSequence(speakers).map(converter::toCommon).toList(),
    )
  }

  fun roomMetadataChanged(metadata: JsString): RoomEvent {
    return RoomEvent.RoomMetadataChanged(
        room,
        Converter.toKotlinString(metadata),
    )
  }

  fun participantMetadataChanged(
      prevMetadata: JsString?,
      participant: ParticipantChoice,
  ): RoomEvent {
    return RoomEvent.ParticipantMetadataChanged(
        room,
        converter.toCommon(participant.unsafeCast<Participant>()),
        prevMetadata?.let(Converter::toKotlinString),
    )
  }

  fun participantAttributesChanged(
      changedAttributes: JsRecord<JsString>,
      participant: ParticipantChoice,
  ): RoomEvent {
    return RoomEvent.ParticipantAttributesChanged(
        room,
        converter.toCommon(participant.unsafeCast<Participant>()),
        Converter.toCommon(changedAttributes),
    )
  }

  fun participantNameChanged(
      name: JsString,
      participant: ParticipantChoice,
  ): RoomEvent {
    return RoomEvent.ParticipantNameChanged(
        room,
        converter.toCommon(participant.unsafeCast<Participant>()),
        Converter.toKotlinString(name),
    )
  }

  fun trackMuted(
      publication: TrackPublication,
      participant: Participant,
  ): RoomEvent {
    return RoomEvent.TrackMuted(
        room,
        converter.toCommon(publication),
        converter.toCommon(participant),
    )
  }

  fun trackUnmuted(
      publication: TrackPublication,
      participant: Participant,
  ): RoomEvent {
    return RoomEvent.TrackUnmuted(
        room,
        converter.toCommon(publication),
        converter.toCommon(participant),
    )
  }

  fun localTrackSubscribed(
      publication: LocalTrackPublication,
      participant: LocalParticipant,
  ): RoomEvent {
    return RoomEvent.LocalTrackSubscribed(
        room,
        converter.toCommon(publication),
        converter.toCommon(participant),
    )
  }

  fun trackPublished(
      publication: RemoteTrackPublication,
      participant: RemoteParticipant,
  ): RoomEvent {
    return RoomEvent.TrackPublished(
        room,
        converter.toCommon(publication),
        converter.toCommon(participant),
    )
  }

  fun trackUnpublished(
      publication: RemoteTrackPublication,
      participant: RemoteParticipant,
  ): RoomEvent {
    return RoomEvent.TrackUnpublished(
        room,
        converter.toCommon(publication),
        converter.toCommon(participant),
    )
  }

  fun trackSubscribed(
      track: RemoteTrack,
      publication: RemoteTrackPublication,
      participant: RemoteParticipant,
  ): RoomEvent {
    return RoomEvent.TrackSubscribed(
        room,
        converter.toCommon(track, publication.trackName),
        converter.toCommon(publication),
        converter.toCommon(participant),
    )
  }

  fun trackSubscriptionFailed(
      sid: JsString,
      participant: RemoteParticipant,
      reason: SubscriptionError?,
  ): RoomEvent {
    return RoomEvent.TrackSubscriptionFailed(
        room,
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
        converter.toCommon(participant),
    )
  }

  fun trackUnsubscribed(
      track: RemoteTrack,
      publication: RemoteTrackPublication,
      participant: RemoteParticipant,
  ): RoomEvent {
    return RoomEvent.TrackUnsubscribed(
        room,
        converter.toCommon(track, publication.trackName),
        converter.toCommon(publication),
        converter.toCommon(participant),
    )
  }

  fun trackStreamStateChanged(
      publication: RemoteTrackPublication,
      streamState: TrackStreamState,
      participant: RemoteParticipant,
  ): RoomEvent {
    return RoomEvent.TrackStreamStateChanged(
        room,
        converter.toCommon(publication),
        Converter.toCommon(streamState),
    )
  }

  fun trackSubscriptionPermissionChanged(
      publication: RemoteTrackPublication,
      status: PermissionStatus,
      participant: RemoteParticipant,
  ): RoomEvent {
    return RoomEvent.TrackSubscriptionPermissionChanged(
        room,
        converter.toCommon(participant),
        converter.toCommon(publication),
        Converter.toCommon(status),
    )
  }

  fun connectionQualityChanged(
      quality: ConnectionQuality,
      participant: Participant,
  ): RoomEvent {
    return RoomEvent.ConnectionQualityChanged(
        room,
        converter.toCommon(participant),
        Converter.toCommon(quality),
    )
  }

  fun participantPermissionsChanged(
      prevPermissions: ParticipantPermissions?,
      participant: ParticipantChoice,
  ): RoomEvent {
    return RoomEvent.ParticipantPermissionsChanged(
        room,
        converter.toCommon(participant.unsafeCast<Participant>()),
        prevPermissions?.let(Converter::toCommon),
    )
  }

  fun recordingStatusChanged(isRecording: JsBoolean): RoomEvent {
    return RoomEvent.RecordingStatusChanged(
        room,
        isRecording.toBoolean(),
    )
  }

  fun encryptionError(
      error: JsError,
      participant: Participant?,
  ): RoomEvent {
    return RoomEvent.EncryptionError(
        room,
        error.unsafeCast<JsPromiseError>().asJsException(),
        participant?.let(converter::toCommon),
    )
  }

  fun participantActive(participant: Participant): RoomEvent {
    return RoomEvent.ParticipantActive(
        room,
        converter.toCommon(participant),
    )
  }
}
