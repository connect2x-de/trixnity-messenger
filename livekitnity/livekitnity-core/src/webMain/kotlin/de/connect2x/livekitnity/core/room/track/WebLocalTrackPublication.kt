package de.connect2x.livekitnity.core.room.track

import de.connect2x.livekitnity.api.room.track.WebLocalTrackPublication
import de.connect2x.livekitnity.api.room.track.WebTrackPublication
import de.connect2x.livekitnity.core.platform.Converter

internal fun WebLocalTrackPublication(
    localTrackPublication: livekit.LocalTrackPublication,
    converter: Converter,
): WebLocalTrackPublication {
  return WebLocalTrackPublicationImpl(
      localTrackPublication = localTrackPublication,
      converter = converter,
  )
}

private class WebLocalTrackPublicationImpl(
    private val base: WebTrackPublication,
) : WebLocalTrackPublication, WebTrackPublication by base {

  constructor(
      localTrackPublication: livekit.LocalTrackPublication,
      converter: Converter,
  ) : this(
      base =
          WebTrackPublication(
              trackPublication = localTrackPublication,
              converter = converter,
          ),
  )
}
