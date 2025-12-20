package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.getMatrixClient
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.scopedMapLatest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.clientserverapi.model.server.setAvatarUrl
import net.folivo.trixnity.clientserverapi.model.server.setDisplayName
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface ProfileViewModelFactory {
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

    companion object : ProfileViewModelFactory
}

// TODO !!! This is totally cursed. The parent should not manipulate the child !!!
interface AccountsViewModel {
    val accountSingleViewModels: StateFlow<List<AccountSingleViewModel>>
    val error: MutableStateFlow<String?>
    val openAvatarCutter: StateFlow<UserId?>
    val isMultiProfile: StateFlow<Boolean>
    val canChangeMultiProfileMode: StateFlow<Boolean>

    fun close()
    fun errorDismiss()
    fun cancelEditDisplayName(userId: UserId)
    fun saveDisplayName(userId: UserId)
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
        (profileManager?.isMultiProfileEnabled?.map { it != null && it } ?: flowOf(false))
            .stateIn(coroutineScope, WhileSubscribed(), false)

    // If there is more than one profile the user cannot disable multi-profile mode
    override val canChangeMultiProfileMode: StateFlow<Boolean> =
        combine(isMultiProfile, profileManager?.profiles?.map { it.size > 1 } ?: flowOf(false)) {
            val isMultiProfile = it[0]
            val moreThanOneProfile = it[1]
            // Technically, we could encounter a case where the multi-profile mode is disabled, but there are more than
            // one profiles. In this case, we should still allow the user to enable it.
            !isMultiProfile || (isMultiProfile && !moreThanOneProfile)
        }.stateIn(coroutineScope, WhileSubscribed(), true)

    private val backCallback = BackCallback {
        close()
    }

    init {
        registerBackCallback(backCallback)
        accountSingleViewModels = matrixClients.scopedMapLatest { matrixClients ->
            matrixClients.map { (userId, _) ->
                this@AccountsViewModelImpl.get<ProfileSingleViewModelFactory>().create(
                    viewModelContext.childContext(this@AccountsViewModelImpl),
                    userId,
                    error,
                    showAccountSetup = { onShowAccountSetup(userId) },
                    removeAccount = { onRemoveAccount(userId) }
                )
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

        openAvatarCutter = accountSingleViewModels.flatMapLatest { profilesOfAccounts ->
            combine(profilesOfAccounts.map { profileOfAccount -> profileOfAccount.openAvatarCutter.map { profileOfAccount.userId to it } }) { list ->
                list.find { (_, openAvatarChooser) -> openAvatarChooser }?.first
            }
        }.stateIn(coroutineScope, SharingStarted.Eagerly, null)
    }

    override fun close() {
        onCloseAccounts()
    }

    override fun errorDismiss() {
        error.value = null
    }

    override fun cancelEditDisplayName(userId: UserId) {
        getEditDisplayNameFlow(userId)?.also {
            it.update(getDisplayNameFlow(userId)?.value ?: "")
        }
    }


    override fun saveDisplayName(userId: UserId) {
        val newDisplayName = getEditDisplayNameFlow(userId)?.value?.text
        if (newDisplayName != getDisplayNameFlow(userId)?.value) {
            coroutineScope.launch {
                val matrixClient = getMatrixClient(userId)
                if (matrixClient.serverData.value?.capabilities?.capabilities?.setDisplayName?.enabled ?: true) {
                    log.debug { "set new display name in account $userId: $newDisplayName" }
                    matrixClient.setDisplayName(newDisplayName)
                        .onFailure {
                            log.error(it) { "Cannot set display name." }
                            if (it is MatrixServerException && it.errorResponse is ErrorResponse.Forbidden) {
                                error.value = i18n.profileNameForbidden()
                            } else {
                                error.value = i18n.profileNameError()
                            }
                        }
                } else {
                    log.warn { "Missing server capability to set the display name." }
                }
            }
        }
    }

    override fun openAvatarCutter(userId: UserId, file: FileDescriptor) {
        if (getMatrixClient(userId).serverData.value?.capabilities?.capabilities?.setAvatarUrl?.enabled ?: true) {
            onOpenAvatarCutter(userId, file)
        } else {
            log.warn { "Missing server capability to change the user avatar." }
        }
    }

    override fun closeAvatarCutter() {
        accountSingleViewModels.value.forEach { it.openAvatarCutter.value = false }
    }

    override fun createNewAccount() = onCreateNewAccount()

    private fun getDisplayNameFlow(userId: UserId) =
        accountSingleViewModels.value.find { it.userId == userId }?.displayName

    private fun getEditDisplayNameFlow(userId: UserId) =
        accountSingleViewModels.value.find { it.userId == userId }?.editDisplayName

    override fun setMultiProfileEnabled(enabled: Boolean) {
        coroutineScope.launch { profileManager?.setMultiProfileEnabled(enabled) }
    }
}
