package de.connect2x.livekitnity.api.room.track

public data class VideoCaptureParameter(
    val width: Int,
    val height: Int,
    val maxFps: Int,
    val adaptOutputToDimensions: Boolean = true,
)
