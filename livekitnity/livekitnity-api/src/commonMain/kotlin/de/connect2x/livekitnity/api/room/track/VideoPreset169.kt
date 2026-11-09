package de.connect2x.livekitnity.api.room.track

public enum class VideoPreset169(
    override val capture: VideoCaptureParameter,
    override val encoding: VideoEncoding,
) : VideoPreset {
  H90(
      VideoCaptureParameter(160, 90, 15),
      VideoEncoding(90_000, 15),
  ),
  H180(
      VideoCaptureParameter(320, 180, 15),
      VideoEncoding(160_000, 15),
  ),
  H216(
      VideoCaptureParameter(384, 216, 15),
      VideoEncoding(180_000, 15),
  ),
  H360(
      VideoCaptureParameter(640, 360, 30),
      VideoEncoding(450_000, 30),
  ),
  H540(
      VideoCaptureParameter(960, 540, 30),
      VideoEncoding(800_000, 30),
  ),
  H720(
      VideoCaptureParameter(1280, 720, 30),
      VideoEncoding(1_700_000, 30),
  ),
  H1080(
      VideoCaptureParameter(1920, 1080, 30),
      VideoEncoding(3_000_000, 30),
  ),
  H1440(
      VideoCaptureParameter(2560, 1440, 30),
      VideoEncoding(5_000_000, 30),
  ),
  H2160(
      VideoCaptureParameter(3840, 2160, 30),
      VideoEncoding(8_000_000, 30),
  ),
}
