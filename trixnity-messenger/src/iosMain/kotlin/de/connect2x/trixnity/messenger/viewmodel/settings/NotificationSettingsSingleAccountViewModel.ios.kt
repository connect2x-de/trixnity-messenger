package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual interface NotificationSettingsSingleAccountViewModel : NotificationSettingsSingleAccountViewModelBase

class NotificationSettingsSingleAccountViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
) : MatrixClientViewModelContext by viewModelContext, NotificationSettingsSingleAccountViewModel

actual fun platformNotificationSettingsSingleAccountViewModelFactoryModule(): Module = module {
    single<NotificationSettingsSingleAccountViewModelFactory> {
        NotificationSettingsSingleAccountViewModelFactory(::NotificationSettingsSingleAccountViewModelImpl)
    }
}
