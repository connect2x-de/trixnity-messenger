package de.connect2x.livekitnity.core

import de.connect2x.livekitnity.api.CameraEnumerator
import de.connect2x.livekitnity.api.RoomOptions
import de.connect2x.livekitnity.api.WebLiveKit
import de.connect2x.livekitnity.api.room.WebRoom
import de.connect2x.livekitnity.core.platform.Converter
import de.connect2x.livekitnity.core.room.WebRoom

public fun WebLiveKit(): WebLiveKit {
  return WebLiveKitImpl()
}

private class WebLiveKitImpl : WebLiveKit {

  override fun create(
      options: RoomOptions,
  ): WebRoom {
    val room =
        livekit.Room(
            options = Converter.fromCommon(options),
        )
    return Converter()
        .toCommon(
            room = room,
        )
  }

  override fun createCameraEnumerator(): CameraEnumerator {
    // TODO(fhilgers): implement
    return object : CameraEnumerator {
      override val deviceNames: List<String>
        get() = listOf("webcam")

      override fun isBackFacing(deviceName: String): Boolean {
        return false
      }

      override fun isFrontFacing(deviceName: String): Boolean {
        return true
      }
    }
  }
}
