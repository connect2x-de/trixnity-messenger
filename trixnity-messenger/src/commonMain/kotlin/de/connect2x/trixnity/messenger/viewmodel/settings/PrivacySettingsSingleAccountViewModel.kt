package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUia
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaResult
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlocking
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

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

    val deactiveAccountLoading: StateFlow<Boolean>
    val deactiveAccountError: StateFlow<String?>
    fun deactiveAccount(erase: Boolean = false)
}

open class PrivacySettingsSingleAccountViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onShowBlockedContactsSettings: (account: UserId) -> Unit,
) : PrivacySettingsSingleAccountViewModel, MatrixClientViewModelContext by viewModelContext {

    private val i18n = get<I18n>()
    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val userBlocking = get<UserBlocking>()
    private val authorizeUia = get<AuthorizeUia>()
    private val deactivateAccountScope = get<CoroutineScope>()

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
            messengerSettings.update<MatrixMessengerAccountSettingsBase>(account) {
                it.copy(presenceIsPublic = !it.presenceIsPublic)
            }
        }
    }

    override fun toggleReadMarkerIsPublic() {
        coroutineScope.launch {
            messengerSettings.update<MatrixMessengerAccountSettingsBase>(account) {
                it.copy(readMarkerIsPublic = !it.readMarkerIsPublic)
            }
        }
    }

    override fun toggleTypingIsPublic() {
        coroutineScope.launch {
            messengerSettings.update<MatrixMessengerAccountSettingsBase>(account) {
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

    override val deactiveAccountLoading = MutableStateFlow(false)
    override val deactiveAccountError = MutableStateFlow<String?>(null)
    override fun deactiveAccount(erase: Boolean) {
        log.info { "trying to deactivate account" }
        if (deactiveAccountLoading.compareAndSet(expect = false, update = true)) {
            deactivateAccountScope.launch {
                val result = authorizeUia(
                    i18n.deactivateAccountConfirmationMessage(matrixClient.userId.full)
                ) {
                    matrixClient.api.authentication.deactivateAccount(erase = erase)
                }
                when (result) {
                    is AuthorizeUiaResult.CancelledByUser ->
                        deactiveAccountError.value = result.message

                    is AuthorizeUiaResult.Error ->
                        deactiveAccountError.value = i18n.deactivateAccountError(result.exception.errorResponse.error)

                    is AuthorizeUiaResult.UnexpectedError ->
                        deactiveAccountError.value = result.message

                    is AuthorizeUiaResult.Success -> {
                        log.info { "successfully deactivated account" }
                        matrixClients.logout(userId)
                            .onSuccess { log.debug { "logout completed" } }
                            .onFailure { log.info { "logout failed" } }
                        matrixClients.remove(userId)
                            .onSuccess { log.debug { "removed account $userId" } }
                            .onFailure {
                                log.error(it) { "cannot remove account $userId" }
                                deactiveAccountError.value = i18n.logoutFailure()
                            }
                    }
                }
                deactiveAccountLoading.value = false
            }
        }
    }
}
