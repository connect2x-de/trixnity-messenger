package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.Wrapper.WizardExplanation
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModelFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsWizardRouter(
    private val viewModelContext: ViewModelContext,
    private val selfVerificationRouter: SelfVerificationRouter
) : ViewModelContext by viewModelContext {

    private val activeAccount = MutableStateFlow<UserId?>(null)

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

    val selfVerification = get<SelfVerificationViewModelFactory>().create(
        viewModelContext.childContext(
            key = "SettingsWizard-Verification",
            userId = activeAccount.value ?: UserId("Unknown")
        ), {})

    val verification = get<VerificationViewModelFactory>().create(
        viewModelContext.childContext(
            key = "SettingsWizard-Bootstrap",
            userId = activeAccount.value ?: UserId("Unknown")
        ), {}, {}, null, null
    )

    private val wizardSteps = listOf<Wrapper>(
        WizardExplanation(activeAccount.value ?: UserId("Unknown")),
        Wrapper.PrivacySettings(privacySettings),
        Wrapper.NotificationSettings(notificationSettings),
        Wrapper.WizardVerification(selfVerification, verification),
        Wrapper.WizardConfirm,
    )

    fun getWizardSteps(): List<Wrapper> {
        return wizardSteps
    }


    sealed class Wrapper {
        class NotificationSettings(val viewModel: StateFlow<NotificationSettingsSingleAccountViewModel?>) : Wrapper()
        class PrivacySettings(val viewModel: StateFlow<PrivacySettingsSingleAccountViewModel?>) : Wrapper()
        class WizardExplanation(val userId: UserId) : Wrapper()
        class WizardVerification(
            val selfVerificationViewModel: SelfVerificationViewModel,
            val verificationViewModel: VerificationViewModel
        ) : Wrapper()

        data object WizardConfirm : Wrapper()
        data object None : Wrapper()
    }
}
