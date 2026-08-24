package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.roomlist.AccountsRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.AppearanceSettingsRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.DeviceSettingsRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.NotificationSettingsRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.PrivacySettingsRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.ProfilesSettingsRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.UserSettingsRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.UserSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.UserSettingsViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class UserSettingsViewModelFactoryAdapter(
    private val factory: UserSettingsViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<UserSettingsViewModel> {
    override fun create(parameters: ParametersHolder): UserSettingsViewModel {
        val route = parameters.get<UserSettingsRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("UserSettings")

        return factory.create(
            viewModelContext = viewModelContext,
            onCloseUserSettings = routeNavigation.navigationCallback { pop(route) },
            onShowDeviceSettings = routeNavigation.navigationCallback { push(DeviceSettingsRoute) },
            onShowAccounts = routeNavigation.navigationCallback { push(AccountsRoute) },
            onShowProfilesSettings = routeNavigation.navigationCallback { push(ProfilesSettingsRoute) },
            onShowNotificationsSettings = routeNavigation.navigationCallback { push(NotificationSettingsRoute) },
            onShowPrivacySettings = routeNavigation.navigationCallback { push(PrivacySettingsRoute) },
            onShowAppearanceSettings = routeNavigation.navigationCallback { push(AppearanceSettingsRoute) },
        )
    }
}
