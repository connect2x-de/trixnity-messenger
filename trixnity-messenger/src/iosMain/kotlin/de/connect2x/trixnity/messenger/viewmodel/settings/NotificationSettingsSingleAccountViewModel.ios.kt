package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.time.Duration.Companion.seconds

actual interface NotificationSettingsSingleAccountViewModel : NotificationSettingsSingleAccountViewModelBase {
    val notificationPermissionsNecessary: StateFlow<Boolean>
}

class NotificationSettingsSingleAccountViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
) : MatrixClientViewModelContext by viewModelContext,
    NotificationSettingsSingleAccountViewModelBase by NotificationSettingsSingleAccountViewModelBaseImpl(
        viewModelContext
    ),
    NotificationSettingsSingleAccountViewModel {
    private val notificationPermissionGranted = MutableStateFlow(false)
    override val notificationPermissionsNecessary: StateFlow<Boolean> =
        enabledForThisDevice.combine(notificationPermissionGranted) { settingEnabled, permissionGranted ->
            settingEnabled && !permissionGranted
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    init {
        coroutineScope.launch {
            while (coroutineScope.isActive) {
                UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler {
                    when (requireNotNull(it).authorizationStatus) {
                        UNAuthorizationStatusAuthorized,
                        UNAuthorizationStatusProvisional,
                        UNAuthorizationStatusEphemeral -> notificationPermissionGranted.value = true

                        else -> notificationPermissionGranted.value = false
                    }
                }
                delay(15.seconds)
            }
        }
    }
}

actual fun platformNotificationSettingsSingleAccountViewModelFactoryModule(): Module = module {
    single<NotificationSettingsSingleAccountViewModelFactory> {
        NotificationSettingsSingleAccountViewModelFactory(::NotificationSettingsSingleAccountViewModelImpl)
    }
}
