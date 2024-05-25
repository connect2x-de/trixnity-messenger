package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.LoadStoreException
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.settings.updateView
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface MatrixClientInitializationViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onNoAccounts: () -> Unit,
        onInitializationSuccess: () -> Unit,
        onInitializationFailure: () -> Unit,
        onStoreFailure: (userId: UserId, exception: LoadStoreException) -> Unit,
    ): MatrixClientInitializationViewModel {
        return MatrixClientInitializationViewModelImpl(
            viewModelContext,
            onNoAccounts,
            onInitializationSuccess,
            onInitializationFailure,
            onStoreFailure
        )
    }

    companion object : MatrixClientInitializationViewModelFactory
}

interface MatrixClientInitializationViewModel {
    val currentState: StateFlow<String>
}

open class MatrixClientInitializationViewModelImpl(
    private val viewModelContext: ViewModelContext,
    private val onNoAccounts: () -> Unit,
    private val onInitializationSuccess: () -> Unit,
    private val onInitializationFailure: () -> Unit,
    private val onStoreFailure: (userId: UserId, exception: LoadStoreException) -> Unit,
) : ViewModelContext by viewModelContext, MatrixClientInitializationViewModel {

    override val currentState = MutableStateFlow("")
    private val matrixClients = get<MatrixClients>()
    private val settings = get<MatrixMessengerSettingsHolder>()

    init {
        coroutineScope.launch {
            retrieveMatrixClientsFromStore()
        }
    }

    private suspend fun retrieveMatrixClientsFromStore() {
        currentState.value = i18n.matrixClientInitLoading()

        log.info { "init MatrixClients ${matrixClients.value.keys} from settings and store" }
        if (settings.value.base.accounts.isEmpty()) { // no account defined yet, show account creation
            onNoAccounts()
        } else {
            checkWhetherSelectedAccountIsStillValid()
            val initFromStoreResult = matrixClients.initFromStore()
            val loadStoreException = initFromStoreResult.failures.entries.find { it.value is LoadStoreException }
            when {
                loadStoreException != null -> onStoreFailure(
                    loadStoreException.key,
                    loadStoreException.value as LoadStoreException
                )

                // TODO This method might be too simplistic for the complex method of loading multiple MatrixClients.
                //  For the future: return a list of successul and failed initializations to the caller and let the caller decide
                //  what to do (e.g., a failed account might get a warning page, but one could still use the other accounts).
                initFromStoreResult.failures.isNotEmpty() -> onInitializationFailure()

                else -> {
                    currentState.value = i18n.matrixClientInitSuccess()
                    onInitializationSuccess()
                }
            }
        }
    }

    private suspend fun checkWhetherSelectedAccountIsStillValid() {
        val baseSettings = settings.value.base
        if (baseSettings.selectedAccount != null &&
            baseSettings.accounts.containsKey(baseSettings.selectedAccount).not()
        ) {
            log.debug { "found a selected account that is not present anymore" }
            if (baseSettings.accounts.size == 1) {
                log.debug { "only 1 account left -> set as the active account" }
                settings.updateView<MatrixMessengerSettingsBase> { it.copy(selectedAccount = it.accounts.keys.firstOrNull()) }
            } else {
                log.debug { "more than 1 account left -> select all of them" }
                settings.updateView<MatrixMessengerSettingsBase> {
                    it.copy(selectedAccount = null)
                }
            }
        }
    }
}

