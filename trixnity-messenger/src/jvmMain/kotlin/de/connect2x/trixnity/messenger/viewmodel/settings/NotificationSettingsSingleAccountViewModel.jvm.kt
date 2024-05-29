package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.MatrixMessengerAccountPlatformNotificationSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.platformNotifications
import de.connect2x.trixnity.messenger.updateView
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.get
import org.koin.core.module.Module
import org.koin.dsl.module

actual interface NotificationSettingsSingleAccountViewModel : NotificationSettingsSingleAccountViewModelBase {
    val playSound: StateFlow<Boolean>
    val showPopup: StateFlow<Boolean>
    val showText: StateFlow<Boolean>

    fun togglePlaySound()
    fun toggleShowPopup()
    fun toggleShowText()
}

class NotificationSettingsSingleAccountViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
) : MatrixClientViewModelContext by viewModelContext,
    NotificationSettingsSingleAccountViewModelBase by NotificationSettingsSingleAccountViewModelBaseImpl(
        viewModelContext
    ),
    NotificationSettingsSingleAccountViewModel {

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val platformNotificationSettings = messengerSettings[userId]
        .filterNotNull()
        .map { it.platformNotifications }

    override val playSound = platformNotificationSettings.map { it.notificationsPlaySound }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val showPopup = platformNotificationSettings.map { it.notificationsShowPopup }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val showText = platformNotificationSettings.map { it.notificationsShowText }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override fun togglePlaySound() {
        coroutineScope.launch {
            messengerSettings.updateView<MatrixMessengerAccountPlatformNotificationSettings>(userId) {
                it.copy(notificationsPlaySound = !it.notificationsPlaySound)
            }
        }
    }

    override fun toggleShowPopup() {
        coroutineScope.launch {
            messengerSettings.updateView<MatrixMessengerAccountPlatformNotificationSettings>(userId) {
                it.copy(notificationsShowPopup = !it.notificationsShowPopup)
            }
        }
    }

    override fun toggleShowText() {
        coroutineScope.launch {
            messengerSettings.updateView<MatrixMessengerAccountPlatformNotificationSettings>(userId) {
                it.copy(notificationsShowText = !it.notificationsShowText)
            }
        }
    }
}

actual fun platformNotificationSettingsSingleAccountViewModelFactoryModule(): Module = module {
    single<NotificationSettingsSingleAccountViewModelFactory> {
        NotificationSettingsSingleAccountViewModelFactory(::NotificationSettingsSingleAccountViewModelImpl)
    }
}


