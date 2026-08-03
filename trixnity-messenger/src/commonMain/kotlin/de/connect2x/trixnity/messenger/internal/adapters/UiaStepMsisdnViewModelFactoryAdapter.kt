package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.uia.UIAController
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepMsisdnViewModel
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepMsisdnViewModelFactory
import kotlinx.coroutines.launch
import org.koin.core.parameter.ParametersHolder

internal class UiaStepMsisdnViewModelFactoryAdapter(
    private val factory: UiaStepMsisdnViewModelFactory,
    private val uiaController: UIAController,
) : ViewModelFactoryAdapter<UiaStepMsisdnViewModel> {
    override fun create(parameters: ParametersHolder): UiaStepMsisdnViewModel {
        val viewModelContext = parameters.get<ViewModelContext>().childContext("UiaStepMsisdn")

        return factory.create(
            viewModelContext = viewModelContext,
            onCancel = { viewModelContext.coroutineScope.launch { uiaController.cancel() } },
        )
    }
}
