package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext

interface RoomSettingsPowerlevelViewModelFactory {
    companion object : RoomSettingsPowerlevelViewModelFactory

    fun create(viewModelContext: MatrixClientViewModelContext): RoomSettingsPowerlevelViewModel =
        RoomSettingsPowerlevelViewModelImpl(viewModelContext)
}

interface RoomSettingsPowerlevelViewModel {

}

class RoomSettingsPowerlevelViewModelImpl(
    viewModelContext: MatrixClientViewModelContext
) : MatrixClientViewModelContext by viewModelContext, RoomSettingsPowerlevelViewModel {

}

class PreviewRoomSettingsPowerlevelViewModel: RoomSettingsPowerlevelViewModel {}
