package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.trixnity.messenger.matrixClientOrThrow
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.namedMatrixClients
import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.previewImageByteArray
import de.connect2x.trixnity.messenger.viewmodel.util.scopedMapLatest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface AccountViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onAccountSelected: (String?) -> Unit,
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
    val allAccounts: StateFlow<List<Account>>
    val activeAccount: StateFlow<Account?>

    fun selectActiveAccount(accountName: String?)
    fun userSettings()
    fun appInfo()
}

open class AccountViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onAccountSelected: (String?) -> Unit,
    private val onUserSettingsSelected: () -> Unit,
    private val onShowAppInfo: () -> Unit,
) : ViewModelContext by viewModelContext, AccountViewModel {
    private val initials = get<Initials>()
    private val messengerSettings = get<MessengerSettings>()

    private val activeAccountName: MutableStateFlow<String?> = MutableStateFlow(messengerSettings.activeAccount)
    override val allAccounts: StateFlow<List<Account>>
    override val activeAccount: StateFlow<Account?>

    init {
        allAccounts = namedMatrixClients.scopedMapLatest { namedMatrixClients ->
            namedMatrixClients.map { namedMatrixClient ->
                val accountName = namedMatrixClient.accountName
                log.info { "account: $accountName" }
                val matrixClient = namedMatrixClient.matrixClientOrThrow()
                val displayNameFlow = matrixClient.displayName.map { it ?: matrixClient.userId.localpart }
                    .stateIn(this, SharingStarted.WhileSubscribed(), matrixClient.userId.localpart)
                Account(
                    accountName = accountName,
                    displayName = displayNameFlow,
                    initials = displayNameFlow.map { initials.compute(it) }
                        .stateIn(this, SharingStarted.WhileSubscribed(), ""),
                    avatar = matrixClient.avatarUrl.map { avatarUrlOrNull ->
                        avatarUrlOrNull?.let { avatarUrl ->
                            matrixClient.media.getThumbnail(
                                avatarUrl,
                                avatarSize().toLong(),
                                avatarSize().toLong(),
                            ).fold(
                                onSuccess = { it.toByteArray() },
                                onFailure = {
                                    log.error(it) { "Cannot load user avatar" }
                                    null
                                }
                            )
                        }
                    }.stateIn(this, SharingStarted.WhileSubscribed(), null)
                )
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), listOf())

        activeAccount = combine(
            allAccounts,
            activeAccountName,
        ) { allAccounts, activeAccountName ->
            when {
                allAccounts.size == 1 -> allAccounts[0]
                activeAccountName != null -> allAccounts.find { account -> account.accountName == activeAccountName }
                else -> null
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    }

    override fun selectActiveAccount(accountName: String?) {
        activeAccountName.value = accountName
        messengerSettings.activeAccount = accountName
        onAccountSelected(accountName)
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
    override val allAccounts: MutableStateFlow<List<Account>> = MutableStateFlow(
        listOf(
            Account(
                accountName = "@bruce.wayne:localhost",
                displayName = MutableStateFlow("Bruce Wayne"),
                initials = MutableStateFlow("BW"),
                avatar = MutableStateFlow(previewImageByteArray()),
            ),
            Account(
                accountName = "@scrooge.mcduck:localhost",
                displayName = MutableStateFlow("Scrooge McDuck"),
                initials = MutableStateFlow("SM"),
                avatar = MutableStateFlow(null),
            ),
            Account(
                accountName = "@arthur.dent:localhost",
                displayName = MutableStateFlow("Arthur Dent"),
                initials = MutableStateFlow("AD"),
                avatar = MutableStateFlow(null),
            ),
        )
    )
    override val activeAccount: MutableStateFlow<Account?> = MutableStateFlow(null)

    override fun selectActiveAccount(accountName: String?) {
    }

    override fun userSettings() {
    }

    override fun appInfo() {
    }

}