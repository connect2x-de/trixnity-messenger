package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.roomlist.ProfilesSettingsRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.ProfilesSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.ProfilesSettingsViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class ProfilesSettingsViewModelFactoryAdapter(
    private val factory: ProfilesSettingsViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<ProfilesSettingsViewModel> {
    override fun create(parameters: ParametersHolder): ProfilesSettingsViewModel {
        val route = parameters.get<ProfilesSettingsRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("ProfilesSettings")

        return factory.create(
            viewModelContext = viewModelContext,
            onCloseProfilesSettings = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
