package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.get


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
    val playSound: StateFlow<Boolean>
    val showPopup: StateFlow<Boolean>
    val showText: StateFlow<Boolean>

    fun togglePlaySound()
    fun toggleShowPopup()
    fun toggleShowText()
    fun back()
}

open class ConfigureNotificationsViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onCloseConfigureNotifications: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, ConfigureNotificationsViewModel {

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()

    override val playSound = messengerSettings[userId].filterNotNull().map { it.notificationsPlaySound }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val showPopup = messengerSettings[userId].filterNotNull().map { it.notificationsShowPopup }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val showText = messengerSettings[userId].filterNotNull().map { it.notificationsShowText }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override fun togglePlaySound() {
        coroutineScope.launch {
            messengerSettings.update(userId) { it?.copy(notificationsPlaySound = !it.notificationsPlaySound) }
        }
    }

    override fun toggleShowPopup() {
        coroutineScope.launch {
            messengerSettings.update(userId) { it?.copy(notificationsShowPopup = !it.notificationsShowPopup) }
        }
    }

    override fun toggleShowText() {
        coroutineScope.launch {
            messengerSettings.update(userId) { it?.copy(notificationsShowText = !it.notificationsShowText) }
        }
    }

    override fun back() {
        onCloseConfigureNotifications()
    }

}
