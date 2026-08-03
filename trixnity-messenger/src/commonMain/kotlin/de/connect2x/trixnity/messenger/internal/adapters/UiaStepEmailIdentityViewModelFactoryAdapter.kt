package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.uia.UIAController
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepEmailIdentityViewModel
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepEmailIdentityViewModelFactory
import kotlinx.coroutines.launch
import org.koin.core.parameter.ParametersHolder

internal class UiaStepEmailIdentityViewModelFactoryAdapter(
    private val factory: UiaStepEmailIdentityViewModelFactory,
    private val uiaController: UIAController,
) : ViewModelFactoryAdapter<UiaStepEmailIdentityViewModel> {
    override fun create(parameters: ParametersHolder): UiaStepEmailIdentityViewModel {
        val viewModelContext = parameters.get<ViewModelContext>().childContext("UiaStepEmailIdentity")

        return factory.create(
            viewModelContext = viewModelContext,
            onCancel = { viewModelContext.coroutineScope.launch { uiaController.cancel() } },
        )
    }
}
