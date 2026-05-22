package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import org.koin.core.component.get

interface DeviceSettingsAllAccountsViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCloseDeviceSettings: () -> Unit,
    ): DeviceSettingsAllAccountsViewModel =
        DeviceSettingsAllAccountsViewModelImpl(viewModelContext, onCloseDeviceSettings)

    companion object : DeviceSettingsAllAccountsViewModelFactory
}

interface DeviceSettingsAllAccountsViewModel {
    val deviceSettings: List<DeviceSettingsSingleAccountViewModel>

    fun back()
}

class DeviceSettingsAllAccountsViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCloseDeviceSettings: () -> Unit,
) : ViewModelContext by viewModelContext, DeviceSettingsAllAccountsViewModel {

    private val backCallback = BackCallback { back() }

    init {
        registerBackCallback(backCallback)
    }

    override val deviceSettings: List<DeviceSettingsSingleAccountViewModel> =
        matrixClients.value.map { (userId, _) ->
            get<DeviceSettingsSingleAccountViewModelFactory>()
                .create(viewModelContext = childContext("privacySetting-${userId}", userId = userId))
        }

    override fun back() {
        onCloseDeviceSettings()
    }
}
