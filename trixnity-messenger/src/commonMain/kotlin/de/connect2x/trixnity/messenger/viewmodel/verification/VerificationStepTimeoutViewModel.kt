package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext


interface VerificationStepTimeoutViewModelFactory {
    fun newVerificationStepTimeoutViewModel(
        viewModelContext: ViewModelContext,
        onVerificationTimeoutOk: () -> Unit,
    ): VerificationStepTimeoutViewModel {
        return VerificationStepTimeoutViewModelImpl(viewModelContext, onVerificationTimeoutOk)
    }
}

interface VerificationStepTimeoutViewModel {
    fun ok()
}

open class VerificationStepTimeoutViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onVerificationTimeoutOk: () -> Unit,
) : ViewModelContext by viewModelContext, VerificationStepTimeoutViewModel {

    override fun ok() {
        onVerificationTimeoutOk()
    }

}
