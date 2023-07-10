package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


interface ConfigureNotificationsViewModelFactory {
    fun newConfigureNotificationsViewModel(
        viewModelContext: MatrixClientViewModelContext,
        onCloseConfigureNotifications: () -> Unit,
    ): ConfigureNotificationsViewModel {
        return ConfigureNotificationsViewModelImpl(
            viewModelContext, onCloseConfigureNotifications
        )
    }
}

interface ConfigureNotificationsViewModel {
    val playSound: MutableStateFlow<Boolean>
    val showPopup: MutableStateFlow<Boolean>
    val showText: MutableStateFlow<Boolean>
    fun back()
}

open class ConfigureNotificationsViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onCloseConfigureNotifications: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, ConfigureNotificationsViewModel {

    private val messengerSettings = getKoin().get<MessengerSettings>()

    override val playSound = MutableStateFlow(
        messengerSettings.notificationPlaySound[accountName] ?: messengerSettings.defaultNotificationPlaySound
    )
    override val showPopup = MutableStateFlow(
        messengerSettings.notificationsShowPopup[accountName] ?: messengerSettings.defaultNotificationShowPopup
    )
    override val showText = MutableStateFlow(
        messengerSettings.notificationsShowText[accountName] ?: messengerSettings.defaultNotificationShowText
    )

    init {
        coroutineScope.launch {
            playSound.collectLatest {
                messengerSettings.notificationPlaySound =
                    messengerSettings.notificationPlaySound - accountName + (accountName to it)
            }
        }
        coroutineScope.launch {
            showPopup.collectLatest {
                messengerSettings.notificationsShowPopup =
                    messengerSettings.notificationsShowPopup - accountName + (accountName to it)
            }
        }
        coroutineScope.launch {
            showText.collectLatest {
                messengerSettings.notificationsShowText =
                    messengerSettings.notificationsShowText - accountName + (accountName to it)
            }
        }
    }

    override fun back() {
        onCloseConfigureNotifications()
    }

}
