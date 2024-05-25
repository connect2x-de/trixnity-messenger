package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module


fun interface NotificationSettingsSingleAccountViewModelFactory {
    fun create(
        account: UserId,
        viewModelContext: MatrixClientViewModelContext,
    ): NotificationSettingsSingleAccountViewModel
}

expect interface NotificationSettingsSingleAccountViewModel {
    val account: UserId

    // FIXME more fields
}

// FIXME abstract class NotificationSettingsSingleAccountViewModelBase

expect fun platformNotificationSettingsSingleAccountViewModelFactoryModule(): Module
