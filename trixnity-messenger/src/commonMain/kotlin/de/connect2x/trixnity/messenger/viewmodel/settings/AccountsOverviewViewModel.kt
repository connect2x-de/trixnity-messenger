package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.GetAccountNames
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.namedMatrixClients
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.get

interface AccountsOverviewViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCreateNewAccount: () -> Unit,
        onRemoveAccount: (String) -> Unit,
        onClose: () -> Unit,
    ): AccountsOverviewViewModel {
        return AccountsOverviewViewModelImpl(viewModelContext, onCreateNewAccount, onRemoveAccount, onClose)
    }

    companion object : AccountsOverviewViewModelFactory
}

interface AccountsOverviewViewModel {
    val accountNames: StateFlow<List<String>>
    fun createNewAccount()
    fun removeAccount(accountName: String)
    fun close()
}

open class AccountsOverviewViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCreateNewAccount: () -> Unit,
    private val onRemoveAccount: (String) -> Unit,
    private val onClose: () -> Unit,
) : AccountsOverviewViewModel, ViewModelContext by viewModelContext {

    override val accountNames: StateFlow<List<String>> =
        namedMatrixClients.map { get<GetAccountNames>()() }.stateIn(coroutineScope, WhileSubscribed(), listOf())

    override fun createNewAccount() {
        onCreateNewAccount()
    }

    override fun removeAccount(accountName: String) {
        onRemoveAccount(accountName)
    }

    override fun close() {
        onClose()
    }

}