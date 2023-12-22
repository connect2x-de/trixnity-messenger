package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.PushMode
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

data class NotificationSettingsOfAccount(
    val userId: UserId,
    val displayName:String?,
    val pushMode: PushMode,
    val notificationSettings: String,
    val showNotificationSettings: Boolean,
)

interface NotificationsSettingsViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCloseNotificationsSettings: () -> Unit,
        onShowConfigureNotifications: (userId: UserId) -> Unit,
    ): NotificationsSettingsViewModel {
        return NotificationsSettingsViewModelImpl(
            viewModelContext, onCloseNotificationsSettings, onShowConfigureNotifications
        )
    }

    companion object : NotificationsSettingsViewModelFactory
}

interface NotificationsSettingsViewModel {
    val notificationSettingsOfAccounts: StateFlow<List<NotificationSettingsOfAccount>>
    fun back()
    fun configureNotifications(userId: UserId)
}

open class NotificationsSettingsViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCloseNotificationsSettings: () -> Unit,
    private val onShowConfigureNotifications: (userId: UserId) -> Unit,
) : ViewModelContext by viewModelContext, NotificationsSettingsViewModel {

    private val i18n = get<I18n>()
    override val notificationSettingsOfAccounts: StateFlow<List<NotificationSettingsOfAccount>>

    private val backCallback = BackCallback {
        back()
    }

    init {
        backHandler.register(backCallback)

        notificationSettingsOfAccounts = combine(
            matrixClients,
            get<MatrixMessengerSettingsHolder>()
        ) { namedMatrixClients, settings ->
            namedMatrixClients.map { (userId, _) ->
                log.trace { "notification settings for account $userId will be loaded" }
                val accountSettings = checkNotNull(settings.accounts[userId])
                NotificationSettingsOfAccount(
                    userId = userId,
                    displayName = accountSettings.displayName,
                    pushMode = accountSettings.pushMode,
                    notificationSettings = getNotificationSettings(accountSettings),
                    showNotificationSettings = accountSettings.pushMode != PushMode.NONE
                )
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    private fun getNotificationSettings(accountSettings: MatrixMessengerAccountSettings): String {
        val sound =
            if (accountSettings.notificationsPlaySound) i18n.settingsNotificationsSound()
            else i18n.settingsNotificationsSilent()
        val bubble =
            if (accountSettings.notificationsShowPopup) i18n.settingsNotificationsPopup()
            else i18n.settingsNotificationsPopupNot()
        val text =
            if (accountSettings.notificationsShowText) i18n.settingsNotificationsText()
            else i18n.settingsNotificationsTextNot()

        return listOf(sound, bubble, text).joinToString(", ")
    }

    override fun back() {
        onCloseNotificationsSettings()
    }

    override fun configureNotifications(userId: UserId) {
        onShowConfigureNotifications(userId)
    }
}