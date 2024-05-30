package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext


interface UserSettingsViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCloseUserSettings: () -> Unit,
        onShowDevicesSettings: () -> Unit,
        onShowProfile: () -> Unit,
        onShowNotificationsSettings: () -> Unit,
        onShowPrivacySettings: () -> Unit,
        onShowAppearanceSettings: () -> Unit,
    ): UserSettingsViewModel {
        return UserSettingsViewModelImpl(
            viewModelContext,
            onCloseUserSettings,
            onShowDevicesSettings,
            onShowProfile,
            onShowNotificationsSettings,
            onShowPrivacySettings,
            onShowAppearanceSettings
        )
    }

    companion object : UserSettingsViewModelFactory
}

interface UserSettingsViewModel {
    fun closeUserSettings()
    fun showDevicesSettings()
    fun showProfile()
    fun showNotificationsSettings()
    fun showPrivacySettings()
    fun showAppearanceSettings()
}

open class UserSettingsViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCloseUserSettings: () -> Unit,
    private val onShowDevicesSettings: () -> Unit,
    private val onShowProfile: () -> Unit,
    private val onShowNotificationsSettings: () -> Unit,
    private val onShowPrivacySettings: () -> Unit,
    private val onShowAppearanceSettings: () -> Unit,
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

    override fun showPrivacySettings() {
        onShowPrivacySettings()
    }

    override fun showAppearanceSettings() {
        onShowAppearanceSettings()
    }
}
