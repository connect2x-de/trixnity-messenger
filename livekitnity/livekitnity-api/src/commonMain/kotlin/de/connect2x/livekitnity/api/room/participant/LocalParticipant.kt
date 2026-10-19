package de.connect2x.livekitnity.api.room.participant

public interface LocalParticipant : Participant {
  public suspend fun setCameraEnabled(enabled: Boolean): Boolean

  public suspend fun setMicrophoneEnabled(enabled: Boolean): Boolean

  public suspend fun setScreenShareEnabled(enabled: Boolean): Boolean
}
