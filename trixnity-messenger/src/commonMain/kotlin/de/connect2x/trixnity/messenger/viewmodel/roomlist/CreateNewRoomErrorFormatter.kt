package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.core.ErrorResponse
import de.connect2x.trixnity.core.MatrixServerException


class CreateNewRoomErrorFormatter(private val i18n: I18n) {
    fun error(
        throwable: Throwable,
        isChat: Boolean
    ): String {
        return if (throwable is MatrixServerException) {
            when (throwable.errorResponse) {
                is ErrorResponse.BadJson -> i18n.createNewRoomBadJson(isChat)
                is ErrorResponse.RoomInUse -> i18n.createNewRoomRoomInUse(isChat)
                is ErrorResponse.InvalidRoomState -> i18n.createNewRoomInvalidState(isChat)
                is ErrorResponse.UnsupportedRoomVersion -> i18n.createNewRoomInvalidRoomVersion(isChat)
                else -> i18n.createNewRoomError(isChat)
            }
        } else {
            i18n.createNewRoomError(isChat)
        }
    }

    fun errorDetails(throwable: Throwable): String? {
        return if (throwable is MatrixServerException) {
            throwable.errorResponse.error
        } else throwable.message
    }

}
