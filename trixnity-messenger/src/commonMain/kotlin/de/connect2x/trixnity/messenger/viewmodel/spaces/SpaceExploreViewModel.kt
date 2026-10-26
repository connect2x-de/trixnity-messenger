package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import org.koin.core.component.get

interface SpaceExploreViewModel : SpaceContentViewModel {
    fun openSettings()
}

interface SpaceExploreViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        userId: UserId,
        spaceId: RoomId,
        onSelect: (SpaceBrowserItem) -> Unit,
        onBack: () -> Unit,
        onOpenSettings: () -> Unit,
    ): SpaceExploreViewModel =
        SpaceExploreViewModelImpl(
            viewModelContext
                .get<SpaceContentViewModelFactory>()
                .create(viewModelContext, userId, spaceId, onSelect, onBack),
            onOpenSettings,
        )

    companion object : SpaceExploreViewModelFactory
}

internal class SpaceExploreViewModelImpl(content: SpaceContentViewModel, private val onOpenSettings: () -> Unit) :
    SpaceExploreViewModel, SpaceContentViewModel by content {
    override fun openSettings() = onOpenSettings()
}
