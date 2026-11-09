package de.connect2x.livekitnity.api.room.track

public data class CustomVideoPreset(
    override val capture: VideoCaptureParameter,
    override val encoding: VideoEncoding,
) : VideoPreset
