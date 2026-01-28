package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.messenger.util.CloseApp
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUia
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import de.connect2x.trixnity.client.key
import de.connect2x.trixnity.core.model.UserId
import org.koin.core.component.get

interface CrossSigningBootstrapViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onClose: (UserId) -> Unit,
    ): CrossSigningBootstrapViewModel = CrossSigningBootstrapViewModelImpl(
        viewModelContext, onClose
    )

    companion object : CrossSigningBootstrapViewModelFactory
}

interface CrossSigningBootstrapViewModel {
    val userId: UserId

    val recoveryKey: StateFlow<String?>
    val recoveryKeyPart1: StateFlow<String?>
    val recoveryKeyPart2: StateFlow<String?>
    val recoveryKeyCopied: StateFlow<Boolean>

    val error: StateFlow<String?>
    val isBootstrapRunning: StateFlow<Boolean>

    fun startCrossSigningBootstrap()
    fun confirmRecoveryKeyCopied()
    fun close()
    fun closeMessenger()
}

open class CrossSigningBootstrapViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onClose: (UserId) -> Unit,
) : MatrixClientViewModelContext by viewModelContext, CrossSigningBootstrapViewModel {

    override val recoveryKey = MutableStateFlow<String?>(null)
    override val recoveryKeyPart1 = recoveryKey.map {
        it?.split(" ")?.take(6)?.joinToString(" ")
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val recoveryKeyPart2 = recoveryKey.map {
        it?.split(" ")?.drop(6)?.joinToString(" ")
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val recoveryKeyCopied = MutableStateFlow(false)

    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val isBootstrapRunning = MutableStateFlow(false)
    private val authorizeUia = get<AuthorizeUia>()

    override fun startCrossSigningBootstrap() {
        if (isBootstrapRunning.getAndUpdate { true }.not()) {
            coroutineScope.launch {
                error.value = null
                val bootstrap = matrixClient.key.bootstrapCrossSigning()
                val result = authorizeUia { bootstrap.result }
                when (result) {
                    is AuthorizeUiaResult.CancelledByUser -> {
                        error.value = result.message
                    }

                    is AuthorizeUiaResult.Error -> {
                        error.value = i18n.bootstrapErrorAccount(result.exception.errorResponse.error)
                    }

                    is AuthorizeUiaResult.UnexpectedError -> {
                        error.value = result.message
                    }

                    is AuthorizeUiaResult.Success -> {
                        log.debug { "successfully completed bootstrap authorization" }
                        recoveryKey.value = bootstrap.recoveryKey
                    }
                }
            }.invokeOnCompletion { isBootstrapRunning.value = false }
        }
    }

    override fun confirmRecoveryKeyCopied() {
        recoveryKeyCopied.value = true
    }

    override fun close() {
        onClose(userId)
    }

    override fun closeMessenger() {
        getOrNull<CloseApp>()?.invoke()
    }
}
