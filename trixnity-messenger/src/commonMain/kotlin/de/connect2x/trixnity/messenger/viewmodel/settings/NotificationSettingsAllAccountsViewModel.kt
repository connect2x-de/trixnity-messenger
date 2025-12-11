package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.scopedMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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
    val notificationSettings: StateFlow<List<NotificationSettingsSingleAccountViewModel>>
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
        registerBackCallback(backCallback)
    }

    override val notificationSettings: StateFlow<List<NotificationSettingsSingleAccountViewModel>> =
        matrixClients.scopedMapLatest { namedMatrixClients ->
            namedMatrixClients.map { (userId, _) ->
                get<NotificationSettingsSingleAccountViewModelFactory>()
                    .create(
                        viewModelContext = childContext("notificationSettings-${userId}", userId = userId),
                    )
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    override fun back() {
        onBack()
    }
}
