package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import net.folivo.trixnity.core.model.UserId


interface UserSettingsViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCloseUserSettings: () -> Unit,
        onShowDevicesSettings: () -> Unit,
        onShowProfile: () -> Unit,
        onShowNotificationsSettings: () -> Unit,
        onShowPrivacySettings: () -> Unit,
        onShowAppearanceSettings: () -> Unit,
        onShowAccountSetup: (userId: UserId) -> Unit,
    ): UserSettingsViewModel {
        return UserSettingsViewModelImpl(
            viewModelContext,
            onCloseUserSettings,
            onShowDevicesSettings,
            onShowProfile,
            onShowNotificationsSettings,
            onShowPrivacySettings,
            onShowAppearanceSettings,
            onShowAccountSetup,
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
    fun showAccountSetup(userId: UserId)
}

open class UserSettingsViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCloseUserSettings: () -> Unit,
    private val onShowDevicesSettings: () -> Unit,
    private val onShowProfile: () -> Unit,
    private val onShowNotificationsSettings: () -> Unit,
    private val onShowPrivacySettings: () -> Unit,
    private val onShowAppearanceSettings: () -> Unit,
    private val onShowAccountSetup: (userId : UserId) -> Unit,
) : ViewModelContext by viewModelContext, UserSettingsViewModel {

    private val backCallback = BackCallback {
        closeUserSettings()
    }

    init {
        registerBackCallback(backCallback)
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

    override fun showAccountSetup(userId: UserId) {
        onShowAccountSetup(userId)
    }
}
