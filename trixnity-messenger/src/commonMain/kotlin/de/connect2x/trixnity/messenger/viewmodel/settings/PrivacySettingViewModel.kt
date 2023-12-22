package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlocking
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.IgnoredUserListEventContent
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

interface PrivacySettingViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onUnblockError: (String) -> Unit,
    ): PrivacySettingViewModel =
        PrivacySettingViewModelImpl(
            viewModelContext,
            onUnblockError,
        )

    companion object : PrivacySettingViewModelFactory
}

interface PrivacySettingViewModel {
    val userId: UserId

    val presenceIsPublic: StateFlow<Boolean>
    val readMarkerIsPublic: StateFlow<Boolean>
    val typingIsPublic: StateFlow<Boolean>

    fun togglePresenceIsPublic()
    fun toggleReadMarkerIsPublic()
    fun toggleTypingIsPublic()

    val blockedUsers: StateFlow<List<UserId>?>
    val unblockingInProgress: MutableStateFlow<List<UserId>>

    fun unblockUser(userId: UserId)
}

open class PrivacySettingViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onUnblockError: (String) -> Unit,
) : PrivacySettingViewModel, MatrixClientViewModelContext by viewModelContext {

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val userBlocking = get<UserBlocking>()

    override val presenceIsPublic = messengerSettings[userId].filterNotNull().map { it.presenceIsPublic }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val readMarkerIsPublic = messengerSettings[userId].filterNotNull().map { it.readMarkerIsPublic }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val typingIsPublic = messengerSettings[userId].filterNotNull().map { it.typingIsPublic }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override fun togglePresenceIsPublic() {
        coroutineScope.launch {
            messengerSettings.update(userId) { it?.copy(presenceIsPublic = !it.presenceIsPublic) }
        }
    }

    override fun toggleReadMarkerIsPublic() {
        coroutineScope.launch {
            messengerSettings.update(userId) { it?.copy(readMarkerIsPublic = !it.readMarkerIsPublic) }
        }
    }

    override fun toggleTypingIsPublic() {
        coroutineScope.launch {
            messengerSettings.update(userId) { it?.copy(typingIsPublic = !it.typingIsPublic) }
        }
    }

    override val blockedUsers: StateFlow<List<UserId>?> = (
            matrixClient.user.getAccountData<IgnoredUserListEventContent>()
                .map { ignoredUserListEventContent ->
                    log.debug { "ignoredUserListEventContent (account $userId): $ignoredUserListEventContent" }
                    ignoredUserListEventContent?.ignoredUsers?.keys?.toList()
                        ?: emptyList()
                }
            )
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)
    override val unblockingInProgress: MutableStateFlow<List<UserId>> = MutableStateFlow(emptyList())

    override fun unblockUser(userId: UserId) {
        coroutineScope.launch {
            unblockingInProgress.value += userId
            try {
                userBlocking.unblockUser(matrixClient, userId) {
                    onUnblockError(i18n.settingsUnblockUserError(userId.full))
                }
            } finally {
                unblockingInProgress.value -= userId
            }
        }
    }
}