package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import de.connect2x.trixnity.core.model.UserId

interface RemoveMatrixAccountViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        userId: UserId,
        onRemoveCompleted: () -> Unit,
    ): RemoveMatrixAccountViewModel {
        return RemoveMatrixAccountViewModelImpl(
            viewModelContext,
            userId,
            onRemoveCompleted,
        )
    }

    companion object : RemoveMatrixAccountViewModelFactory
}

interface RemoveMatrixAccountViewModel {
    val userId: UserId
    val error: StateFlow<String?>

    fun tryAgain(force: Boolean = false)
    fun close()
}

class RemoveMatrixAccountViewModelImpl(
    val viewModelContext: ViewModelContext,
    override val userId: UserId,
    val onRemoveCompleted: () -> Unit,
) : ViewModelContext by viewModelContext, RemoveMatrixAccountViewModel {

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()
    private val logoutMutex = Mutex()

    init {
        coroutineScope.launch {
            logout()
        }
    }

    override fun close() {
        onRemoveCompleted()
    }

    private suspend fun logout(force: Boolean = false) {
        if (logoutMutex.isLocked) return
        logoutMutex.withLock {
            _error.value = null
            matrixClients.logout(userId)
                .onSuccess {
                    log.debug { "logout completed" }
                    onRemoveCompleted()
                }
                .onFailure { e ->
                    if (force) {
                        matrixClients.remove(userId)
                            .onFailure {
                                log.error(it) { "cannot remove account $userId (force=$force)" }
                                _error.value = i18n.logoutFailure()
                            }
                    } else {
                        log.error(e) { "cannot log out of account $userId" }
                        _error.value = i18n.logoutFailure()
                    }
                }
        }
    }

    override fun tryAgain(force: Boolean) {
        coroutineScope.launch {
            logout(force)
        }
    }
}
