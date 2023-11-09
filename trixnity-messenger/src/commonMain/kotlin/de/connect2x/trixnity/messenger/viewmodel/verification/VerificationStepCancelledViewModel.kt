package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext

interface VerificationStepCancelledViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onVerificationCancelledOk: () -> Unit,
    ): VerificationStepCancelledViewModel {
        return VerificationStepCancelledViewModelImpl(viewModelContext, onVerificationCancelledOk)
    }

    companion object : VerificationStepCancelledViewModelFactory
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
