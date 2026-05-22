package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext

interface VerificationStepRejectedViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onVerificationRejectedOk: () -> Unit,
    ): VerificationStepRejectedViewModel {
        return VerificationStepRejectedViewModelImpl(viewModelContext, onVerificationRejectedOk)
    }

    companion object : VerificationStepRejectedViewModelFactory
}

interface VerificationStepRejectedViewModel {
    fun ok()
}

open class VerificationStepRejectedViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onVerificationRejectedOk: () -> Unit,
) : ViewModelContext by viewModelContext, VerificationStepRejectedViewModel {

    override fun ok() {
        onVerificationRejectedOk()
    }
}
