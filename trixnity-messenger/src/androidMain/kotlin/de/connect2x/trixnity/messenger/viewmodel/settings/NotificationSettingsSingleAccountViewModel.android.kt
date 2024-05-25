package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.MatrixMessengerAccountPlatformNotificationSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.PushMode
import de.connect2x.trixnity.messenger.notifications
import de.connect2x.trixnity.messenger.settings.updateView
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get
import org.koin.core.module.Module
import org.koin.dsl.module

actual interface NotificationSettingsSingleAccountViewModel {
    actual val account: UserId
    val pushMode: StateFlow<PushMode>

    fun togglePushMode()
}

class NotificationSettingsSingleAccountViewModelImpl(
    override val account: UserId,
    viewModelContext: MatrixClientViewModelContext,
) : ViewModelContext by viewModelContext, NotificationSettingsSingleAccountViewModel {

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val platformNotificationSettings = messengerSettings[account]
        .filterNotNull()
        .map { it.base.platform.notifications }

    override val pushMode = platformNotificationSettings.map { it.pushMode }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), PushMode.POLLING)

    override fun togglePushMode() {
        coroutineScope.launch {
            messengerSettings.updateView<MatrixMessengerAccountPlatformNotificationSettings>(account) {
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
