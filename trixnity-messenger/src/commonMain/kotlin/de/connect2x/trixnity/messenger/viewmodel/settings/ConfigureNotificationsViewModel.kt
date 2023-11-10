package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


interface ConfigureNotificationsViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onCloseConfigureNotifications: () -> Unit,
    ): ConfigureNotificationsViewModel {
        return ConfigureNotificationsViewModelImpl(
            viewModelContext, onCloseConfigureNotifications
        )
    }

    companion object : ConfigureNotificationsViewModelFactory
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
        messengerSettings.notificationsPlaySound(accountName)
    )
    override val showPopup = MutableStateFlow(
        messengerSettings.notificationsShowPopup(accountName)
    )
    override val showText = MutableStateFlow(
        messengerSettings.notificationsShowText(accountName)
    )

    init {
        coroutineScope.launch {
            playSound.collectLatest {
                messengerSettings.setNotificationsPlaySound(accountName, it)
            }
        }
        coroutineScope.launch {
            showPopup.collectLatest {
                messengerSettings.setNotificationsShowPopup(accountName, it)
            }
        }
        coroutineScope.launch {
            showText.collectLatest {
                messengerSettings.setNotificationsShowText(accountName, it)
            }
        }
    }

    override fun back() {
        onCloseConfigureNotifications()
    }

}
