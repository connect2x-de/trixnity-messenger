package de.connect2x.trixnity.messenger.viewmodel.uia

import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationType

interface UiaStepEmailIdentityViewModelFactory {
    fun create(viewModelContext: ViewModelContext, onCancel: () -> Unit): UiaStepEmailIdentityViewModel {
        return UiaStepEmailIdentityViewModelImpl(viewModelContext, onCancel)
    }

    companion object : UiaStepEmailIdentityViewModelFactory
}

interface UiaStepEmailIdentityViewModel {
    val error: StateFlow<String?>
    fun cancel()
}

class UiaStepEmailIdentityViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCancel: () -> Unit,
) : UiaStepEmailIdentityViewModel, ViewModelContext by viewModelContext {
    override val error: StateFlow<String?> = flowOf(i18n.uiaFallbackNotSupported(AuthenticationType.EmailIdentity))
        .stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            initialValue = i18n.uiaFallbackNotSupported(AuthenticationType.EmailIdentity)
        )

    override fun cancel() {
        onCancel()
    }

}
