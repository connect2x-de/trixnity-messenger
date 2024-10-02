package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.decompose.router.stack.active
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.Wrapper.WizardExplanation
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsWizardRouter(
    private val viewModelContext: ViewModelContext,
    val verificationRouter: VerificationRouter,
    val selfVerificationRouter: SelfVerificationRouter
) : ViewModelContext by viewModelContext {

    private val activeAccount = MutableStateFlow<UserId?>(null)
    private val verificationViewModel = MutableStateFlow<VerificationViewModel?>(null)

    init {
        val messengerSettings = get<MatrixMessengerSettingsHolder>()
        activeAccount.value = messengerSettings.value.base.selectedAccount
    }

    private val privacySettings =
        viewModelContext.get<PrivacySettingsAllAccountsViewModelFactory>()
            .create(
                viewModelContext.childContext(key = "SettingsWizard-Privacy"),
                {},
                {}).privacySettings.transformLatest { value ->
                val element = value.find { it.account == activeAccount.value }
                if (element != null) emit(
                    element
                )
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private val notificationSettings = viewModelContext.get<NotificationSettingsAllAccountsViewModelFactory>()
        .create(
            viewModelContext.childContext(key = "SettingsWizard-Notifications")
        ) {}.notificationSettings.transformLatest { value ->
            val element = value.find { it.account == activeAccount.value }
            if (element != null) emit(
                element
            )
        }.stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(), null
        )

    val selfVerification = MutableStateFlow<SelfVerificationViewModel?>(null)

    fun startVerification() {
        activeAccount.value?.let { selfVerificationRouter.showSelfVerification(it) }
        if (selfVerificationRouter.stack.active.instance is SelfVerificationRouter.Wrapper.View) {
            selfVerification.value = (selfVerificationRouter.stack.active.instance as SelfVerificationRouter.Wrapper.View).viewModel
        }
    }

    private val wizardSteps = listOf<Wrapper>(
        WizardExplanation(activeAccount.value ?: UserId("Unknown")),
        Wrapper.PrivacySettings(privacySettings),
        Wrapper.NotificationSettings(notificationSettings),
        Wrapper.WizardVerification(selfVerification, verificationViewModel, ::startCrossVerification, ::startVerification),
        Wrapper.WizardConfirm,
    )

    fun getWizardSteps(): List<Wrapper> {
        return wizardSteps
    }

    fun startCrossVerification() {
        coroutineScope.launch {
            activeAccount.value?.let { verificationRouter.startDeviceVerification(it) }
            val active = verificationRouter.stack.value.active
            if (active.instance is VerificationRouter.Wrapper.Verification) {
                verificationViewModel.value = (active.instance as VerificationRouter.Wrapper.Verification).viewModel
            }
        }
    }


sealed class Wrapper {
    class NotificationSettings(val viewModel: StateFlow<NotificationSettingsSingleAccountViewModel?>) : Wrapper()
    class PrivacySettings(val viewModel: StateFlow<PrivacySettingsSingleAccountViewModel?>) : Wrapper()
    class WizardExplanation(val userId: UserId) : Wrapper()
    class WizardVerification(
        val selfVerificationViewModel: StateFlow<SelfVerificationViewModel?>,
        val verificationViewModel: StateFlow<VerificationViewModel?>,
        val startCrossSigning: () -> Unit,
        val startVerification: () -> Unit
    ) : Wrapper()

    data object WizardConfirm : Wrapper()
    data object None : Wrapper()
}
}
