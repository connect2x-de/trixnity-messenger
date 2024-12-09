package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

interface AccountSetupViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onWizardClose: (userId: UserId) -> Unit,
        onStartVerification: (UserId, Boolean) -> Unit,
    ): AccountSetupViewModel {
        return AccountSetupViewModelImpl(
            viewModelContext,
            onWizardClose,
            onStartVerification,
        )
    }

    companion object : AccountSetupViewModelFactory
}

interface AccountSetupViewModel {
    fun closeAccountSetup()
    fun startVerification()
    fun changeVerificationCompleteStatus(newVerificationCompleteStatus: Boolean)
    val completedVerification: MutableStateFlow<Boolean?>
    val userId: UserId
    val privacySettingsViewModel: PrivacySettingsSingleAccountViewModel
    val notificationSettingsViewModel: NotificationSettingsSingleAccountViewModel
}

class AccountSetupViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    val onWizardClose: (UserId) -> Unit,
    val onStartVerification: (UserId, Boolean) -> Unit,
) :
    ViewModelContext by viewModelContext, AccountSetupViewModel {
    override val userId = viewModelContext.userId

    override val completedVerification : MutableStateFlow<Boolean?> = MutableStateFlow(null)

    override val privacySettingsViewModel by lazy {
        get<PrivacySettingsSingleAccountViewModelFactory>().create(viewModelContext) {}
    }
    override val notificationSettingsViewModel by lazy {
        get<NotificationSettingsSingleAccountViewModelFactory>().create(viewModelContext)
    }

    private val startedVerification = MutableStateFlow(false)

    override fun startVerification() {
        if (!startedVerification.value) {
            onStartVerification(userId, true)
            startedVerification.value = true
            completedVerification.value = null
        }
    }

    override fun closeAccountSetup() {
        this.onWizardClose(userId)
    }

    override fun changeVerificationCompleteStatus(newVerificationCompleteStatus: Boolean) {
        completedVerification.value = newVerificationCompleteStatus
        startedVerification.value = false
    }

}
