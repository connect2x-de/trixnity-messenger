package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.scopedMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.get

interface PrivacySettingsViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onClosePrivacySettings: () -> Unit,
    ): PrivacySettingsViewModel {
        return PrivacySettingsViewModelImpl(viewModelContext, onClosePrivacySettings)
    }

    companion object : PrivacySettingsViewModelFactory
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
        matrixClients.scopedMapLatest { namedMatrixClients ->
            namedMatrixClients.map { (userId, _) ->
                get<PrivacySettingViewModelFactory>()
                    .create(
                        viewModelContext = childContext("privacySetting-${userId}", userId = userId),
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
