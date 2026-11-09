package de.connect2x.livekitnity.api.events

import de.connect2x.livekitnity.api.room.participant.LocalParticipant
import de.connect2x.livekitnity.api.room.participant.Participant
import de.connect2x.livekitnity.api.room.participant.ParticipantPermission
import de.connect2x.livekitnity.api.room.participant.RemoteParticipant
import de.connect2x.livekitnity.api.room.track.LocalTrackPublication
import de.connect2x.livekitnity.api.room.track.RemoteTrackPublication
import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.api.room.track.TrackPublication

public sealed class ParticipantEvent(public open val participant: Participant) : Event() {

  public class MetadataChanged(participant: Participant, public val prevMetadata: String?) :
      ParticipantEvent(participant)

  public class NameChanged(participant: Participant, public val name: String?) :
      ParticipantEvent(participant)

  public class AttributesChanged(
      participant: Participant,
      public val changedAttributes: Map<String, String>,
  ) : ParticipantEvent(participant)

  public class SpeakingChanged(participant: Participant, public val isSpeaking: Boolean) :
      ParticipantEvent(participant)

  public class TrackMuted(participant: Participant, public val publication: TrackPublication) :
      ParticipantEvent(participant)

  public class TrackUnmuted(participant: Participant, public val publication: TrackPublication) :
      ParticipantEvent(participant)

  public class LocalTrackSubscribed(
      override val participant: LocalParticipant,
      public val publication: LocalTrackPublication,
  ) : ParticipantEvent(participant)

  public class LocalTrackPublished(
      override val participant: LocalParticipant,
      public val publication: LocalTrackPublication,
  ) : ParticipantEvent(participant)

  public class LocalTrackUnpublished(
      override val participant: LocalParticipant,
      public val publication: LocalTrackPublication,
  ) : ParticipantEvent(participant)

  public class TrackPublished(
      override val participant: RemoteParticipant,
      public val publication: RemoteTrackPublication,
  ) : ParticipantEvent(participant)

  public class TrackUnpublished(
      override val participant: RemoteParticipant,
      public val publication: RemoteTrackPublication,
  ) : ParticipantEvent(participant)

  public class TrackSubscribed(
      override val participant: RemoteParticipant,
      public val track: Track,
      public val publication: RemoteTrackPublication,
  ) : ParticipantEvent(participant)

  public class TrackSubscriptionFailed(
      override val participant: RemoteParticipant,
      public val sid: String,
      public val exception: Exception,
  ) : ParticipantEvent(participant)

  public class TrackUnsubscribed(
      override val participant: RemoteParticipant,
      public val track: Track,
      public val publication: RemoteTrackPublication,
  ) : ParticipantEvent(participant)

  public class TrackStreamStateChanged(
      override val participant: Participant,
      public val trackPublication: TrackPublication,
      public val streamState: Track.StreamState,
  ) : ParticipantEvent(participant)

  public class TrackSubscriptionPermissionChanged(
      override val participant: RemoteParticipant,
      public val trackPublication: RemoteTrackPublication,
      public val subscriptionAllowed: Boolean,
  ) : ParticipantEvent(participant)

  public class ParticipantPermissionsChanged(
      override val participant: Participant,
      public val newPermissions: ParticipantPermission?,
      public val oldPermissions: ParticipantPermission?,
  ) : ParticipantEvent(participant)
}
