package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.selectSpaceItem
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceNavigationRailViewModel
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceNavigationRailViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class SpaceNavigationRailViewModelFactoryAdapter(
    private val factory: SpaceNavigationRailViewModelFactory,
    private val navigation: RouteNavigation,
) : ViewModelFactoryAdapter<SpaceNavigationRailViewModel> {
    override fun create(parameters: ParametersHolder): SpaceNavigationRailViewModel {
        val context = parameters.get<ViewModelContext>().childContext("SpaceNavigationRail")
        return factory.create(
            viewModelContext = context,
            onSelect = navigation.navigationCallback { item -> selectSpaceItem(item) },
        )
    }
}
