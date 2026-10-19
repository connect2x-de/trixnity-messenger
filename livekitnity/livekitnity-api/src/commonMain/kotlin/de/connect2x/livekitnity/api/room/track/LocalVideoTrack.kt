package de.connect2x.livekitnity.api.room.track

public interface LocalVideoTrack : VideoTrack {

  public val deviceId: String?
  public val position: CameraPosition?

  public fun switchCamera(deviceId: String? = null, position: CameraPosition? = null)
}
