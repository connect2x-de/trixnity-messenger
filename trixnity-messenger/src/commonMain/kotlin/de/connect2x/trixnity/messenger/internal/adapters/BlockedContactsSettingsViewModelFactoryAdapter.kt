package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.roomlist.BlockedContactsSettingsRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.BlockedContactsSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.BlockedContactsSettingsViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class BlockedContactsSettingsViewModelFactoryAdapter(
    private val factory: BlockedContactsSettingsViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<BlockedContactsSettingsViewModel> {

    override fun create(parameters: ParametersHolder): BlockedContactsSettingsViewModel {
        val route = parameters.get<BlockedContactsSettingsRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("BlockedContactsSettings", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            onCloseBlockedContactsSettings = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
