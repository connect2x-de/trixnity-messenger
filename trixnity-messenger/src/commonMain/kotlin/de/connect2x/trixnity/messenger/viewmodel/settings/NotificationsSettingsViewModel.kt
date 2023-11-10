package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.subscribe
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.namedMatrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.scopedMapLatest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.Koin

private val log = KotlinLogging.logger { }

data class NotificationSettingsOfAccount(
    val accountName: String,
    val pushMode: MutableStateFlow<PushMode>,
    val notificationSettings: MutableStateFlow<String>,
    val showNotificationSettings: StateFlow<Boolean>,
)

interface NotificationsSettingsViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCloseNotificationsSettings: () -> Unit,
        onShowConfigureNotifications: (accountName: String) -> Unit,
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
    fun configureNotifications(accountName: String)
    fun reloadNotificationSettings()
}

open class NotificationsSettingsViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCloseNotificationsSettings: () -> Unit,
    private val onShowConfigureNotifications: (accountName: String) -> Unit,
) : ViewModelContext by viewModelContext, NotificationsSettingsViewModel {

    private val messengerSettings = getKoin().get<MessengerSettings>()
    override val notificationSettingsOfAccounts: StateFlow<List<NotificationSettingsOfAccount>>

    private val backCallback = BackCallback {
        back()
    }

    init {
        backHandler.register(backCallback)

        notificationSettingsOfAccounts = namedMatrixClients.scopedMapLatest { namedMatrixClients ->
            namedMatrixClients.map { (accountName, _) ->
                log.trace { "notification settings for account $accountName will be loaded" }
                val pushMode = MutableStateFlow(
                    messengerSettings.pushMode(accountName)
                )
                NotificationSettingsOfAccount(
                    accountName = accountName,
                    pushMode = pushMode,
                    notificationSettings = MutableStateFlow(getNotificationSettings(accountName, getKoin())),
                    showNotificationSettings = messengerSettings.pushModeFlow(accountName).map { it != PushMode.NONE }
                        .stateIn(
                            this,
                            SharingStarted.WhileSubscribed(),
                            messengerSettings.pushMode(accountName) != PushMode.NONE,
                        )
                ).also {
                    this.launch {
                        pushMode.collectLatest {
                            messengerSettings.setPushMode(accountName, it)
                        }
                    }
                }
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

        lifecycle.subscribe(onResume = {
            notificationSettingsOfAccounts.value.forEach { notificationSettingsOfAccount ->
                notificationSettingsOfAccount.notificationSettings.value =
                    getNotificationSettings(notificationSettingsOfAccount.accountName, getKoin())
            }
        })
    }

    override fun back() {
        onCloseNotificationsSettings()
    }

    override fun configureNotifications(accountName: String) {
        doConfigureNotifications(accountName) {
            onShowConfigureNotifications(accountName)
        }
    }

    override fun reloadNotificationSettings() {
        log.debug { "reload notification settings for all accounts" }
        notificationSettingsOfAccounts.value.forEach { notificationSettingsOfAccount ->
            notificationSettingsOfAccount.notificationSettings.value =
                getNotificationSettings(notificationSettingsOfAccount.accountName, getKoin())
        }
    }
}

expect fun doConfigureNotifications(accountName: String, onShowConfigureNotifications: () -> Unit)
expect fun getNotificationSettings(accountName: String, koin: Koin): String