package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    val redactionWarningEnabled: StateFlow<Boolean?>
    fun back()
    fun toggleRedactionWarningEnabled()
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
        registerBackCallback(backCallback)
    }

    val settings = get<MatrixMessengerSettingsHolder>()

    override val redactionWarningEnabled: StateFlow<Boolean?> =
        settings.map { it.base.showRedactionWarning }.stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(), null
        )

    override fun toggleRedactionWarningEnabled() {
        coroutineScope.launch {
            settings.update<MatrixMessengerSettingsBase> {
                it.copy(showRedactionWarning = !it.showRedactionWarning)
            }
        }
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
