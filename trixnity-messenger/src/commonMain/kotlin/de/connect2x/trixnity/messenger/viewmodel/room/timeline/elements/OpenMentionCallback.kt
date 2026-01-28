package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.core.model.UserId

typealias OpenMentionCallback = (userId: UserId, timelineElementMention: TimelineElementMention) -> Unit
