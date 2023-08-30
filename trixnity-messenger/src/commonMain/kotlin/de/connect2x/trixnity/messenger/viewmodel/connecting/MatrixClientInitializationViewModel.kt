package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.GetAccountNames
import de.connect2x.trixnity.messenger.LoadStoreException
import de.connect2x.trixnity.messenger.MatrixClientService
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface MatrixClientInitializationViewModelFactory {
    fun newMatrixClientInitializationViewModel(
        viewModelContext: ViewModelContext,
        matrixClientService: MatrixClientService,
        onNoAccounts: () -> Unit,
        onInitializationSuccess: () -> Unit,
        onInitializationFailure: () -> Unit,
        onStoreFailure: (accountName: String, exception: LoadStoreException) -> Unit,
    ): MatrixClientInitializationViewModel {
        return MatrixClientInitializationViewModelImpl(
            viewModelContext,
            matrixClientService,
            onNoAccounts,
            onInitializationSuccess,
            onInitializationFailure,
            onStoreFailure
        )
    }
}

interface MatrixClientInitializationViewModel {
    val currentState: MutableStateFlow<String>
}

open class MatrixClientInitializationViewModelImpl(
    private val viewModelContext: ViewModelContext,
    private val matrixClientService: MatrixClientService,
    private val onNoAccounts: () -> Unit,
    private val onInitializationSuccess: () -> Unit,
    private val onInitializationFailure: () -> Unit,
    private val onStoreFailure: (accountName: String, exception: LoadStoreException) -> Unit,
) : ViewModelContext by viewModelContext, MatrixClientInitializationViewModel {

    override val currentState = MutableStateFlow("")
    private val getAccountNames = get<GetAccountNames>()

    init {
        coroutineScope.launch {
            retrieveMatrixClientsFromStore()
        }
    }

    private suspend fun retrieveMatrixClientsFromStore() {
        currentState.value = i18n.matrixClientInitLoading()

        val accounts = getAccountNames()
        log.info { "Init MatrixClients $accounts from Settings and Store." }
        if (accounts.isEmpty()) { // no account defined yet, show account creation
            onNoAccounts()
        } else {
            accounts.forEach { accountName ->
                retrieveMatrixClientFromStore(accountName)
            }
            onInitializationSuccess()
        }
    }

    private suspend fun retrieveMatrixClientFromStore(accountName: String) {
        val couldInitFromStore = matrixClientService.initFromStore(accountName).fold(
            onSuccess = {
                log.info { "Successfully retrieved MatrixClient $accountName from store: $it" }
                it
            },
            onFailure = { exc ->
                log.error(exc) { "Cannot retrieve MatrixClient $accountName from store." }
                if (exc is LoadStoreException) {
                    onStoreFailure(accountName, exc)
                    return
                }
                false
            }
        )
        if (couldInitFromStore) {
            currentState.value = i18n.matrixClientInitSuccess()
        } else {
            onInitializationFailure()
        }
    }
}

