package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent

typealias OpenMediaCallback = (userId: UserId, content: RoomMessageEventContent.FileBased) -> Unit
