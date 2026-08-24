package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.root.MatrixClientInitializationRoute
import de.connect2x.trixnity.messenger.internal.routes.root.OAuth2AuthorizationCodeLoginRoute
import de.connect2x.trixnity.messenger.internal.routes.root.OAuth2DeviceAuthorizationLoginRoute
import de.connect2x.trixnity.messenger.internal.routes.root.PasswordLoginRoute
import de.connect2x.trixnity.messenger.internal.routes.root.RegisterMatrixAccountRoute
import de.connect2x.trixnity.messenger.internal.routes.root.RootRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.root.SSOLoginRoute
import de.connect2x.trixnity.messenger.internal.util.CloseAppIfPossible
import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountMethod
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModelFactory
import kotlinx.coroutines.launch
import org.koin.core.parameter.ParametersHolder

internal class AddMatrixAccountViewModelFactoryAdapter(
    private val factory: AddMatrixAccountViewModelFactory,
    private val routeNavigation: RouteNavigation,
    private val profileManager: ProfileManager,
    private val matrixClients: MatrixClients,
    private val closeAppIfPossible: CloseAppIfPossible,
) : ViewModelFactoryAdapter<AddMatrixAccountViewModel> {
    override fun create(parameters: ParametersHolder): AddMatrixAccountViewModel {
        val viewModelContext = parameters.get<ViewModelContext>().childContext("AddMatrixAccount")

        return factory.create(
            viewModelContext = viewModelContext,
            onAddMatrixAccountMethod =
                routeNavigation.navigationCallback { it ->
                    val route =
                        when (it) {
                            is AddMatrixAccountMethod.OAuth2AuthorizationCode ->
                                OAuth2AuthorizationCodeLoginRoute(it.serverUrl, it.type)
                            is AddMatrixAccountMethod.OAuth2DeviceAuthorization ->
                                OAuth2DeviceAuthorizationLoginRoute(it.serverUrl)
                            is AddMatrixAccountMethod.Password -> PasswordLoginRoute(it.serverUrl)
                            is AddMatrixAccountMethod.Register -> RegisterMatrixAccountRoute(it.serverUrl)
                            is AddMatrixAccountMethod.SSO ->
                                SSOLoginRoute(it.serverUrl, it.identityProvider?.id, it.identityProvider?.name)
                        }

                    push(route)
                },
            onCancel =
                routeNavigation.navigationCallback {
                    val isMultiProfile = profileManager.isMultiProfileEnabled.value
                    when {
                        matrixClients.value.isNotEmpty() -> {
                            replace<RootRouteMarker>(MatrixClientInitializationRoute)
                        }

                        isMultiProfile == true -> {
                            viewModelContext.coroutineScope.launch { profileManager.closeProfile() }
                        }

                        else -> {
                            viewModelContext.log.info { "There are no MatrixClients configured yet, so close the app" }
                            closeAppIfPossible()
                        }
                    }
                },
        )
    }
}
