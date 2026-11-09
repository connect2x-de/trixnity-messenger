package de.connect2x.livekitnity.api.room.participant

import de.connect2x.livekitnity.api.room.track.Track

public data class AudioTrackPublishOptions(
    override val name: String? = null,
    override val audioBitrate: Int? = null,
    override val dtx: Boolean = true,
    override val red: Boolean = true,
    override val source: Track.Source? = null,
    override val stream: String? = null,
    override val preconnect: Boolean = false,
) : BaseAudioTrackPublishOptions, TrackPublishOptions
