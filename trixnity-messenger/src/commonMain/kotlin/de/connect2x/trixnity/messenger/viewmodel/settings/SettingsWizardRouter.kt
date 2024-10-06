package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.decompose.router.stack.active
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.Wrapper.WizardExplanation
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.subscribe
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
        log.debug{"Initializing Wizard"}
        get<MatrixMessengerSettingsHolder>().onEach { activeAccount.value = it.base.selectedAccount }.launchIn(coroutineScope)
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
            selfVerification.value =
                (selfVerificationRouter.stack.active.instance as SelfVerificationRouter.Wrapper.View).viewModel
        }
    }

    private val wizardSteps = listOf<Wrapper>(
        WizardExplanation(activeAccount, ::onWizardClose),
        Wrapper.PrivacySettings(privacySettings),
        Wrapper.NotificationSettings(notificationSettings),
        Wrapper.WizardVerification(
            selfVerification,
            verificationViewModel,
            ::startCrossVerification,
            ::startVerification
        ),
        Wrapper.WizardConfirm(::onWizardClose),
    )

    fun getWizardSteps(): Pair<StateFlow<Boolean>, List<Wrapper>> {
        return showWizard to wizardSteps
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

    private val settings = get<MatrixMessengerSettingsHolder>()

    val showWizard = activeAccount.value?.let { settings[it].map { it?.base?.showAccountWizard == true } }?.stateIn(
        coroutineScope,
        SharingStarted.WhileSubscribed(),
        false
    ) ?: MutableStateFlow(false)

    fun onWizardClose() {
        coroutineScope.launch {
            activeAccount.value?.let {
                settings.update<MatrixMessengerAccountSettingsBase>(it) {
                    it.copy(showAccountWizard = false)
                }
            }
        }
    }


    sealed class Wrapper {
        class NotificationSettings(val viewModel: StateFlow<NotificationSettingsSingleAccountViewModel?>) : Wrapper()
        class PrivacySettings(val viewModel: StateFlow<PrivacySettingsSingleAccountViewModel?>) : Wrapper()
        class WizardExplanation(val userId: StateFlow<UserId?>, val onWizardClose: () -> Unit) : Wrapper()
        class WizardVerification(
            val selfVerificationViewModel: StateFlow<SelfVerificationViewModel?>,
            val verificationViewModel: StateFlow<VerificationViewModel?>,
            val startCrossSigning: () -> Unit,
            val startVerification: () -> Unit
        ) : Wrapper()

        data class WizardConfirm(val onWizardClose: () -> Unit) : Wrapper()
        data object None : Wrapper()
    }
}
