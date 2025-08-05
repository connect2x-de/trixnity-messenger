package de.connect2x.trixnity.messenger.i18n

import net.folivo.trixnity.client.store.RoomOutboxMessage

fun RoomOutboxMessage.SendError.getErrorMessage(i18n: I18n) : String? {
    return when (this) {
        RoomOutboxMessage.SendError.NoEventPermission -> i18n.sendErrorEventPermission()
        RoomOutboxMessage.SendError.NoMediaPermission -> i18n.sendErrorMediaPermission()
        RoomOutboxMessage.SendError.MediaTooLarge -> i18n.sendErrorMediaTooLarge()
        is RoomOutboxMessage.SendError.BadRequest -> i18n.sendErrorUnknown(this.errorResponse.error)
        is RoomOutboxMessage.SendError.Unknown -> i18n.sendErrorUnknown(this.errorResponse?.error)
        RoomOutboxMessage.SendError.EncryptionAlgorithmNotSupported -> i18n.sendErrorUnknown(this.toString())
        is RoomOutboxMessage.SendError.EncryptionError -> i18n.sendErrorUnknown(this.reason)
        null -> null
    }
}
