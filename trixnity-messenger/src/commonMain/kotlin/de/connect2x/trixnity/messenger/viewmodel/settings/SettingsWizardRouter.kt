package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.active
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.popWhile
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.launchPush
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.WizardSteps.PrivacySettings
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.WizardSteps.WizardConfirm
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.WizardSteps.WizardExplanation
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.WizardSteps.WizardVerification
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsWizardRouter(
    private val viewModelContext: ViewModelContext,
    val verificationRouter: VerificationRouter,
    val selfVerificationRouter: SelfVerificationRouter
) : ViewModelContext by viewModelContext {

    private val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.None,
        key = "SettingsWizardRouter,",
        childFactory = ::createChild
    )


    private val activeAccount = MutableStateFlow<UserId?>(null)

    private val verificationViewModel = MutableStateFlow<VerificationViewModel?>(null)

    init {
        log.debug { "Initializing Wizard" }
        activeAccount.value = get<MatrixMessengerSettingsHolder>().value.base.selectedAccount
    }


    val selfVerification = MutableStateFlow<SelfVerificationViewModel?>(null)

    fun startVerification() {
        activeAccount.value?.let { selfVerificationRouter.showSelfVerification(it) }
        if (selfVerificationRouter.stack.active.instance is SelfVerificationRouter.Wrapper.View) {
            selfVerification.value =
                (selfVerificationRouter.stack.active.instance as SelfVerificationRouter.Wrapper.View).viewModel
        }
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

    private fun createChild(config: Config, componentContext: ComponentContext): Wrapper = when (config) {
        is Config.None -> {
            Wrapper.None
        }

        is Config.ShowWizard -> {
            Wrapper.ShowWizard(
                listOf<Wrapper>(
                    WizardExplanation(get<MatrixMessengerSettingsHolder>().map { it.base.selectedAccount }
                        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null), ::onWizardClose),
                    PrivacySettings(viewModelContext.get<PrivacySettingsAllAccountsViewModelFactory>().create(
                        viewModelContext.childContext(key = "SettingsWizard-Privacy"),
                        {},
                        {}).privacySettings.transformLatest { value ->
                        val element = value.find { it.account == activeAccount.value }
                        if (element != null) emit(
                            element
                        )
                    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
                    ),
                    WizardSteps.NotificationSettings(viewModelContext.get<NotificationSettingsAllAccountsViewModelFactory>()
                        .create(
                            viewModelContext.childContext(key = "SettingsWizard-Notifications")
                        ) {}.notificationSettings.transformLatest { value ->
                            val element = value.find { it.account == activeAccount.value }
                            if (element != null) emit(
                                element
                            )
                        }.stateIn(
                            coroutineScope, SharingStarted.WhileSubscribed(), null
                        )
                    ),
                    WizardVerification(
                        MutableStateFlow<SelfVerificationViewModel?>(null),
                        MutableStateFlow<VerificationViewModel?>(null),
                        ::startCrossVerification,
                        ::startVerification
                    ),
                    WizardConfirm(::onWizardClose),
                )
            )
        }
    }

    private val settings = get<MatrixMessengerSettingsHolder>()

    val showWizard = activeAccount.value?.let { settings[it].map { it?.base?.showAccountWizard == true } }?.stateIn(
        coroutineScope, SharingStarted.WhileSubscribed(), false
    ) ?: MutableStateFlow(false)

    fun onWizardClose() {
        coroutineScope.launch {
            activeAccount.value?.let {
                settings.update<MatrixMessengerAccountSettingsBase>(it) {
                    it.copy(showAccountWizard = false)
                }
            }
            navigation.popWhile {
                it !is Config.None
            }
        }
    }

    fun possiblyStartWizard() {
        log.debug { "Start Wizard: ${showWizard.value}" }
        if (showWizard.value) {
            log.debug { "Starting Wizard" }
            navigation.launchPush(coroutineScope, Config.ShowWizard)
        }
    }

    sealed class WizardSteps {
        data class NotificationSettings(val viewModel: StateFlow<NotificationSettingsSingleAccountViewModel?>) :
            Wrapper()

        data class PrivacySettings(val viewModel: StateFlow<PrivacySettingsSingleAccountViewModel?>) : Wrapper()
        data class WizardExplanation(val userId: StateFlow<UserId?>, val onWizardClose: () -> Unit) : Wrapper()
        data class WizardVerification(
            val selfVerificationViewModel: StateFlow<SelfVerificationViewModel?>,
            val verificationViewModel: StateFlow<VerificationViewModel?>,
            val startCrossSigning: () -> Unit,
            val startVerification: () -> Unit
        ) : Wrapper()

        data class WizardConfirm(val onWizardClose: () -> Unit) : Wrapper()
    }

    sealed class Wrapper {
        data class ShowWizard(val steps: List<Wrapper>) : Wrapper()
        data object None : Wrapper()
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object ShowWizard : Config()

        @Serializable
        data object None : Config()
    }
}

