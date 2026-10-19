package de.connect2x.livekitnity.core.room.track

import de.connect2x.livekitnity.api.room.track.AndroidAudioTrack
import de.connect2x.livekitnity.api.room.track.AndroidLocalAudioTrack
import de.connect2x.livekitnity.core.platform.Converter

internal fun AndroidLocalAudioTrack(
    delegate: io.livekit.android.room.track.LocalAudioTrack,
    converter: Converter,
): AndroidLocalAudioTrack {
  return AndroidLocalAudioTrackImpl(
      delegate = delegate,
      converter = converter,
  )
}

private class AndroidLocalAudioTrackImpl(
    delegate: io.livekit.android.room.track.LocalAudioTrack,
    converter: Converter,
) :
    AndroidLocalAudioTrack,
    AndroidAudioTrack by AndroidAudioTrack(
        delegate = delegate,
        converter = converter,
    )
