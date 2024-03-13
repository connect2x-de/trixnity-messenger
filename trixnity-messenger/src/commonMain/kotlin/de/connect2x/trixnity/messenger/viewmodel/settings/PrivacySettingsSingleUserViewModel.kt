package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
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


interface PrivacySettingsSingleUserViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onShowBlockedContactsSettings: (userId: UserId) -> Unit,
    ): PrivacySettingsSingleUserViewModel =
        PrivacySettingsSingleUserViewModelImpl(
            viewModelContext,
            onShowBlockedContactsSettings,
        )

    companion object : PrivacySettingsSingleUserViewModelFactory
}

interface PrivacySettingsSingleUserViewModel {
    val userId: UserId

    val presenceIsPublic: StateFlow<Boolean>
    val readMarkerIsPublic: StateFlow<Boolean>
    val typingIsPublic: StateFlow<Boolean>

    fun togglePresenceIsPublic()
    fun toggleReadMarkerIsPublic()
    fun toggleTypingIsPublic()

    fun showBlockedContactsSettings()
    val blockedContactsCount: StateFlow<Int>
}

open class PrivacySettingsSingleUserViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onShowBlockedContactsSettings: (userId: UserId) -> Unit,
) : PrivacySettingsSingleUserViewModel, MatrixClientViewModelContext by viewModelContext {

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val userBlocking = get<UserBlocking>()

    override val presenceIsPublic = messengerSettings[userId]
        .filterNotNull().map { it.presenceIsPublic }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override val readMarkerIsPublic = messengerSettings[userId]
        .filterNotNull().map { it.readMarkerIsPublic }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override val typingIsPublic = messengerSettings[userId]
        .filterNotNull().map { it.typingIsPublic }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override fun togglePresenceIsPublic() {
        coroutineScope.launch {
            messengerSettings.update(userId) {
                it?.copy(presenceIsPublic = !it.presenceIsPublic)
            }
        }
    }

    override fun toggleReadMarkerIsPublic() {
        coroutineScope.launch {
            messengerSettings.update(userId) {
                it?.copy(readMarkerIsPublic = !it.readMarkerIsPublic)
            }
        }
    }

    override fun toggleTypingIsPublic() {
        coroutineScope.launch {
            messengerSettings.update(userId) {
                it?.copy(typingIsPublic = !it.typingIsPublic)
            }
        }
    }

    override fun showBlockedContactsSettings() {
        onShowBlockedContactsSettings(this.userId)
    }

    override val blockedContactsCount: StateFlow<Int> =
        userBlocking.getBlockedUsers(viewModelContext.matrixClient).map { it.size }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), 0)
}
