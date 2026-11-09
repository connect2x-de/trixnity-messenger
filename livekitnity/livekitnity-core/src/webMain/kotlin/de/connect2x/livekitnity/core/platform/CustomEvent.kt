package de.connect2x.livekitnity.core.platform

import livekit.EventCallback0
import livekit.EventEmitter

internal object CustomEvent {
  // TODO(fhilgers): cleanup the amount of unsafeCast
  internal val StreamStateChanged: EventCallback0 =
      "custom-streamStateChanged".toJsString().unsafeCast<EventCallback0>()
  internal val TrackChanged: EventCallback0 =
      "custom-trackChanged".toJsString().unsafeCast<EventCallback0>()
  internal val IdentityChanged: EventCallback0 =
      "custom-identityChanged".toJsString().unsafeCast<EventCallback0>()
  internal val AudioLevelChanged: EventCallback0 =
      "custom-audioLevelChanged".toJsString().unsafeCast<EventCallback0>()

  fun subscribe(room: livekit.Room): () -> Unit {
    val onStreamStateChanged =
        onStreamStateChanged@{
            publication: livekit.RemoteTrackPublication,
            _: livekit.TrackStreamState,
            _: livekit.RemoteParticipant ->
          val track = publication.track ?: return@onStreamStateChanged
          emitEvent(track, StreamStateChanged)
        }

    val onTrackSubscribed =
        {
            _: livekit.RemoteTrack,
            publication: livekit.RemoteTrackPublication,
            _: livekit.RemoteParticipant ->
          emitEvent(publication, TrackChanged)
        }

    val onTrackUnsubscribed =
        {
            _: livekit.RemoteTrack,
            publication: livekit.RemoteTrackPublication,
            _: livekit.RemoteParticipant ->
          emitEvent(publication, TrackChanged)
        }

    val onLocalTrackPublished =
        { publication: livekit.LocalTrackPublication, _: livekit.LocalParticipant ->
          emitEvent(publication, TrackChanged)
        }

    val onLocalTrackUnpublished =
        { publication: livekit.LocalTrackPublication, _: livekit.LocalParticipant ->
          emitEvent(publication, TrackChanged)
        }

    val onSignalConnected = {
      emitEvent(room.localParticipant, IdentityChanged)
    }

    val onReconnected = {
      emitEvent(room.localParticipant, IdentityChanged)
    }

    val onParticipantConnected = { participant: livekit.RemoteParticipant ->
      emitEvent(participant, IdentityChanged)
    }

    var previousSpeakers = room.activeSpeakers.toList()

    val onActiveSpeakersChanged = { speakers: JsArray<livekit.Participant> ->
      val currentSpeakers = speakers.toList()
      val affectedParticipants = (previousSpeakers + currentSpeakers).distinct()
      previousSpeakers = currentSpeakers

      affectedParticipants.forEach { participant ->
        emitEvent(participant, AudioLevelChanged)
      }
    }

    room.on(livekit.RoomEvent.TrackStreamStateChanged, onStreamStateChanged)
    room.on(livekit.RoomEvent.TrackSubscribed, onTrackSubscribed)
    room.on(livekit.RoomEvent.TrackUnsubscribed, onTrackUnsubscribed)
    room.on(livekit.RoomEvent.LocalTrackPublished, onLocalTrackPublished)
    room.on(livekit.RoomEvent.LocalTrackUnpublished, onLocalTrackUnpublished)
    room.on(livekit.RoomEvent.SignalConnected, onSignalConnected)
    room.on(livekit.RoomEvent.Reconnected, onReconnected)
    room.on(livekit.RoomEvent.ParticipantConnected, onParticipantConnected)
    room.on(livekit.RoomEvent.ActiveSpeakersChanged, onActiveSpeakersChanged)

    var registered = true

    return unregister@{
      if (!registered) return@unregister
      registered = false

      room.off(livekit.RoomEvent.TrackStreamStateChanged, onStreamStateChanged)
      room.off(livekit.RoomEvent.TrackSubscribed, onTrackSubscribed)
      room.off(livekit.RoomEvent.TrackUnsubscribed, onTrackUnsubscribed)
      room.off(livekit.RoomEvent.LocalTrackPublished, onLocalTrackPublished)
      room.off(livekit.RoomEvent.LocalTrackUnpublished, onLocalTrackUnpublished)
      room.off(livekit.RoomEvent.SignalConnected, onSignalConnected)
      room.off(livekit.RoomEvent.Reconnected, onReconnected)
      room.off(livekit.RoomEvent.ParticipantConnected, onParticipantConnected)
      room.off(livekit.RoomEvent.ActiveSpeakersChanged, onActiveSpeakersChanged)

      previousSpeakers = emptyList()
    }
  }
}

@JsFun("""(eventEmitter, eventCallback) => eventEmitter.emit(eventCallback)""")
private external fun emitEvent(eventEmitter: EventEmitter, eventCallback: EventCallback0)
