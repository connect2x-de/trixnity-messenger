package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.MatrixClientService
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

interface MatrixClientLogoutViewModelFactory {
    fun newMatrixClientLogoutViewModel(
        viewModelContext: ViewModelContext,
        matrixClientService: MatrixClientService,
        accountName: String,
        onLogoutCompleted: () -> Unit,
    ): MatrixClientLogoutViewModel {
        return MatrixClientLogoutViewModelImpl(
            viewModelContext,
            matrixClientService,
            accountName,
            onLogoutCompleted,
        )
    }
}

interface MatrixClientLogoutViewModel {
    val accountName: String
    val error: StateFlow<String?>
    fun close()
}

class MatrixClientLogoutViewModelImpl(
    val viewModelContext: ViewModelContext,
    val matrixClientService: MatrixClientService,
    override val accountName: String,
    val onLogoutCompleted: () -> Unit,
) : ViewModelContext by viewModelContext, MatrixClientLogoutViewModel {

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    init {
        coroutineScope.launch {
            matrixClientService.logout(accountName)
                .onSuccess {
                    log.debug { "logout completed" }
                    onLogoutCompleted()
                }
                .onFailure {
                    log.error(it) { "cannot log out of account $accountName" }
                    _error.value = get<I18n>().logoutFailure()
                }
        }
    }

    override fun close() {
        onLogoutCompleted()
    }

}