package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.getMatrixClient
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
    val accountName: String
    val presenceIsPublic: MutableStateFlow<Boolean>
    val readMarkerIsPublic: MutableStateFlow<Boolean>
    val typingIsPublic: MutableStateFlow<Boolean>
    val blockedUsers: StateFlow<List<UserId>?>
    val unblockingInProgress: MutableStateFlow<List<UserId>>

    fun unblockUser(userId: UserId)
}

open class PrivacySettingViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onUnblockError: (String) -> Unit,
) : PrivacySettingViewModel, MatrixClientViewModelContext by viewModelContext {

    private val messengerSettings = get<MessengerSettings>()
    private val userBlocking = get<UserBlocking>()

    override val presenceIsPublic: MutableStateFlow<Boolean> =
        MutableStateFlow(messengerSettings.presenceIsPublic(accountName))
    override val readMarkerIsPublic: MutableStateFlow<Boolean> =
        MutableStateFlow(messengerSettings.readMarkerIsPublic(accountName))
    override val typingIsPublic: MutableStateFlow<Boolean> =
        MutableStateFlow(messengerSettings.typingIsPublic(accountName))
    override val blockedUsers: StateFlow<List<UserId>?> = (
            matrixClient.user.getAccountData<IgnoredUserListEventContent>()
                .map { ignoredUserListEventContent ->
                    log.debug { "ignoredUserListEventContent (account $accountName): $ignoredUserListEventContent" }
                    ignoredUserListEventContent?.ignoredUsers?.keys?.toList()
                        ?: emptyList()
                }
            )
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)
    override val unblockingInProgress: MutableStateFlow<List<UserId>> = MutableStateFlow(emptyList())

    init {
        // reflect changes back to settings
        coroutineScope.launch {
            presenceIsPublic.drop(1).collect {
                messengerSettings.setPresenceIsPublic(accountName, it)
            }
        }
        coroutineScope.launch {
            readMarkerIsPublic.drop(1).collect {
                messengerSettings.setReadMarkerIsPublic(accountName, it)
            }
        }
        coroutineScope.launch {
            typingIsPublic.drop(1).collect {
                messengerSettings.setTypingIsPublic(accountName, it)
            }
        }
    }

    override fun unblockUser(userId: UserId) {
        coroutineScope.launch {
            unblockingInProgress.value += userId
            try {
                userBlocking.unblockUser(getMatrixClient(accountName), userId) {
                    onUnblockError(i18n.settingsUnblockUserError(userId.full))
                }
            } finally {
                unblockingInProgress.value -= userId
            }
        }
    }
}