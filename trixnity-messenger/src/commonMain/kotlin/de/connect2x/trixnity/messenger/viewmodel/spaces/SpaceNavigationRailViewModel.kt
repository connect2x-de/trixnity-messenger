package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import org.koin.core.component.get

interface SpaceNavigationRailViewModel : SpaceContentViewModel

interface SpaceNavigationRailViewModelFactory {
    fun create(viewModelContext: ViewModelContext, onSelect: (SpaceBrowserItem) -> Unit): SpaceNavigationRailViewModel =
        SpaceNavigationRailViewModelImpl(
            viewModelContext.get<SpaceContentViewModelFactory>().create(viewModelContext, null, null, onSelect, {})
        )

    companion object : SpaceNavigationRailViewModelFactory
}

internal class SpaceNavigationRailViewModelImpl(content: SpaceContentViewModel) :
    SpaceNavigationRailViewModel, SpaceContentViewModel by content
