package de.connect2x.livekitnity.api.room.track

public enum class VideoPreset43(
    override val capture: VideoCaptureParameter,
    override val encoding: VideoEncoding,
) : VideoPreset {
  H120(
      VideoCaptureParameter(160, 120, 15),
      VideoEncoding(70_000, 15),
  ),
  H180(
      VideoCaptureParameter(240, 180, 15),
      VideoEncoding(125_000, 15),
  ),
  H240(
      VideoCaptureParameter(320, 240, 15),
      VideoEncoding(140_000, 15),
  ),
  H360(
      VideoCaptureParameter(480, 360, 30),
      VideoEncoding(330_000, 30),
  ),
  H480(
      VideoCaptureParameter(640, 480, 30),
      VideoEncoding(500_000, 30),
  ),
  H540(
      VideoCaptureParameter(720, 540, 30),
      VideoEncoding(600_000, 30),
  ),
  H720(
      VideoCaptureParameter(960, 720, 30),
      VideoEncoding(1_300_000, 30),
  ),
  H1080(
      VideoCaptureParameter(1440, 1080, 30),
      VideoEncoding(2_300_000, 30),
  ),
  H1440(
      VideoCaptureParameter(1920, 1440, 30),
      VideoEncoding(3_800_000, 30),
  ),
}
