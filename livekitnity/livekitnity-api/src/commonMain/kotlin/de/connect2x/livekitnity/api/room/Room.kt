package de.connect2x.livekitnity.api.room

import de.connect2x.livekitnity.api.ConnectOptions
import de.connect2x.livekitnity.api.audio.AudioSwitchHandler
import de.connect2x.livekitnity.api.e2ee.KeyProvider
import de.connect2x.livekitnity.api.events.EventListenable
import de.connect2x.livekitnity.api.events.RoomEvent
import de.connect2x.livekitnity.api.room.participant.LocalParticipant
import de.connect2x.livekitnity.api.room.participant.Participant
import de.connect2x.livekitnity.api.room.participant.RemoteParticipant
import kotlin.jvm.JvmInline
import kotlinx.coroutines.flow.StateFlow

public interface Room {

  public val events: EventListenable<RoomEvent>

  public val keyProvider: KeyProvider?

  public val sid: StateFlow<Sid?>

  public val name: StateFlow<String?>

  public val state: StateFlow<State>

  public val metadata: StateFlow<String?>

  public val isRecording: StateFlow<Boolean>

  public val localParticipant: LocalParticipant

  public val remoteParticipants: StateFlow<Map<Participant.Identity, RemoteParticipant>>

  public val audioSwitchHandler: AudioSwitchHandler?

  public val activeSpeakers: StateFlow<List<Participant>>

  public suspend fun prepareConnection(url: String, token: String? = null)

  public suspend fun connect(url: String, token: String, options: ConnectOptions = ConnectOptions())

  public suspend fun disconnect()

  public suspend fun release()

  @JvmInline public value class Sid(public val sid: String)

  public enum class State {
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    RECONNECTING,
  }
}
