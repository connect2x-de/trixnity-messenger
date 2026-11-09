package de.connect2x.livekitnity.api.room.participant

public data class AudioTrackPublishDefaults(
    override val audioBitrate: Int? = AudioPresets.MUSIC.maxBitrate,
    override val dtx: Boolean = true,
    override val red: Boolean = true,
    override val preconnect: Boolean = false,
) : BaseAudioTrackPublishOptions
