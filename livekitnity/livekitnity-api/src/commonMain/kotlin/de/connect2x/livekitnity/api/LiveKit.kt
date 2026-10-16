package de.connect2x.livekitnity.api

import de.connect2x.livekitnity.api.room.Room

public interface LiveKit {
  public fun create(
      options: RoomOptions = RoomOptions(),
  ): Room

  // TODO(fhilgers): this should be moved somewhere else and unified with AudioSwitchHandler
  public fun createCameraEnumerator(): CameraEnumerator
}
