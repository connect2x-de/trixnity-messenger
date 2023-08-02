package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.namedMatrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.scopedMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.get

interface PrivacySettingsViewModelFactory {
    fun newPrivacySettingsViewModel(
        viewModelContext: ViewModelContext,
        onClosePrivacySettings: () -> Unit,
    ): PrivacySettingsViewModel {
        return PrivacySettingsViewModelImpl(viewModelContext, onClosePrivacySettings)
    }
}

interface PrivacySettingsViewModel {
    val error: StateFlow<String?>
    val privacySettings: StateFlow<List<PrivacySettingViewModel>>
    fun clearError()
    fun back()
}

open class PrivacySettingsViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onClosePrivacySettings: () -> Unit,
) : ViewModelContext by viewModelContext, PrivacySettingsViewModel {

    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

    override val privacySettings: StateFlow<List<PrivacySettingViewModel>> =
        namedMatrixClients.scopedMapLatest { namedMatrixClients ->
            namedMatrixClients.map { (accountName, matrixClientFlow) ->
                get<PrivacySettingViewModelFactory>()
                    .newPrivacySettingViewModel(
                        viewModelContext = childContext("privacySetting-${accountName}", accountName = accountName),
                        onUnblockError = { error.value = it }
                    )
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    override fun clearError() {
        error.value = null
    }

    override fun back() {
        onClosePrivacySettings()
    }

}
