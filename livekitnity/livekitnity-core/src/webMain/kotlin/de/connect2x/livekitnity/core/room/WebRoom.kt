package de.connect2x.livekitnity.core.room

import de.connect2x.livekitnity.api.ConnectOptions
import de.connect2x.livekitnity.api.audio.AudioDevice
import de.connect2x.livekitnity.api.audio.AudioDeviceChangeListener
import de.connect2x.livekitnity.api.audio.AudioSwitchHandler
import de.connect2x.livekitnity.api.e2ee.KeyProvider
import de.connect2x.livekitnity.api.events.EventListenable
import de.connect2x.livekitnity.api.events.RoomEvent
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.api.room.WebRoom
import de.connect2x.livekitnity.api.room.participant.Participant
import de.connect2x.livekitnity.api.room.participant.WebLocalParticipant
import de.connect2x.livekitnity.api.room.participant.WebParticipant
import de.connect2x.livekitnity.api.room.participant.WebRemoteParticipant
import de.connect2x.livekitnity.core.SimpleEventListenable
import de.connect2x.livekitnity.core.platform.Converter
import de.connect2x.livekitnity.core.platform.CustomEvent
import de.connect2x.livekitnity.core.platform.EventDescriptors
import de.connect2x.livekitnity.core.platform.EventSharedFlow
import de.connect2x.livekitnity.core.platform.EventStateFlow
import de.connect2x.livekitnity.core.platform.RoomCtx
import de.connect2x.livekitnity.core.platform.StateDescriptors
import kotlin.js.Promise
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import livekit.E2EEManagerOptions

internal fun WebRoom(
    room: livekit.Room,
    converter: Converter,
): WebRoom {
  return WebRoomImpl(
      room = room,
      converter = converter,
  )
}

private class WebRoomImpl(
    private val room: livekit.Room,
    private val converter: Converter,
) : WebRoom {

  private val unsubscribeCustomEvents = CustomEvent.subscribe(room)

  override val keyProvider: KeyProvider?
    get() =
        converter.toCommon(
            keyProvider =
                room.options.encryption?.unsafeCast<E2EEManagerOptions>()?.keyProvider
                    ?: return null,
        )

  override val events: EventListenable<RoomEvent> =
      SimpleEventListenable(
          EventSharedFlow(
              makeCtx = { onEvent ->
                RoomCtx(
                    room = this,
                    eventEmitter = room,
                    converter = converter,
                    emit = onEvent,
                )
              },
              descriptors = EventDescriptors.Room.all,
          ),
      )

  override val sid: StateFlow<Room.Sid?> =
      EventStateFlow(
          eventEmitter = room,
          converter = converter,
          descriptor = StateDescriptors.Room.sid,
      )

  override var name: StateFlow<String?> =
      EventStateFlow(
          eventEmitter = room,
          converter = converter,
          descriptor = StateDescriptors.Room.name,
      )

  override var state: StateFlow<Room.State> =
      EventStateFlow(
          eventEmitter = room,
          converter = converter,
          descriptor = StateDescriptors.Room.state,
      )

  override var metadata: StateFlow<String?> =
      EventStateFlow(
          eventEmitter = room,
          converter = converter,
          descriptor = StateDescriptors.Room.metadata,
      )

  override var isRecording: StateFlow<Boolean> =
      EventStateFlow(
          eventEmitter = room,
          converter = converter,
          descriptor = StateDescriptors.Room.isRecording,
      )

  override val audioSwitchHandler: AudioSwitchHandler =
      // TODO (fhilgers): implement
      object : AudioSwitchHandler {
        override val selectedAudioDevice: AudioDevice?
          get() = null

        override val availableAudioDevices: List<AudioDevice>
          get() = emptyList()

        override fun selectDevice(audioDevice: AudioDevice?) {}

        override fun registerAudioDeviceChangeListener(listener: AudioDeviceChangeListener) {}

        override fun unregisterAudioDeviceChangeListener(listener: AudioDeviceChangeListener) {}
      }

  override val localParticipant: WebLocalParticipant
    get() =
        converter.toCommon(
            localParticipant = room.localParticipant,
        )

  override val remoteParticipants: StateFlow<Map<Participant.Identity, WebRemoteParticipant>> =
      EventStateFlow(
          eventEmitter = room,
          converter = converter,
          descriptor = StateDescriptors.Room.remoteParticipants,
      )

  override val activeSpeakers: StateFlow<List<WebParticipant>> =
      EventStateFlow(
          eventEmitter = room,
          converter = converter,
          descriptor = StateDescriptors.Room.activeSpeakers,
      )

  override suspend fun prepareConnection(url: String, token: String?) {
    withContext(NonCancellable) {
      room.prepareConnection(
          url = url,
          token = token,
      )
    }
  }

  override suspend fun connect(
      url: String,
      token: String,
      options: ConnectOptions,
  ) {
    withCleanupOnCancel(
        action = {
          room.localParticipant.setE2EEEnabled(room.options.encryption != null).then {
            room.connect(
                url = url,
                token = token,
                opts = Converter.fromCommon(options),
            )
          }
        },
        cleanup = {
          Converter.await(room.disconnect())
        },
    )
  }

  override suspend fun disconnect() {
    withContext(NonCancellable) {
      Converter.await(room.disconnect())
    }
  }

  override suspend fun release() {
    unsubscribeCustomEvents.invoke()
    converter.clearCache()
  }
}

private suspend fun <T : JsAny?> withCleanupOnCancel(
    action: () -> Promise<T>,
    cleanup: suspend () -> Unit,
): T {
  try {
    return Converter.await(action())
  } catch (cancelled: CancellationException) {
    withContext(NonCancellable) {
      try {
        cleanup()
      } catch (cleanupError: Throwable) {
        cancelled.addSuppressed(cleanupError)
      }
    }
    throw cancelled
  }
}
