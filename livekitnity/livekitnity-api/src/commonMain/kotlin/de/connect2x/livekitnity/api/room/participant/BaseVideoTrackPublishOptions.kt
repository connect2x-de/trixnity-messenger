package de.connect2x.livekitnity.api.room.participant

import de.connect2x.livekitnity.api.room.track.VideoEncoding

public interface BaseVideoTrackPublishOptions {
  public val videoEncoding: VideoEncoding?
  public val simulcast: Boolean
  public val videoCodec: String
  public val scalabilityMode: String?
  public val backupCodec: BackupVideoCodec?
}
