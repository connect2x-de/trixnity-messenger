package de.connect2x.livekitnity.api.room.track

public enum class ScreenSharePresets(
    override val capture: VideoCaptureParameter,
    override val encoding: VideoEncoding,
) : VideoPreset {
  H360_FPS3(
      VideoCaptureParameter(640, 360, 3),
      VideoEncoding(200_000, 3),
  ),
  H360_FPS15(
      VideoCaptureParameter(640, 360, 15),
      VideoEncoding(400_000, 15),
  ),
  H720_FPS5(
      VideoCaptureParameter(1280, 720, 5),
      VideoEncoding(800_000, 5),
  ),
  H720_FPS15(
      VideoCaptureParameter(1280, 720, 15),
      VideoEncoding(1_500_000, 15),
  ),
  H720_FPS30(
      VideoCaptureParameter(1280, 720, 30),
      VideoEncoding(2_000_000, 30),
  ),
  H1080_FPS15(
      VideoCaptureParameter(1920, 1080, 15),
      VideoEncoding(2_500_000, 15),
  ),
  H1080_FPS30(
      VideoCaptureParameter(1920, 1080, 30),
      VideoEncoding(5_000_000, 30),
  ),

  /** Uses the original resolution without resizing. */
  ORIGINAL(
      VideoCaptureParameter(0, 0, 30, adaptOutputToDimensions = false),
      VideoEncoding(7_000_000, 30),
  ),
}
