package de.connect2x.livekitnity.api.room.participant

public data class ParticipantTrackPermission(
    val participantIdentity: String? = null,
    val participantSid: String? = null,
    val allTracksAllowed: Boolean = false,
    val allowedTrackSids: List<String> = emptyList(),
) {
  init {
    if (participantIdentity == null && participantSid == null) {
      throw IllegalArgumentException("Either identity or sid must be provided.")
    }
  }
}
