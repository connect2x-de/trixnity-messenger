package de.connect2x.livekitnity.core

import android.content.Context
import de.connect2x.livekitnity.api.AndroidLiveKit
import de.connect2x.livekitnity.api.CameraEnumerator
import de.connect2x.livekitnity.api.RoomOptions
import de.connect2x.livekitnity.api.room.AndroidRoom
import de.connect2x.livekitnity.core.platform.Converter
import io.livekit.android.room.track.video.CameraCapturerUtils
import io.livekit.android.util.LoggingLevel

public fun AndroidLiveKit(
    context: Context,
): AndroidLiveKit {
  return AndroidLiveKitImpl(
      context = context,
  )
}

private class AndroidLiveKitImpl(
    private val context: Context,
) : AndroidLiveKit {

  init {
    io.livekit.android.LiveKit.init(context)
    io.livekit.android.LiveKit.loggingLevel = LoggingLevel.INFO
  }

  override fun create(
      options: RoomOptions,
  ): AndroidRoom {
    val room =
        io.livekit.android.LiveKit.create(
            appContext = context,
            options = Converter.fromCommon(options),
        )
    return Converter(context).toCommon(room)
  }

  override fun createCameraEnumerator(): CameraEnumerator {
    return Converter.toCommon(CameraCapturerUtils.createCameraEnumerator(context))
  }
}
