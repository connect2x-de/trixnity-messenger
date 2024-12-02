package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.isVerified
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.key
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

interface AccountSetupViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onWizardClose: (userId: UserId) -> Unit,
        onStartCrossSigningBootstrap: (userId: UserId) -> Unit,
        onCloseCrossDeviceVerification: () -> Unit,
        onStartVerification: (UserId, Boolean) -> Unit,
        completedVerification: MutableStateFlow<Boolean?>
    ): AccountSetupViewModel {
        return AccountSetupViewModelImpl(
            viewModelContext,
            onWizardClose,
            onStartCrossSigningBootstrap,
            onCloseCrossDeviceVerification,
            onStartVerification,
            completedVerification
        )
    }

    companion object : AccountSetupViewModelFactory
}

interface AccountSetupViewModel {
    fun closeAccountSetup()
    fun closeCrossDeviceVerification()
    fun startVerification()
    val completedVerification: MutableStateFlow<Boolean?>
    val userId: UserId
    val privacySettingsViewModel: PrivacySettingsSingleAccountViewModel
    val notificationSettingsViewModel: NotificationSettingsSingleAccountViewModel
    val isVerified: StateFlow<Boolean?>
}

class AccountSetupViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    val onWizardClose: (UserId) -> Unit,
    val onStartCrossSigningBootstrap: (UserId) -> Unit,
    val onCloseCrossDeviceVerification: () -> Unit,
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

    private fun startCrossSigningBootstrap() {
        log.debug { "Start cross signing bootstrap from AccountBootstrapping" }
        onStartCrossSigningBootstrap(userId)
    }

    override fun closeCrossDeviceVerification() {
        log.debug { "Close device Verification from AccountBootstrapping" }
        onCloseCrossDeviceVerification()
    }

    private val startedVerification = MutableStateFlow(false)
    override fun startVerification() {
        if (!startedVerification.value) {
            onStartVerification(userId, true)
            startedVerification.value = true
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isVerified =
        matrixClients.map { it[userId] }.filterNotNull()
            .map { it.key.getTrustLevel(userId, it.deviceId).map { it.isVerified } }.flatMapLatest({ it })
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)


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
