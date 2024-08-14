package de.connect2x.trixnity.messenger.viewmodel.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import de.connect2x.trixnity.messenger.MatrixMessengerAccountPlatformNotificationSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.PushMode
import de.connect2x.trixnity.messenger.platformNotifications
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.get
import org.koin.core.module.Module
import org.koin.dsl.module

actual interface NotificationSettingsSingleAccountViewModel : NotificationSettingsSingleAccountViewModelBase {
    val pushMode: StateFlow<PushMode>
    val notificationPermissionsNecessary: StateFlow<Boolean>

    fun setPushMode(pushMode: PushMode)
}

class NotificationSettingsSingleAccountViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
) : MatrixClientViewModelContext by viewModelContext,
    NotificationSettingsSingleAccountViewModelBase by NotificationSettingsSingleAccountViewModelBaseImpl(
        viewModelContext
    ),
    NotificationSettingsSingleAccountViewModel {
    val context = get<Context>()
    private val _notificationPermissionGranted = MutableStateFlow(false)

    init {
        coroutineScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                while (isActive) {
                    _notificationPermissionGranted.value =
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
    }

    override val notificationPermissionsNecessary: StateFlow<Boolean> =
        enabledForThisDevice.combine(_notificationPermissionGranted) { settingEnabled, permissionGranted -> settingEnabled && !permissionGranted }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val platformNotificationSettings = messengerSettings[userId]
        .filterNotNull()
        .map { it.platformNotifications }

    override val pushMode = platformNotificationSettings.map { it.pushMode }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), PushMode.POLLING)

    override fun setPushMode(pushMode: PushMode) {
        coroutineScope.launch {
            messengerSettings.update<MatrixMessengerAccountPlatformNotificationSettings>(userId) {
                it.copy(pushMode = pushMode)
            }
        }
    }
}

actual fun platformNotificationSettingsSingleAccountViewModelFactoryModule(): Module = module {
    single<NotificationSettingsSingleAccountViewModelFactory> {
        NotificationSettingsSingleAccountViewModelFactory(::NotificationSettingsSingleAccountViewModelImpl)
    }
}
