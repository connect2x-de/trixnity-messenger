package de.connect2x.trixnity.messenger.viewmodel.uia

import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationType

interface UiaStepMsisdnViewModelFactory {
    fun create(viewModelContext: ViewModelContext, onCancel: () -> Unit): UiaStepMsisdnViewModel {
        return UiaStepMsisdnViewModelImpl(viewModelContext, onCancel)
    }

    companion object : UiaStepMsisdnViewModelFactory
}

interface UiaStepMsisdnViewModel {
    val error: StateFlow<String?>
    fun cancel()
}

class UiaStepMsisdnViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCancel: () -> Unit,
): UiaStepMsisdnViewModel, ViewModelContext by viewModelContext {
    override val error: StateFlow<String?> = flowOf(i18n.uiaFallbackNotSupported(AuthenticationType.Msisdn))
        .stateIn(coroutineScope, SharingStarted.Eagerly, i18n.uiaFallbackNotSupported(AuthenticationType.Msisdn))

    override fun cancel() {
        onCancel()
    }
}
