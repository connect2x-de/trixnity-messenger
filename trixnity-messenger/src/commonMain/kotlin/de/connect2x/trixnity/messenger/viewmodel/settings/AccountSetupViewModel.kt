package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

interface AccountSetupViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onWizardClose: (userId: UserId) -> Unit,
        onStartVerification: (UserId, Boolean) -> Unit,
        completedVerification: MutableStateFlow<Boolean?>
    ): AccountSetupViewModel {
        return AccountSetupViewModelImpl(
            viewModelContext,
            onWizardClose,
            onStartVerification,
            completedVerification
        )
    }

    companion object : AccountSetupViewModelFactory
}

interface AccountSetupViewModel {
    fun closeAccountSetup()
    fun startVerification()
    val completedVerification: MutableStateFlow<Boolean?>
    val userId: UserId
    val privacySettingsViewModel: PrivacySettingsSingleAccountViewModel
    val notificationSettingsViewModel: NotificationSettingsSingleAccountViewModel
}

class AccountSetupViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    val onWizardClose: (UserId) -> Unit,
    val onStartVerification: (UserId, Boolean) -> Unit,
    override val completedVerification: MutableStateFlow<Boolean?>
) :
    ViewModelContext by viewModelContext, AccountSetupViewModel {
    override val userId = viewModelContext.userId

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
        }
    }

    override fun closeAccountSetup() {
        this.onWizardClose(userId)
    }

    init {
        coroutineScope.launch {
            completedVerification.collectLatest {
                if (it != null) startedVerification.value = false
            }
        }
    }

}
