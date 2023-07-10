package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext

interface VerificationStepCancelledViewModelFactory {
    fun newVerificationStepCancelledViewModel(
        viewModelContext: ViewModelContext,
        onVerificationCancelledOk: () -> Unit,
    ): VerificationStepCancelledViewModel {
        return VerificationStepCancelledViewModelImpl(viewModelContext, onVerificationCancelledOk)
    }
}

interface VerificationStepCancelledViewModel {
    fun ok()
}

open class VerificationStepCancelledViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onVerificationCancelledOk: () -> Unit,
) : ViewModelContext by viewModelContext, VerificationStepCancelledViewModel {

    override fun ok() {
        onVerificationCancelledOk()
    }
}
