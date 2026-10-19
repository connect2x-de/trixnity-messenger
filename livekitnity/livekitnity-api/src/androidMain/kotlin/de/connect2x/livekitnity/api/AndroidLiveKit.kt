package de.connect2x.livekitnity.api

import de.connect2x.livekitnity.api.room.AndroidRoom

public interface AndroidLiveKit : LiveKit {
  override fun create(options: RoomOptions): AndroidRoom
}
