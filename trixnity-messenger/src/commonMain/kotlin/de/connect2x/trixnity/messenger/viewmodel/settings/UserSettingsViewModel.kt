package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext

interface UserSettingsViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCloseUserSettings: () -> Unit,
        onShowDeviceSettings: () -> Unit,
        onShowAccounts: () -> Unit,
        onShowProfilesSettings: () -> Unit,
        onShowNotificationsSettings: () -> Unit,
        onShowPrivacySettings: () -> Unit,
        onShowAppearanceSettings: () -> Unit,
    ): UserSettingsViewModel {
        return UserSettingsViewModelImpl(
            viewModelContext,
            onCloseUserSettings,
            onShowDeviceSettings,
            onShowAccounts,
            onShowProfilesSettings,
            onShowNotificationsSettings,
            onShowPrivacySettings,
            onShowAppearanceSettings,
        )
    }

    companion object : UserSettingsViewModelFactory
}

interface UserSettingsViewModel {
    val canShowProfilesSettings: Boolean

    fun closeUserSettings()

    fun showDeviceSettings()

    fun showAccounts()

    fun showProfilesSettings()

    fun showNotificationsSettings()

    fun showPrivacySettings()

    fun showAppearanceSettings()
}

open class UserSettingsViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCloseUserSettings: () -> Unit,
    private val onShowDeviceSettings: () -> Unit,
    private val onShowAccounts: () -> Unit,
    private val onShowProfilesSettings: () -> Unit,
    private val onShowNotificationsSettings: () -> Unit,
    private val onShowPrivacySettings: () -> Unit,
    private val onShowAppearanceSettings: () -> Unit,
) : ViewModelContext by viewModelContext, UserSettingsViewModel {

    override val canShowProfilesSettings = (viewModelContext.getOrNull<ProfileManager>() != null)

    private val backCallback = BackCallback { closeUserSettings() }

    init {
        registerBackCallback(backCallback)
    }

    override fun closeUserSettings() {
        onCloseUserSettings()
    }

    override fun showDeviceSettings() {
        onShowDeviceSettings()
    }

    override fun showAccounts() {
        onShowAccounts()
    }

    override fun showProfilesSettings() {
        if (canShowProfilesSettings) {
            onShowProfilesSettings()
        }
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
