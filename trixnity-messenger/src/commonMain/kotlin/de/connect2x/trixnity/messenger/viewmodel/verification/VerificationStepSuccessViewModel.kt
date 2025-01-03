package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


interface VerificationStepSuccessViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onVerificationSuccessOk: () -> Unit,
    ): VerificationStepSuccessViewModel {
        return VerificationStepSuccessViewModelImpl(
            viewModelContext, onVerificationSuccessOk
        )
    }

    companion object : VerificationStepSuccessViewModelFactory
}

interface VerificationStepSuccessViewModel {
    fun ok()
}

open class VerificationStepSuccessViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onVerificationSuccessOk: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, VerificationStepSuccessViewModel {

    override fun ok() {
        onVerificationSuccessOk()
    }

}
