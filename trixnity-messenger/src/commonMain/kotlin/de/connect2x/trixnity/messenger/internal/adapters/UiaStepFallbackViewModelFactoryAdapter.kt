package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.routes.uia.UiaStepFallbackRoute
import de.connect2x.trixnity.messenger.internal.uia.UIAController
import de.connect2x.trixnity.messenger.internal.uia.UIAStateHolder
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepFallbackViewModel
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepFallbackViewModelFactory
import kotlinx.coroutines.launch
import org.koin.core.parameter.ParametersHolder

internal class UiaStepFallbackViewModelFactoryAdapter(
    private val factory: UiaStepFallbackViewModelFactory,
    private val uiaController: UIAController,
    private val uiaStateHolder: UIAStateHolder,
) : ViewModelFactoryAdapter<UiaStepFallbackViewModel> {
    override fun create(parameters: ParametersHolder): UiaStepFallbackViewModel {
        val route = parameters.get<UiaStepFallbackRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("UiaStepFallback")

        return factory.create(
            viewModelContext = viewModelContext,
            uiaStep = uiaStateHolder.step,
            authenticationType = route.authenticationType,
            onNext = { viewModelContext.coroutineScope.launch { uiaController.next(it) } },
            onCancel = { viewModelContext.coroutineScope.launch { uiaController.cancel() } },
            onError = { viewModelContext.coroutineScope.launch { uiaController.error(it) } },
        )
    }
}
