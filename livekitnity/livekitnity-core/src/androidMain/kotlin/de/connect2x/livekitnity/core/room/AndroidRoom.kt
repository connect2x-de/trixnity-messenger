package de.connect2x.livekitnity.core.room

import de.connect2x.livekitnity.api.ConnectOptions
import de.connect2x.livekitnity.api.audio.AudioSwitchHandler
import de.connect2x.livekitnity.api.e2ee.KeyProvider
import de.connect2x.livekitnity.api.events.EventListenable
import de.connect2x.livekitnity.api.events.RoomEvent
import de.connect2x.livekitnity.api.room.AndroidRoom
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.api.room.participant.AndroidLocalParticipant
import de.connect2x.livekitnity.api.room.participant.AndroidParticipant
import de.connect2x.livekitnity.api.room.participant.AndroidRemoteParticipant
import de.connect2x.livekitnity.api.room.participant.Participant
import de.connect2x.livekitnity.core.SimpleEventListenable
import de.connect2x.livekitnity.core.e2ee.AndroidKeyProvider
import de.connect2x.livekitnity.core.e2ee.NativeKeyProvider
import de.connect2x.livekitnity.core.mapSharedNotNull
import de.connect2x.livekitnity.core.mapState
import de.connect2x.livekitnity.core.platform.Converter
import io.livekit.android.util.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import livekit.org.webrtc.EglBase

internal fun AndroidRoom(
    room: io.livekit.android.room.Room,
    converter: Converter,
): AndroidRoom {
  return AndroidRoomImpl(
      room = room,
      converter = converter,
  )
}

private class AndroidRoomImpl(
    private val room: io.livekit.android.room.Room,
    private val converter: Converter,
) : AndroidRoom {

  override val eglBaseContext: EglBase.Context
    get() = room.lkObjects.eglBase.eglBaseContext

  override val keyProvider: KeyProvider? =
      room.e2eeOptions?.keyProvider?.let { keyProvider ->
        AndroidKeyProvider(
            keyProvider = keyProvider as NativeKeyProvider,
            e2EEManager = { room.e2eeManager },
        )
      }

  override val events: EventListenable<RoomEvent> =
      SimpleEventListenable(
          events = room.events.events.mapSharedNotNull(converter::toCommon),
      )

  override val sid: StateFlow<Room.Sid?>
    get() = room::sid.flow.mapState { Converter.toCommon(it ?: return@mapState null) }

  override val name: StateFlow<String?>
    get() = room::name.flow

  override val state: StateFlow<Room.State>
    get() = room::state.flow.mapState { Converter.toCommon(it) }

  override val metadata: StateFlow<String?>
    get() = room::metadata.flow

  override val isRecording: StateFlow<Boolean>
    get() = room::isRecording.flow

  override val audioSwitchHandler: AudioSwitchHandler?
    get() = converter.toCommon(room.audioSwitchHandler ?: return null)

  override val localParticipant: AndroidLocalParticipant
    get() = converter.toCommon(room.localParticipant)

  override val remoteParticipants: StateFlow<Map<Participant.Identity, AndroidRemoteParticipant>>
    get() = room::remoteParticipants.flow.mapState { converter.toCommon(it) }

  override val activeSpeakers: StateFlow<List<AndroidParticipant>>
    get() = room::activeSpeakers.flow.mapState { converter.toCommon(it) }

  override suspend fun prepareConnection(url: String, token: String?) {
    room.prepareConnection(url, token)
  }

  override suspend fun connect(
      url: String,
      token: String,
      options: ConnectOptions,
  ) {
    room.connect(
        url = url,
        token = token,
        options = Converter.fromCommon(options),
    )
  }

  override suspend fun disconnect() {
    withContext(NonCancellable) {
      withContext(Dispatchers.IO) {
        room.disconnect()
      }
    }
  }

  override suspend fun release() {
    withContext(NonCancellable) {
      withContext(Dispatchers.IO) {
        try {
          room.release()
        } finally {
          converter.clearCache()
        }
      }
    }
  }
}
