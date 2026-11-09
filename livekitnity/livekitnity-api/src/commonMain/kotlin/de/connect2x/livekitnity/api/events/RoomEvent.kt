package de.connect2x.livekitnity.api.events

import de.connect2x.livekitnity.api.room.DisconnectReason
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.api.room.participant.ConnectionQuality
import de.connect2x.livekitnity.api.room.participant.LocalParticipant
import de.connect2x.livekitnity.api.room.participant.Participant
import de.connect2x.livekitnity.api.room.participant.ParticipantPermission
import de.connect2x.livekitnity.api.room.participant.RemoteParticipant
import de.connect2x.livekitnity.api.room.track.LocalTrackPublication
import de.connect2x.livekitnity.api.room.track.RemoteTrackPublication
import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.api.room.track.TrackPublication

public sealed class RoomEvent(public val room: Room) : Event() {

  public class Connected(room: Room) : RoomEvent(room)

  public class Reconnecting(room: Room) : RoomEvent(room)

  public class Reconnected(room: Room) : RoomEvent(room)

  public class Disconnected(
      room: Room,
      public val error: Exception?,
      public val reason: DisconnectReason,
  ) : RoomEvent(room)

  public class ParticipantConnected(room: Room, public val participant: RemoteParticipant) :
      RoomEvent(room)

  public class ParticipantDisconnected(room: Room, public val participant: RemoteParticipant) :
      RoomEvent(room)

  public class ActiveSpeakersChanged(room: Room, public val speakers: List<Participant>) :
      RoomEvent(room)

  public class RoomMetadataChanged(
      room: Room,
      public val newMetadata: String?,
  ) : RoomEvent(room)

  public class ParticipantMetadataChanged(
      room: Room,
      public val participant: Participant,
      public val prevMetadata: String?,
  ) : RoomEvent(room)

  public class ParticipantAttributesChanged(
      room: Room,
      public val participant: Participant,
      public val changedAttributes: Map<String, String>,
      // public val oldAttributes: Map<String, String>,
  ) : RoomEvent(room)

  public class ParticipantNameChanged(
      room: Room,
      public val participant: Participant,
      public val name: String?,
  ) : RoomEvent(room)

  public class TrackMuted(
      room: Room,
      public val publication: TrackPublication,
      public val participant: Participant,
  ) : RoomEvent(room)

  public class TrackUnmuted(
      room: Room,
      public val publication: TrackPublication,
      public val participant: Participant,
  ) : RoomEvent(room)

  public class LocalTrackSubscribed(
      room: Room,
      public val publication: LocalTrackPublication,
      public val participant: LocalParticipant,
  ) : RoomEvent(room)

  public class TrackPublished(
      room: Room,
      public val publication: TrackPublication,
      public val participant: Participant,
  ) : RoomEvent(room)

  public class TrackUnpublished(
      room: Room,
      public val publication: TrackPublication,
      public val participant: Participant,
  ) : RoomEvent(room)

  public class TrackSubscribed(
      room: Room,
      public val track: Track,
      public val publication: TrackPublication,
      public val participant: RemoteParticipant,
  ) : RoomEvent(room)

  public class TrackSubscriptionFailed(
      room: Room,
      public val sid: String,
      public val exception: Exception,
      public val participant: RemoteParticipant,
  ) : RoomEvent(room)

  public class TrackUnsubscribed(
      room: Room,
      public val track: Track,
      public val publications: TrackPublication,
      public val participant: RemoteParticipant,
  ) : RoomEvent(room)

  public class TrackStreamStateChanged(
      room: Room,
      public val trackPublication: TrackPublication,
      public val streamState: Track.StreamState,
  ) : RoomEvent(room)

  public class TrackSubscriptionPermissionChanged(
      room: Room,
      public val participant: RemoteParticipant,
      public val trackPublication: RemoteTrackPublication,
      public val subscriptionAllowed: Boolean,
  ) : RoomEvent(room)

  public class ConnectionQualityChanged(
      room: Room,
      public val participant: Participant,
      public val quality: ConnectionQuality,
  ) : RoomEvent(room)

  public class ParticipantPermissionsChanged(
      room: Room,
      public val participant: Participant,
      public val oldPermissions: ParticipantPermission?,
  ) : RoomEvent(room)

  public class RecordingStatusChanged(room: Room, public val isRecording: Boolean) : RoomEvent(room)

  public class EncryptionError(
      room: Room,
      public val error: Throwable,
      public val participant: Participant?,
  ) : RoomEvent(room)

  public class ParticipantActive(
      room: Room,
      public val participant: Participant,
  ) : RoomEvent(room)
}
