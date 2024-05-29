package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.MatrixMessengerAccountPlatformNotificationSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.PushMode
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
    val pushMode: StateFlow<PushMode>

    fun togglePushMode()
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

    override val pushMode = platformNotificationSettings.map { it.pushMode }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), PushMode.POLLING)

    override fun togglePushMode() {
        coroutineScope.launch {
            messengerSettings.updateView<MatrixMessengerAccountPlatformNotificationSettings>(userId) {
                it.copy(
                    pushMode = when (it.pushMode) {
                        PushMode.PUSH -> PushMode.POLLING
                        PushMode.POLLING -> PushMode.PUSH
                    }
                )
            }
        }
    }
}

actual fun platformNotificationSettingsSingleAccountViewModelFactoryModule(): Module = module {
    single<NotificationSettingsSingleAccountViewModelFactory> {
        NotificationSettingsSingleAccountViewModelFactory(::NotificationSettingsSingleAccountViewModelImpl)
    }
}
