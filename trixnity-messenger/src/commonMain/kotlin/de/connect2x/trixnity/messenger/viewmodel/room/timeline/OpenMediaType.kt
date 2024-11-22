package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent


enum class OpenMediaType {
    VIDEO,
    IMAGE,
    PDF,
    TEXT,
    MARKDOWN,
}

typealias OpenMediaCallback = (
    content: RoomMessageEventContent.FileBased,
    onDownloadAction: () -> Unit
) -> Unit

typealias OpenMediaUserCallback = (
    content: RoomMessageEventContent.FileBased,
    onDownloadAction: () -> Unit,
    userId: UserId,
) -> Unit
