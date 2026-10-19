package de.connect2x.livekitnity.core.room.track

import de.connect2x.livekitnity.api.room.track.AndroidAudioTrack
import de.connect2x.livekitnity.api.room.track.AndroidRemoteAudioTrack
import de.connect2x.livekitnity.core.platform.Converter

internal fun AndroidRemoteAudioTrack(
    delegate: io.livekit.android.room.track.RemoteAudioTrack,
    converter: Converter,
): AndroidRemoteAudioTrack {
  return AndroidRemoteAudioTrackImpl(
      delegate = delegate,
      converter = converter,
  )
}

private class AndroidRemoteAudioTrackImpl(
    private val delegate: io.livekit.android.room.track.RemoteAudioTrack,
    converter: Converter,
) :
    AndroidRemoteAudioTrack,
    AndroidAudioTrack by AndroidAudioTrack(
        delegate = delegate,
        converter = converter,
    ) {

  override fun setVolume(volume: Double) {
    delegate.setVolume(volume)
  }
}
