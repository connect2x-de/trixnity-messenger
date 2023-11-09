package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext

interface RedoSelfVerificationViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onStartSelfVerification: () -> Unit,
        onClose: () -> Unit,
    ): RedoSelfVerificationViewModel {
        return RedoSelfVerificationViewModelImpl(
            viewModelContext, onStartSelfVerification, onClose,
        )
    }

    companion object : RedoSelfVerificationViewModelFactory
}

interface RedoSelfVerificationViewModel {
    val accountName: String
    fun startSelfVerification()
    fun close()
}

open class RedoSelfVerificationViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onStartSelfVerification: () -> Unit,
    private val onClose: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, RedoSelfVerificationViewModel {

    override val accountName: String = viewModelContext.accountName

    override fun startSelfVerification() {
        onStartSelfVerification()
    }

    override fun close() {
        onClose()
    }

}
