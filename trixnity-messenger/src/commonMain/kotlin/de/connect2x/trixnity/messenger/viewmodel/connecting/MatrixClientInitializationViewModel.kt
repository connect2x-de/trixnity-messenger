package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.GetAccountNames
import de.connect2x.trixnity.messenger.MatrixClientService
import de.connect2x.trixnity.messenger.StoreAccessException
import de.connect2x.trixnity.messenger.StoreLockedException
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
        accountName: String,
        onInitializationFailure: () -> Unit,
        onStoreFailure: (Result<Unit>) -> Unit,
    ): MatrixClientInitializationViewModel {
        return MatrixClientInitializationViewModelImpl(
            viewModelContext, matrixClientService, accountName, onInitializationFailure, onStoreFailure
        )
    }
}

interface MatrixClientInitializationViewModel {
    val currentState: MutableStateFlow<String>
}

open class MatrixClientInitializationViewModelImpl(
    private val viewModelContext: ViewModelContext,
    private val matrixClientService: MatrixClientService,
    private val accountName: String,
    private val onInitializationFailure: () -> Unit,
    private val onStoreFailure: (Result<Unit>) -> Unit,
) : ViewModelContext by viewModelContext, MatrixClientInitializationViewModel {

    override val currentState = MutableStateFlow("")

    init {
        coroutineScope.launch {
            retrieveMatrixClientFromStore()
        }
    }

    private suspend fun retrieveMatrixClientFromStore() {
        log.info { "Init MatrixClient from Settings and Store." }
        currentState.value = i18n.matrixClientInitLoading()
        val couldInitFromStore = matrixClientService.initFromStore(accountName).fold(
            onSuccess = {
                log.info { "Successfully retrieved MatrixClient from store: $it" }
                it
            },
            onFailure = { exc ->
                log.error(exc) { "Cannot retrieve MatrixClient from store." }
                if (exc is StoreLockedException || exc is StoreAccessException) {
                    onStoreFailure(Result.failure(exc))
                    return
                }
                false
            }
        )
        if (couldInitFromStore) {
            log.info { "Retrieved MatrixClient from store." }
            currentState.value = i18n.matrixClientInitSuccess()
            log.debug { "MatrixClient state: ${currentState.value}" }
        } else {
            log.info { "Could not retrieve MatrixClient from store." }
            onInitializationFailure()
        }
    }
}

