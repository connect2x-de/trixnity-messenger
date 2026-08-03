package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.uia.UIAController
import de.connect2x.trixnity.messenger.internal.uia.UIAStateHolder
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepDummyViewModel
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepDummyViewModelFactory
import kotlinx.coroutines.launch
import org.koin.core.parameter.ParametersHolder

internal class UiaStepDummyViewModelFactoryAdapter(
    private val factory: UiaStepDummyViewModelFactory,
    private val uiaController: UIAController,
    private val uiaStateHolder: UIAStateHolder,
) : ViewModelFactoryAdapter<UiaStepDummyViewModel> {
    override fun create(parameters: ParametersHolder): UiaStepDummyViewModel {
        val viewModelContext = parameters.get<ViewModelContext>().childContext("UiaStepDummy")

        return factory.create(
            viewModelContext = viewModelContext,
            uiaStep = uiaStateHolder.step,
            onNext = { viewModelContext.coroutineScope.launch { uiaController.next(it) } },
            onCancel = { viewModelContext.coroutineScope.launch { uiaController.cancel() } },
            onError = { viewModelContext.coroutineScope.launch { uiaController.error(it) } },
        )
    }
}
