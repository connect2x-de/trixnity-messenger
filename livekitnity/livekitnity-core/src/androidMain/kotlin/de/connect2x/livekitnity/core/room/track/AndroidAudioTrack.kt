package de.connect2x.livekitnity.core.room.track

import de.connect2x.livekitnity.api.room.track.AndroidAudioTrack
import de.connect2x.livekitnity.api.room.track.AndroidTrack
import de.connect2x.livekitnity.core.platform.Converter

internal fun AndroidAudioTrack(
    delegate: io.livekit.android.room.track.AudioTrack,
    converter: Converter,
): AndroidAudioTrack {
  return AndroidAudioTrackImpl(
      delegate = delegate,
      converter = converter,
  )
}

private class AndroidAudioTrackImpl(
    delegate: io.livekit.android.room.track.AudioTrack,
    converter: Converter,
) :
    AndroidAudioTrack,
    AndroidTrack by AndroidTrack(
        delegate = delegate,
        converter = converter,
    )
