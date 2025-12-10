package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import org.koin.core.component.get


interface NotificationSettingsAllAccountsViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onBack: () -> Unit,
    ): NotificationSettingsAllAccountsViewModel = NotificationSettingsAllAccountsViewModelImpl(
        viewModelContext,
        onBack,
    )

    companion object : NotificationSettingsAllAccountsViewModelFactory
}

interface NotificationSettingsAllAccountsViewModel {
    val notificationSettings: List<NotificationSettingsSingleAccountViewModel>
    fun back()
}

class NotificationSettingsAllAccountsViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onBack: () -> Unit,
) : ViewModelContext by viewModelContext, NotificationSettingsAllAccountsViewModel {

    private val backCallback = BackCallback {
        back()
    }

    init {
        backHandler.register(backCallback)
    }

    override val notificationSettings: List<NotificationSettingsSingleAccountViewModel> =
        matrixClients.value.map { (userId, _) ->
            get<NotificationSettingsSingleAccountViewModelFactory>()
                .create(
                    viewModelContext = childContext("notificationSettings-${userId}", userId = userId),
                )
        }

    override fun back() {
        onBack()
    }
}
