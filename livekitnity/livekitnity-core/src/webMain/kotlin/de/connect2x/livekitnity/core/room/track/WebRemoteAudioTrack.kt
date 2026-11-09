package de.connect2x.livekitnity.core.room.track

import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.api.room.track.WebRemoteAudioTrack
import de.connect2x.livekitnity.api.room.track.WebTrack
import de.connect2x.livekitnity.core.platform.Converter

internal fun WebRemoteAudioTrack(
    remoteAudioTrack: livekit.RemoteAudioTrack,
    converter: Converter,
    trackName: String,
): WebRemoteAudioTrack {
  return WebRemoteAudioTrackImpl(
      remoteAudioTrack = remoteAudioTrack,
      converter = converter,
      trackName = trackName,
  )
}

private class WebRemoteAudioTrackImpl
private constructor(
    private val remoteAudioTrack: livekit.RemoteAudioTrack,
    private val base: WebTrack,
) : WebRemoteAudioTrack, Track by base {

  constructor(
      remoteAudioTrack: livekit.RemoteAudioTrack,
      converter: Converter,
      trackName: String,
  ) : this(
      remoteAudioTrack = remoteAudioTrack,
      base =
          WebTrack(
              track = remoteAudioTrack,
              converter = converter,
              trackName = trackName,
          ),
  )

  init {
    // TODO(fhilgers): In the future we might want to properly set up an Audio Graph
    //                 using `AudioContext` for now, attaching on create ensures that it
    // automatically
    //                 plays aligning with the android behavior.
    remoteAudioTrack.attach()
  }

  override fun setVolume(volume: Double) {
    remoteAudioTrack.setVolume(volume)
  }
}
