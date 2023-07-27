package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.namedMatrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.scopedMapLatest
import korlibs.io.async.launch
import kotlinx.coroutines.flow.*
import org.koin.core.component.get

data class PrivacySetting(
    val accountName: String,
    val presenceIsPublic: MutableStateFlow<Boolean>,
    val readMarkerIsPublic: MutableStateFlow<Boolean>,
    val typingIsPublic: MutableStateFlow<Boolean>,
)

interface PrivacySettingsViewModelFactory {
    fun newPrivacySettingsViewModel(
        viewModelContext: ViewModelContext,
        onClosePrivacySettings: () -> Unit,
    ): PrivacySettingsViewModel {
        return PrivacySettingsViewModelImpl(viewModelContext, onClosePrivacySettings)
    }
}

interface PrivacySettingsViewModel {
    val privacySettings: StateFlow<List<PrivacySetting>>
    fun back()
}

class PrivacySettingsViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onClosePrivacySettings: () -> Unit,
) : ViewModelContext by viewModelContext, PrivacySettingsViewModel {

    private val messengerSettings = get<MessengerSettings>()

    override val privacySettings: StateFlow<List<PrivacySetting>> =
        namedMatrixClients.scopedMapLatest { namedMatrixClients ->
            namedMatrixClients.map { (accountName, _) ->
                val presenceIsPublic = MutableStateFlow(messengerSettings.presenceIsPublic(accountName))
                val readMarkerIsPublic = MutableStateFlow(messengerSettings.readMarkerIsPublic(accountName))
                val typingIsPublic = MutableStateFlow(messengerSettings.typingIsPublic(accountName))

                // reflect changes back to settings
                this.launch {
                    presenceIsPublic.drop(1).collect {
                        messengerSettings.setPresenceIsPublic(accountName, it)
                    }
                }
                this.launch {
                    readMarkerIsPublic.drop(1).collect {
                        messengerSettings.setReadMarkerIsPublic(accountName, it)
                    }
                }
                this.launch {
                    typingIsPublic.drop(1).collect {
                        messengerSettings.setTypingIsPublic(accountName, it)
                    }
                }

                PrivacySetting(accountName, presenceIsPublic, readMarkerIsPublic, typingIsPublic)
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())


    override fun back() {
        onClosePrivacySettings()
    }

}
