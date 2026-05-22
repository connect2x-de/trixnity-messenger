package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import org.koin.core.component.get

interface PrivacySettingsAllAccountsViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onShowBlockedContactsSettings: (account: UserId) -> Unit,
        onClosePrivacySettings: () -> Unit,
    ): PrivacySettingsAllAccountsViewModel =
        PrivacySettingsAllAccountsViewModelImpl(viewModelContext, onShowBlockedContactsSettings, onClosePrivacySettings)

    companion object : PrivacySettingsAllAccountsViewModelFactory
}

interface PrivacySettingsAllAccountsViewModel {
    val privacySettings: List<PrivacySettingsSingleAccountViewModel>

    fun back()
}

class PrivacySettingsAllAccountsViewModelImpl(
    viewModelContext: ViewModelContext,
    onShowBlockedContactsSettings: (account: UserId) -> Unit,
    private val onClosePrivacySettings: () -> Unit,
) : ViewModelContext by viewModelContext, PrivacySettingsAllAccountsViewModel {

    private val backCallback = BackCallback { back() }

    init {
        registerBackCallback(backCallback)
    }

    override val privacySettings: List<PrivacySettingsSingleAccountViewModel> =
        matrixClients.value.map { (userId, _) ->
            get<PrivacySettingsSingleAccountViewModelFactory>()
                .create(
                    viewModelContext = childContext("privacySetting-${userId}", userId = userId),
                    onShowBlockedContactsSettings = onShowBlockedContactsSettings,
                )
        }

    override fun back() {
        onClosePrivacySettings()
    }
}
