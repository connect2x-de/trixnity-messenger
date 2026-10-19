package de.connect2x.livekitnity.core.room.track

import de.connect2x.livekitnity.api.room.track.WebLocalAudioTrack
import de.connect2x.livekitnity.api.room.track.WebTrack
import de.connect2x.livekitnity.core.platform.Converter

internal fun WebLocalAudioTrack(
    localAudioTrack: livekit.LocalAudioTrack,
    converter: Converter,
    trackName: String,
): WebLocalAudioTrack {
  return WebLocalAudioTrackImpl(
      localAudioTrack = localAudioTrack,
      converter = converter,
      trackName = trackName,
  )
}

private class WebLocalAudioTrackImpl
private constructor(
    private val base: WebTrack,
) : WebLocalAudioTrack, WebTrack by base {

  constructor(
      localAudioTrack: livekit.LocalAudioTrack,
      converter: Converter,
      trackName: String,
  ) : this(
      base =
          WebTrack(
              track = localAudioTrack,
              converter = converter,
              trackName = trackName,
          ),
  )
}
