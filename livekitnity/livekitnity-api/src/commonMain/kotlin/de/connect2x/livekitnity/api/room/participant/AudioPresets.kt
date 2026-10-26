package de.connect2x.livekitnity.api.room.participant

public enum class AudioPresets(
    public val maxBitrate: Int,
) {
  TELEPHONE(12_000),
  SPEECH(24_000),
  MUSIC(48_000),
  MUSIC_STEREO(64_000),
  MUSIC_HIGH_QUALITY(96_000),
  MUSIC_HIGH_QUALITY_STEREO(128_000),
}
