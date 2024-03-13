package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.scopedMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get


interface PrivacySettingsAllUsersViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onShowBlockedContactsSettings: (userId: UserId) -> Unit,
        onClosePrivacySettings: () -> Unit,
    ): PrivacySettingsAllUsersViewModel = PrivacySettingsAllUsersViewModelImpl(
        viewModelContext,
        onShowBlockedContactsSettings,
        onClosePrivacySettings,
    )

    companion object : PrivacySettingsAllUsersViewModelFactory
}

interface PrivacySettingsAllUsersViewModel {
    val privacySettings: StateFlow<List<PrivacySettingsSingleUserViewModel>>
    fun back()
}

open class PrivacySettingsAllUsersViewModelImpl(
    viewModelContext: ViewModelContext,
    onShowBlockedContactsSettings: (userId: UserId) -> Unit,
    private val onClosePrivacySettings: () -> Unit,
) : ViewModelContext by viewModelContext, PrivacySettingsAllUsersViewModel {

    override val privacySettings: StateFlow<List<PrivacySettingsSingleUserViewModel>> =
        matrixClients.scopedMapLatest { namedMatrixClients ->
            namedMatrixClients.map { (userId, _) ->
                get<PrivacySettingsSingleUserViewModelFactory>()
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
