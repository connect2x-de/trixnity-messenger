package de.connect2x.livekitnity.api.room.participant

public interface BaseAudioTrackPublishOptions {
  public val audioBitrate: Int?
  public val dtx: Boolean
  public val red: Boolean
  public val preconnect: Boolean
}
