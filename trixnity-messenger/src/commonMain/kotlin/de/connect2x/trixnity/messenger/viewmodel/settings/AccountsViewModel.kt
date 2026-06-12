package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import de.connect2x.trixnity.clientserverapi.model.server.profileFields
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.getMatrixClient
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.get

interface AccountsViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCloseAccounts: () -> Unit,
        onOpenAvatarCutter: (UserId, FileDescriptor) -> Unit,
        onShowAccountSetup: (UserId) -> Unit,
        onRemoveAccount: (UserId) -> Unit,
        onCreateNewAccount: () -> Unit,
    ): AccountsViewModel {
        return AccountsViewModelImpl(
            viewModelContext,
            onCloseAccounts,
            onOpenAvatarCutter,
            onShowAccountSetup,
            onRemoveAccount,
            onCreateNewAccount,
        )
    }

    companion object : AccountsViewModelFactory
}

interface AccountsViewModel {
    val accountSingleViewModels: StateFlow<List<AccountSingleViewModel>>
    val error: MutableStateFlow<String?>
    val openAvatarCutter: StateFlow<UserId?>
    val isMultiProfile: StateFlow<Boolean>
    val canChangeMultiProfileMode: StateFlow<Boolean>

    fun close()

    fun errorDismiss()

    fun openAvatarCutter(userId: UserId, file: FileDescriptor)

    fun closeAvatarCutter()

    fun createNewAccount()

    fun setMultiProfileEnabled(enabled: Boolean)
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCloseAccounts: () -> Unit,
    private val onOpenAvatarCutter: (UserId, FileDescriptor) -> Unit,
    private val onShowAccountSetup: (UserId) -> Unit,
    private val onRemoveAccount: (userId: UserId) -> Unit,
    private val onCreateNewAccount: () -> Unit,
) : ViewModelContext by viewModelContext, AccountsViewModel {
    private val profileManager = getOrNull<ProfileManager>() // If we are in single-profile mode

    override val accountSingleViewModels: StateFlow<List<AccountSingleViewModel>>
    override val error = MutableStateFlow<String?>(null)
    override val openAvatarCutter: StateFlow<UserId?>
    override val isMultiProfile: StateFlow<Boolean> =
        (profileManager?.isMultiProfileEnabled?.map { it != null && it } ?: flowOf(false)).stateIn(
            coroutineScope,
            WhileSubscribed(),
            false,
        )

    // If there is more than one profile the user cannot disable multi-profile mode
    override val canChangeMultiProfileMode: StateFlow<Boolean> =
        combine(isMultiProfile, profileManager?.profiles?.map { it.size > 1 } ?: flowOf(false)) {
                val isMultiProfile = it[0]
                val moreThanOneProfile = it[1]
                // Technically, we could encounter a case where the multi-profile mode is disabled, but there are more
                // than
                // one profiles. In this case, we should still allow the user to enable it.
                !isMultiProfile || (isMultiProfile && !moreThanOneProfile)
            }
            .stateIn(coroutineScope, WhileSubscribed(), true)

    private val backCallback = BackCallback { close() }

    init {
        registerBackCallback(backCallback)
        accountSingleViewModels =
            AccountSingleViewModels(
                    parentContext = viewModelContext.childContext("AccountSingleViewModels"),
                    matrixClients = matrixClients,
                    error = error,
                    accountSingleViewModelFactory = get(),
                    showAccountSetup = onShowAccountSetup,
                    removeAccount = onRemoveAccount,
                )
                .viewModels()
                .stateIn(coroutineScope, WhileSubscribed(), emptyList())

        openAvatarCutter =
            accountSingleViewModels
                .flatMapLatest { profilesOfAccounts ->
                    combine(
                        profilesOfAccounts.map { profileOfAccount ->
                            profileOfAccount.openAvatarCutter.map { profileOfAccount.userId to it }
                        }
                    ) { list ->
                        list.find { (_, openAvatarChooser) -> openAvatarChooser }?.first
                    }
                }
                .stateIn(coroutineScope, SharingStarted.Eagerly, null)
    }

    override fun close() {
        onCloseAccounts()
    }

    override fun errorDismiss() {
        error.value = null
    }

    override fun closeAvatarCutter() {
        accountSingleViewModels.value.forEach { it.openAvatarCutter.value = false }
    }

    override fun createNewAccount() = onCreateNewAccount()

    override fun openAvatarCutter(userId: UserId, file: FileDescriptor) {
        if (getMatrixClient(userId).serverData.value?.capabilities?.capabilities?.profileFields?.enabled ?: true) {
            onOpenAvatarCutter(userId, file)
        } else {
            log.warn { "Missing server capability to change the user avatar." }
        }
    }

    override fun setMultiProfileEnabled(enabled: Boolean) {
        coroutineScope.launch { profileManager?.setMultiProfileEnabled(enabled) }
    }
}

private class AccountSingleViewModels(
    private val parentContext: ViewModelContext,
    private val matrixClients: MatrixClients,
    private val error: MutableStateFlow<String?>,
    private val accountSingleViewModelFactory: AccountSingleViewModelFactory,
    private val showAccountSetup: (UserId) -> Unit,
    private val removeAccount: (UserId) -> Unit,
) {

    fun viewModels(): Flow<List<AccountSingleViewModel>> {
        return matrixClients
            .map { clients -> clients.keys }
            .scan(emptyMap<UserId, Entry>()) { entries, userIds -> entries.reconcileWith(userIds) }
            .map { entries -> entries.values.map(Entry::viewModel) }
    }

    private fun Map<UserId, Entry>.reconcileWith(currentUserIds: Set<UserId>): Map<UserId, Entry> {
        val removedUserIds = keys - currentUserIds

        removedUserIds.forEach { userId -> getValue(userId).destroy() }

        return currentUserIds.associateWith { userId -> this[userId] ?: createEntry(userId) }
    }

    private fun createEntry(userId: UserId): Entry {
        val lifecycle = LifecycleRegistry(initialState = Lifecycle.State.STARTED)

        return Entry(lifecycle = lifecycle, viewModel = createViewModel(userId = userId, lifecycle = lifecycle))
    }

    private fun createViewModel(userId: UserId, lifecycle: Lifecycle): AccountSingleViewModel {
        return accountSingleViewModelFactory.create(
            viewModelContext =
                parentContext.childContextWithOwnLifecycle(
                    name = "account-settings-${userId.full}",
                    lifecycle = lifecycle,
                    userId = userId,
                ),
            userId = userId,
            error = error,
            showAccountSetup = { showAccountSetup(userId) },
            removeAccount = { removeAccount(userId) },
        )
    }

    private data class Entry(val lifecycle: LifecycleRegistry, val viewModel: AccountSingleViewModel) {
        fun destroy() {
            lifecycle.destroy()
        }
    }
}
