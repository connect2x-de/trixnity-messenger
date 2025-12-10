package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
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
    val privacySettings: List<PrivacySettingsSingleAccountViewModel>
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
