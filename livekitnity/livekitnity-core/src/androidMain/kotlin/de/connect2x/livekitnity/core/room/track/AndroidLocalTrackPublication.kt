package de.connect2x.livekitnity.core.room.track

import de.connect2x.livekitnity.api.room.track.AndroidLocalTrackPublication
import de.connect2x.livekitnity.api.room.track.AndroidTrackPublication
import de.connect2x.livekitnity.core.platform.Converter

internal fun AndroidLocalTrackPublication(
    delegate: io.livekit.android.room.track.LocalTrackPublication,
    converter: Converter,
): AndroidLocalTrackPublication {
  return AndroidLocalTrackPublicationImpl(delegate, converter)
}

private class AndroidLocalTrackPublicationImpl(
    delegate: io.livekit.android.room.track.LocalTrackPublication,
    converter: Converter,
) :
    AndroidLocalTrackPublication,
    AndroidTrackPublication by AndroidTrackPublication(delegate, converter)
