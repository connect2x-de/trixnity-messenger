package de.connect2x.livekitnity.api.room.participant

import de.connect2x.livekitnity.api.room.track.Track

public data class ParticipantPermission(
    val canPublish: Boolean,
    val canSubscribe: Boolean,
    val canPublishData: Boolean,
    val hidden: Boolean,
    val recorder: Boolean,
    val canPublishSources: List<Track.Source>,
    val canUpdateMetadata: Boolean,
    val canSubscribeMetrics: Boolean,
)
