package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.routes.uia.UiaActionConfirmationRoute
import de.connect2x.trixnity.messenger.internal.uia.UIAController
import de.connect2x.trixnity.messenger.internal.uia.UIAStateHolder
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaActionConfirmationViewModel
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaActionConfirmationViewModelFactory
import kotlinx.coroutines.launch
import org.koin.core.parameter.ParametersHolder

internal class UiaActionConfirmationViewModelFactoryAdapter(
    private val factory: UiaActionConfirmationViewModelFactory,
    private val uiaController: UIAController,
    private val uiaStateHolder: UIAStateHolder,
) : ViewModelFactoryAdapter<UiaActionConfirmationViewModel> {
    override fun create(parameters: ParametersHolder): UiaActionConfirmationViewModel {
        val route = parameters.get<UiaActionConfirmationRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("UiaActionConfirmation")

        return factory.create(
            viewModelContext = viewModelContext,
            message = route.message,
            action = uiaStateHolder::executeAction,
            onNext = { viewModelContext.coroutineScope.launch { uiaController.next(it) } },
            onCancel = { viewModelContext.coroutineScope.launch { uiaController.cancel() } },
            onError = { viewModelContext.coroutineScope.launch { uiaController.error(it) } },
        )
    }
}
