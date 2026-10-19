package de.connect2x.livekitnity.api.room.participant

import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.api.room.track.VideoCodec
import de.connect2x.livekitnity.api.room.track.VideoEncoding

public data class VideoTrackPublishOptions(
    override val name: String? = null,
    override val videoEncoding: VideoEncoding? = null,
    override val simulcast: Boolean = true,
    override val videoCodec: String = VideoCodec.VP8.codecName,
    override val scalabilityMode: String? = null,
    override val backupCodec: BackupVideoCodec? = null,
    override val source: Track.Source? = null,
    override val stream: String? = null,
) : BaseVideoTrackPublishOptions, TrackPublishOptions
