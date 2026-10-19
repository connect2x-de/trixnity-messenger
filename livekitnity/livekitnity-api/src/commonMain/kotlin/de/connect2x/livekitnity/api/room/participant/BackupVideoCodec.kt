package de.connect2x.livekitnity.api.room.participant

import de.connect2x.livekitnity.api.room.track.VideoEncoding

public data class BackupVideoCodec(
    val codec: String = "vp8",
    val encoding: VideoEncoding? = null,
    val simulcast: Boolean = true,
)
