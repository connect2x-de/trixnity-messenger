package de.connect2x.livekitnity.api.events

import de.connect2x.livekitnity.api.room.track.TrackPublication

public sealed class TrackPublicationEvent(public val publication: TrackPublication) : Event()
