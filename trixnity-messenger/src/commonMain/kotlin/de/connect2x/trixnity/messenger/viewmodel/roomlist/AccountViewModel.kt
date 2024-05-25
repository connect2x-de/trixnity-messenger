package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.settings.updateView
import de.connect2x.trixnity.messenger.viewmodel.AccountInfo
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.toAccountInfo
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.previewImageByteArray
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface AccountViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onAccountSelected: (UserId?) -> Unit,
        onUserSettingsSelected: () -> Unit,
        onShowAppInfo: () -> Unit,
    ): AccountViewModel {
        return AccountViewModelImpl(
            viewModelContext, onAccountSelected, onUserSettingsSelected, onShowAppInfo
        )
    }

    companion object : AccountViewModelFactory
}

interface AccountViewModel {
    val accounts: StateFlow<List<AccountInfo>>

    /**
     * If `null`, no account is selected -> all accounts should be displayed.
     */
    val activeAccount: StateFlow<UserId?>

    /**
     * When there is only one account, UIs can decide to display the information about the singular account differently (i.e., without a selection of other accounts).
     */
    val isSingleAccount: StateFlow<Boolean>

    fun selectActiveAccount(userId: UserId?)
    fun userSettings()
    fun appInfo()
}

open class AccountViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onAccountSelected: (UserId?) -> Unit,
    private val onUserSettingsSelected: () -> Unit,
    private val onShowAppInfo: () -> Unit,
) : ViewModelContext by viewModelContext, AccountViewModel {
    private val initials = get<Initials>()
    private val messengerSettings = get<MatrixMessengerSettingsHolder>()

    override val accounts: StateFlow<List<AccountInfo>> =
        matrixClients.toAccountInfo(messengerSettings, initials)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), listOf())

    override val activeAccount: StateFlow<UserId?> =
        messengerSettings.map { it.base.selectedAccount }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val isSingleAccount: StateFlow<Boolean> = accounts.map { it.size <= 1 }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)

    override fun selectActiveAccount(userId: UserId?) {
        coroutineScope.launch {
            messengerSettings.updateView<MatrixMessengerSettingsBase> { it.copy(selectedAccount = userId) }
            onAccountSelected(userId)
        }
    }

    override fun userSettings() {
        coroutineScope.launch {
            onUserSettingsSelected()
        }
    }

    override fun appInfo() {
        coroutineScope.launch {
            onShowAppInfo()
        }
    }
}

class PreviewAccountViewModel : AccountViewModel {
    override val accounts: MutableStateFlow<List<AccountInfo>> = MutableStateFlow(
        listOf(
            AccountInfo(
                userId = UserId("@bruce.wayne:localhost"),
                displayName = "Bruce Wayne",
                initials = "BW",
                avatar = previewImageByteArray(),
                displayColor = null,
            ),
            AccountInfo(
                userId = UserId("@scrooge.mcduck:localhost"),
                displayName = "Scrooge McDuck",
                initials = "SM",
                avatar = null,
                displayColor = null,
            ),
            AccountInfo(
                userId = UserId("@arthur.dent:localhost"),
                displayName = "Arthur Dent",
                initials = "AD",
                avatar = null,
                displayColor = null,
            ),
        )
    )
    override val activeAccount: MutableStateFlow<UserId?> = MutableStateFlow(null)
    override val isSingleAccount: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override fun selectActiveAccount(userId: UserId?) {
    }

    override fun userSettings() {
    }

    override fun appInfo() {
    }

}
