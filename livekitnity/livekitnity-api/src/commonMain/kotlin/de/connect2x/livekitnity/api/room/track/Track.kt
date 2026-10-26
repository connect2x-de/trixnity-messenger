package de.connect2x.livekitnity.api.room.track

import de.connect2x.livekitnity.api.events.EventListenable
import de.connect2x.livekitnity.api.events.TrackEvent
import kotlin.jvm.JvmInline
import kotlinx.coroutines.flow.StateFlow

public interface Track {
  public val events: EventListenable<TrackEvent>

  public val name: String

  public val kind: Kind

  public val sid: Sid?

  public val streamState: StateFlow<StreamState>

  public enum class Kind(public val value: String) {
    AUDIO("audio"),
    VIDEO("video"),
    UNRECOGNIZED("unrecognized"),
  }

  public enum class Source {
    UNKNOWN,
    CAMERA,
    MICROPHONE,
    SCREEN_SHARE,
    SCREEN_SHARE_AUDIO,
  }

  public enum class StreamState {
    ACTIVE,
    PAUSED,
    UNKNOWN,
  }

  @JvmInline public value class Sid(public val value: String)

  public data class Dimensions(val width: Int, val height: Int)
}
