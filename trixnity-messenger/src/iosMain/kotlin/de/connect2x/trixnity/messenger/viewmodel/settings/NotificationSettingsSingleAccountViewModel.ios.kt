package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module
import org.koin.dsl.module

actual interface NotificationSettingsSingleAccountViewModel {
    actual val account: UserId
}

class NotificationSettingsSingleAccountViewModelImpl(
    override val account: UserId,
    viewModelContext: MatrixClientViewModelContext,
) : ViewModelContext by viewModelContext, NotificationSettingsSingleAccountViewModel

actual fun platformNotificationSettingsSingleAccountViewModelFactoryModule(): Module = module {
    single<NotificationSettingsSingleAccountViewModelFactory> {
        NotificationSettingsSingleAccountViewModelFactory(::NotificationSettingsSingleAccountViewModelImpl)
    }
}
