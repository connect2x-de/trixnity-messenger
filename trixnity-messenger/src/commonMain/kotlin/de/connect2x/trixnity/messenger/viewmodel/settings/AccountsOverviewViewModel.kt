package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.viewmodel.AccountInfo
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.toAccountInfo
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

interface AccountsOverviewViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCreateNewAccount: () -> Unit,
        onRemoveAccount: (UserId) -> Unit,
        onClose: () -> Unit,
    ): AccountsOverviewViewModel {
        return AccountsOverviewViewModelImpl(viewModelContext, onCreateNewAccount, onRemoveAccount, onClose)
    }

    companion object : AccountsOverviewViewModelFactory
}

interface AccountsOverviewViewModel {
    val accounts: StateFlow<List<AccountInfo>>

    fun createNewAccount()
    fun changeLocalDisplayName(userId: UserId, newLocalDisplayName: String?)
    fun removeAccount(userId: UserId)
    fun close()
}

class AccountsOverviewViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCreateNewAccount: () -> Unit,
    private val onRemoveAccount: (UserId) -> Unit,
    private val onClose: () -> Unit,
) : AccountsOverviewViewModel, ViewModelContext by viewModelContext {


    private val backCallback = BackCallback {
        close()
    }

    init {
        registerBackCallback(backCallback)
    }

    private val initials = get<Initials>()
    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val maxMediaSizeInMemory = get<MatrixMessengerConfiguration>().maxMediaSizeInMemory

    override val accounts: StateFlow<List<AccountInfo>> =
        matrixClients.toAccountInfo(coroutineScope, messengerSettings, initials, maxMediaSizeInMemory)
            .stateIn(coroutineScope, WhileSubscribed(), listOf())

    override fun createNewAccount() {
        onCreateNewAccount()
    }

    override fun changeLocalDisplayName(userId: UserId, newLocalDisplayName: String?) {
        coroutineScope.launch {
            messengerSettings.update<MatrixMessengerAccountSettingsBase>(userId) {
                it.copy(displayName = newLocalDisplayName)
            }
        }
    }

    override fun removeAccount(userId: UserId) {
        onRemoveAccount(userId)
    }

    override fun close() {
        onClose()
    }

}
