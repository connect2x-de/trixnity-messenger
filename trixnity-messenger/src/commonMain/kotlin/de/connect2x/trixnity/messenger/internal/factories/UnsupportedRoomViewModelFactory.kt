package de.connect2x.trixnity.messenger.internal.factories

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.RoomViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.RoomViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.OpenAvatarCutterCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback

internal fun UnsupportedRoomViewModelFactory(): RoomViewModelFactory {
    return UnsupportedRoomViewModelFactoryImpl()
}

private class UnsupportedRoomViewModelFactoryImpl : RoomViewModelFactory {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        onOpenRoom: (UserId, RoomId) -> Unit,
        onCloseRoom: () -> Unit,
        onOpenMention: OpenMentionCallback,
        onOpenAvatarCutter: OpenAvatarCutterCallback,
    ): RoomViewModel {
        error("RoomViewModel is not supported when using nav3")
    }
}
