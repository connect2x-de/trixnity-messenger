package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.uia.UIAController
import de.connect2x.trixnity.messenger.internal.uia.UIAStateHolder
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepPasswordViewModel
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepPasswordViewModelFactory
import kotlinx.coroutines.launch
import org.koin.core.parameter.ParametersHolder

internal class UiaStepPasswordViewModelFactoryAdapter(
    private val factory: UiaStepPasswordViewModelFactory,
    private val uiaController: UIAController,
    private val uiaStateHolder: UIAStateHolder,
) : ViewModelFactoryAdapter<UiaStepPasswordViewModel> {
    override fun create(parameters: ParametersHolder): UiaStepPasswordViewModel {
        val viewModelContext = parameters.get<ViewModelContext>().childContext("UiaStepPassword")

        return factory.create(
            viewModelContext = viewModelContext,
            uiaStep = uiaStateHolder.step,
            onNext = { viewModelContext.coroutineScope.launch { uiaController.next(it) } },
            onCancel = { viewModelContext.coroutineScope.launch { uiaController.cancel() } },
            onError = { viewModelContext.coroutineScope.launch { uiaController.error(it) } },
        )
    }
}
