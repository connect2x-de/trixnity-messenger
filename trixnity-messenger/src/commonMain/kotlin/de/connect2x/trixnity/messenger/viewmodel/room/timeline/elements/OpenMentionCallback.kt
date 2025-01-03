package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import net.folivo.trixnity.core.model.UserId

typealias OpenMentionCallback = (userId: UserId, timelineElementMention: TimelineElementMention) -> Unit
