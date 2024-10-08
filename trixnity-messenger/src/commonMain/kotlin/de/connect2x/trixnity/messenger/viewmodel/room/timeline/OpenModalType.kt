package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile


enum class OpenModalType {
    VIDEO,
    IMAGE,
    PDF,
    TEXT,
    MARKDOWN,
}

typealias OpenModalCallback = (
    type: OpenModalType,
    mxcUrl: String,
    encryptedFile: EncryptedFile?,
    fileName: String,
) -> Unit

typealias OpenModalUserCallback = (
    type: OpenModalType,
    mxcUrl: String,
    encryptedFile: EncryptedFile?,
    fileName: String,
    userId: UserId,
) -> Unit
