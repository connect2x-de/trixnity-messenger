package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext


interface UserSettingsViewModelFactory {
    fun newUserSettingsViewModel(
        viewModelContext: ViewModelContext,
        onCloseUserSettings: () -> Unit,
        onShowDevicesSettings: () -> Unit,
        onShowProfile: () -> Unit,
        onShowNotificationsSettings: () -> Unit,
    ): UserSettingsViewModel {
        return UserSettingsViewModelImpl(
            viewModelContext, onCloseUserSettings, onShowDevicesSettings, onShowProfile, onShowNotificationsSettings
        )
    }
}

interface UserSettingsViewModel {
    fun closeUserSettings()
    fun showDevicesSettings()
    fun showProfile()
    fun showNotificationsSettings()
}

open class UserSettingsViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCloseUserSettings: () -> Unit,
    private val onShowDevicesSettings: () -> Unit,
    private val onShowProfile: () -> Unit,
    private val onShowNotificationsSettings: () -> Unit,
) : ViewModelContext by viewModelContext, UserSettingsViewModel {

    private val backCallback = BackCallback {
        closeUserSettings()
    }

    init {
        backHandler.register(backCallback)
    }

    override fun closeUserSettings() {
        onCloseUserSettings()
    }

    override fun showDevicesSettings() {
        onShowDevicesSettings()
    }

    override fun showProfile() {
        onShowProfile()
    }

    override fun showNotificationsSettings() {
        onShowNotificationsSettings()
    }
}