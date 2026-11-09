package de.connect2x.livekitnity.api

import de.connect2x.livekitnity.api.room.WebRoom

public interface WebLiveKit : LiveKit {
  override fun create(options: RoomOptions): WebRoom
}
