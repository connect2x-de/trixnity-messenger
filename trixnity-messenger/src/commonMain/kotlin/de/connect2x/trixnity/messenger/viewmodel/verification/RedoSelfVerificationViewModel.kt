package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import net.folivo.trixnity.core.model.UserId

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
    val userId: UserId
    fun startSelfVerification()
    fun close()
}

open class RedoSelfVerificationViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onStartSelfVerification: () -> Unit,
    private val onClose: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, RedoSelfVerificationViewModel {

    override fun startSelfVerification() {
        onStartSelfVerification()
    }

    override fun close() {
        onClose()
    }

    private val backCallback = BackCallback {
        close()
    }

    init {
        registerBackCallback(backCallback)
    }
}
