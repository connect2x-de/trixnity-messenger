package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.EventContent

typealias OpenMediaCallback = (
    userId: UserId,
    content: EventContent,
) -> Unit
