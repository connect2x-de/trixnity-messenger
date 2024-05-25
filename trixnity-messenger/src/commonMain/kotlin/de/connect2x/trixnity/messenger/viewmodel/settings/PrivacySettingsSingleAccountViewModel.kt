package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.settings.updateView
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlocking
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get


interface PrivacySettingsSingleAccountViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onShowBlockedContactsSettings: (account: UserId) -> Unit,
    ): PrivacySettingsSingleAccountViewModel =
        PrivacySettingsSingleAccountViewModelImpl(
            viewModelContext,
            onShowBlockedContactsSettings,
        )

    companion object : PrivacySettingsSingleAccountViewModelFactory
}

interface PrivacySettingsSingleAccountViewModel {
    val account: UserId

    val presenceIsPublic: StateFlow<Boolean>
    val readMarkerIsPublic: StateFlow<Boolean>
    val typingIsPublic: StateFlow<Boolean>

    fun togglePresenceIsPublic()
    fun toggleReadMarkerIsPublic()
    fun toggleTypingIsPublic()

    val blockedContactsCount: StateFlow<Int>
    fun showBlockedContactsSettings()
}

open class PrivacySettingsSingleAccountViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onShowBlockedContactsSettings: (account: UserId) -> Unit,
) : PrivacySettingsSingleAccountViewModel, MatrixClientViewModelContext by viewModelContext {

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val userBlocking = get<UserBlocking>()

    final override val account = userId

    override val presenceIsPublic = messengerSettings[account]
        .filterNotNull().map { it.base.presenceIsPublic }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override val readMarkerIsPublic = messengerSettings[account]
        .filterNotNull().map { it.base.readMarkerIsPublic }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override val typingIsPublic = messengerSettings[account]
        .filterNotNull().map { it.base.typingIsPublic }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override fun togglePresenceIsPublic() {
        coroutineScope.launch {
            messengerSettings.updateView<MatrixMessengerAccountSettingsBase>(account) {
                it.copy(presenceIsPublic = !it.presenceIsPublic)
            }
        }
    }

    override fun toggleReadMarkerIsPublic() {
        coroutineScope.launch {
            messengerSettings.updateView<MatrixMessengerAccountSettingsBase>(account) {
                it.copy(readMarkerIsPublic = !it.readMarkerIsPublic)
            }
        }
    }

    override fun toggleTypingIsPublic() {
        coroutineScope.launch {
            messengerSettings.updateView<MatrixMessengerAccountSettingsBase>(account) {
                it.copy(typingIsPublic = !it.typingIsPublic)
            }
        }
    }

    override val blockedContactsCount: StateFlow<Int> =
        userBlocking.getBlockedUsers(viewModelContext.matrixClient).map { it.size }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), 0)

    override fun showBlockedContactsSettings() {
        onShowBlockedContactsSettings(this.account)
    }
}
