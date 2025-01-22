package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.MatrixClientInitializationException
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.update
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
        onInitializationFailure: (userId: UserId, exception: MatrixClientInitializationException) -> Unit,
    ): MatrixClientInitializationViewModel {
        return MatrixClientInitializationViewModelImpl(
            viewModelContext,
            onNoAccounts,
            onInitializationSuccess,
            onInitializationFailure,
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
    private val onInitializationFailure: (userId: UserId, exception: MatrixClientInitializationException) -> Unit,
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
            when {
                initFromStoreResult.failures.isNotEmpty() -> {
                    val firstFailure = initFromStoreResult.failures.entries.first()
                    onInitializationFailure(firstFailure.key, firstFailure.value)
                }

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
                settings.update<MatrixMessengerSettingsBase> { it.copy(selectedAccount = it.accounts.keys.firstOrNull()) }
            } else {
                log.debug { "more than 1 account left -> select all of them" }
                settings.update<MatrixMessengerSettingsBase> {
                    it.copy(selectedAccount = null)
                }
            }
        }
    }
}

