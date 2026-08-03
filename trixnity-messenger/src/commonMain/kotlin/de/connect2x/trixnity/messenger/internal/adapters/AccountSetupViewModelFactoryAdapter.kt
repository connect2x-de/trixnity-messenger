package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.internal.navigation.ResultEventBus
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.clear
import de.connect2x.trixnity.messenger.internal.navigation.getResult
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.AccountSetupRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.SelfVerificationRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.SelfVerificationRouteMarker
import de.connect2x.trixnity.messenger.internal.verification.ChangeVerificationCompleteStatus
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupViewModelFactory
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.koin.core.parameter.ParametersHolder

internal class AccountSetupViewModelFactoryAdapter(
    private val factory: AccountSetupViewModelFactory,
    private val routeNavigation: RouteNavigation,
    private val matrixMessengerSettingsHolder: MatrixMessengerSettingsHolder,
    private val resultEventBus: ResultEventBus,
) : ViewModelFactoryAdapter<AccountSetupViewModel> {
    override fun create(parameters: ParametersHolder): AccountSetupViewModel {
        val route = parameters.get<AccountSetupRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("AccountSetup", route.userId)

        val accountSetupViewModel =
            factory.create(
                viewModelContext = viewModelContext,
                onStartVerification =
                    routeNavigation.navigationCallback { userId, _ ->
                        replace<SelfVerificationRouteMarker>(SelfVerificationRoute(userId))
                    },
                onWizardClose =
                    routeNavigation.navigationCallback { userId ->
                        viewModelContext.coroutineScope.launch {
                            matrixMessengerSettingsHolder.update<MatrixMessengerAccountSettingsBase>(userId) {
                                it.copy(accountSetupFinished = true)
                            }
                        }
                        clear<AccountSetupRoute>()
                    },
            )

        viewModelContext.coroutineScope.launch {
            resultEventBus
                .getResult<ChangeVerificationCompleteStatus>()
                .filter { it.userId == route.userId }
                .collect { accountSetupViewModel.changeVerificationCompleteStatus(it.newVerificationCompleteStatus) }
        }

        return accountSetupViewModel
    }
}
