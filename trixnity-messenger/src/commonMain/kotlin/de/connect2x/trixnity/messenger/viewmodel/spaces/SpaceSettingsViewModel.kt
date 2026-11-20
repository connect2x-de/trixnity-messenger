package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel

interface SpaceSettingsViewModel {
    val details: RoomSettingsViewModel
}

interface SpaceSettingsViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        createDetails: (MatrixClientViewModelContext) -> RoomSettingsViewModel,
    ): SpaceSettingsViewModel = SpaceSettingsViewModelImpl(createDetails(viewModelContext.childContext("Details")))

    companion object : SpaceSettingsViewModelFactory
}

internal class SpaceSettingsViewModelImpl(override val details: RoomSettingsViewModel) : SpaceSettingsViewModel
