package de.connect2x.livekitnity.api.room.participant

import de.connect2x.livekitnity.api.events.EventListenable
import de.connect2x.livekitnity.api.events.ParticipantEvent
import de.connect2x.livekitnity.api.room.track.TrackPublication
import kotlin.jvm.JvmInline
import kotlinx.coroutines.flow.StateFlow

public interface Participant {

  public val events: EventListenable<ParticipantEvent>

  public val sid: Sid

  public val identity: StateFlow<Identity?>

  public val audioLevel: StateFlow<Float>

  public val isSpeaking: StateFlow<Boolean>

  public val name: StateFlow<String?>

  public val metadata: StateFlow<String?>

  public val attributes: StateFlow<Map<String, String>>

  public val permissions: StateFlow<ParticipantPermission?>

  public val connectionQuality: StateFlow<ConnectionQuality>

  public val lastSpokeAt: StateFlow<Long?>

  public val kind: StateFlow<Kind>

  public val trackPublications: StateFlow<Map<String, TrackPublication>>

  public val isMicrophoneEnabled: StateFlow<Boolean>

  public val isCameraEnabled: StateFlow<Boolean>

  public val isScreenShareEnabled: StateFlow<Boolean>

  @JvmInline public value class Identity(public val value: String)

  @JvmInline public value class Sid(public val value: String)

  public enum class Kind {
    AGENT,
    STANDARD,
    INGRESS,
    EGRESS,
    SIP,
    CONNECTOR,
    BRIDGE,
    UNKNOWN,
  }

  public enum class State {
    JOINING,
    JOINED,
    ACTIVE,
    DISCONNECTED,
    UNKNOWN,
  }
}
