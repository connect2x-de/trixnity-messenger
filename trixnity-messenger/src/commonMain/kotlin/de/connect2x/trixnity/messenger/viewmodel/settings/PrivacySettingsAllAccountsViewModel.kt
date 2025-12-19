package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.scopedMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get


interface PrivacySettingsAllAccountsViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onShowBlockedContactsSettings: (account: UserId) -> Unit,
        onClosePrivacySettings: () -> Unit,
    ): PrivacySettingsAllAccountsViewModel = PrivacySettingsAllAccountsViewModelImpl(
        viewModelContext,
        onShowBlockedContactsSettings,
        onClosePrivacySettings,
    )

    companion object : PrivacySettingsAllAccountsViewModelFactory
}

interface PrivacySettingsAllAccountsViewModel {
    val privacySettings: StateFlow<List<PrivacySettingsSingleAccountViewModel>>
    fun back()
}

class PrivacySettingsAllAccountsViewModelImpl(
    viewModelContext: ViewModelContext,
    onShowBlockedContactsSettings: (account: UserId) -> Unit,
    private val onClosePrivacySettings: () -> Unit,
) : ViewModelContext by viewModelContext, PrivacySettingsAllAccountsViewModel {

    private val backCallback = BackCallback {
        back()
    }

    init {
        backHandler.register(backCallback)
    }

    override val privacySettings: StateFlow<List<PrivacySettingsSingleAccountViewModel>> =
        matrixClients.scopedMapLatest { namedMatrixClients ->
            namedMatrixClients.map { (userId, _) ->
                get<PrivacySettingsSingleAccountViewModelFactory>()
                    .create(
                        viewModelContext = childContext("privacySetting-${userId}", userId = userId),
                        onShowBlockedContactsSettings = onShowBlockedContactsSettings,
                    )
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    override fun back() {
        onClosePrivacySettings()
    }
}
