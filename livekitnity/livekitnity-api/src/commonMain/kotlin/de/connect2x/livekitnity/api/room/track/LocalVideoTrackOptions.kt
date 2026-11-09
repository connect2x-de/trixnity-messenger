package de.connect2x.livekitnity.api.room.track

public data class LocalVideoTrackOptions(
    val isScreencast: Boolean = false,
    val deviceId: String? = null,
    val position: CameraPosition? = CameraPosition.FRONT,
    val captureParams: VideoCaptureParameter = VideoPreset169.H720.capture,
)
