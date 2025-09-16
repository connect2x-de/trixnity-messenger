package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.MiddleSpacer
import de.connect2x.messenger.compose.view.common.Wizard
import de.connect2x.messenger.compose.view.common.WizardNavigationButton.Custom
import de.connect2x.messenger.compose.view.common.WizardStep
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupRouter.Wrapper
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupViewModel


/**
 * Represents a step in the Account Setup Wizard, utilizing a unique identifier for each step.
 * This class serves as a base for defining various specific steps within the wizard process.
 * Inherit from this class to create or replace steps.
 *
 * @property stepId Unique identifier for the specific step.
 */
open class AccountSetupWizardStep(val stepId: String) {
    data object ExplanationStep : AccountSetupWizardStep("ACCOUNT_SETUP_WIZARD_EXPLANATION")
    data object AccessibilityStep : AccountSetupWizardStep("ACCOUNT_SETUP_WIZARD_ACCESSIBILITY")
    data object VerificationStep : AccountSetupWizardStep("ACCOUNT_SETUP_WIZARD_VERIFICATION")
    data object PrivacySettingsStep : AccountSetupWizardStep("ACCOUNT_SETUP_WIZARD_PRIVACY")
    data object NotificationSettingsStep : AccountSetupWizardStep("ACCOUNT_SETUP_WIZARD_NOTIFICATION")
    data object ConfirmationStep : AccountSetupWizardStep("ACCOUNT_SETUP_WIZARD_CONFIRM")
}

/**
 * Represents a list of steps in the account setup wizard process.
 * To change certain or add new steps, replace `AccountSetupWizardStepListImpl`
 */
interface AccountSetupWizardStepList {
    val steps: List<AccountSetupWizardStep>
}

class AccountSetupWizardStepListImpl : AccountSetupWizardStepList {
    override val steps = listOf(
        AccountSetupWizardStep.ExplanationStep,
        AccountSetupWizardStep.AccessibilityStep,
        AccountSetupWizardStep.PrivacySettingsStep,
        AccountSetupWizardStep.NotificationSettingsStep,
        AccountSetupWizardStep.VerificationStep,
        AccountSetupWizardStep.ConfirmationStep
    )
}


/**
 * This interface allows for the creation of custom steps within the wizard by providing
 * its own specific logic and user interface components as part of the wizard flow.
 *
 * This is built upon `AccountSetupWizardStep` and is used
 * to extend the default functionality of the account setup process.
 *
 * If you wish to add multiple steps, they should still be handled by
 * resolving the appropriate `AccountSetupWizardStep`
 *
 * @see AccountSetupWizardStep
 * @see WizardStep
 */
interface AdditionalAccountSetupWizardStep {
    fun <T : Any> create(viewModel: T, step: AccountSetupWizardStep): WizardStep
}

class AdditionalAccountSetupWizardStepImpl() : AdditionalAccountSetupWizardStep {
    override fun <T : Any> create(
        viewModel: T,
        step: AccountSetupWizardStep
    ): WizardStep {
        throw IllegalArgumentException("Creating an AccountSetupWizard step with ${step::class} is unsupported and requires an implementation")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSetupWizard(showAccountBootstrapWrapper: Wrapper.ShowAccountSetup) {
    val i18n = DI.get<I18nView>()

    val viewModel = showAccountBootstrapWrapper.viewModel
    val additionalAccountSetupWizardStep = DI.get<AdditionalAccountSetupWizardStep>()
    val setupSteps = DI.get<AccountSetupWizardStepList>().steps
    val wizardSteps = remember(viewModel, setupSteps, i18n) {
        setupSteps.map {
            when (it) {
                is AccountSetupWizardStep.ExplanationStep ->
                    wizardStepExplanation(viewModel, it, i18n)

                is AccountSetupWizardStep.AccessibilityStep ->
                    wizardStepAccessibility(viewModel, it, i18n)

                is AccountSetupWizardStep.NotificationSettingsStep ->
                    wizardStepNotification(viewModel, it, i18n)

                is AccountSetupWizardStep.ConfirmationStep ->
                    wizardStepConfirmation(viewModel, it, i18n)

                is AccountSetupWizardStep.PrivacySettingsStep ->
                    wizardStepPrivacy(viewModel, it, i18n)

                is AccountSetupWizardStep.VerificationStep ->
                    wizardStepVerification(viewModel, it, i18n)

                else ->
                    additionalAccountSetupWizardStep.create(viewModel, it)
            }
        }
    }
    Wizard(wizardSteps, viewModel.backHandler)
}

private fun wizardStepExplanation(
    viewModel: AccountSetupViewModel,
    step: AccountSetupWizardStep,
    i18n: I18nView
): WizardStep {
    return WizardStep(
        id = step.stepId,
        title = { "${i18n.commonWelcome()} ${viewModel.userId.localpart}" },
        content = { Text(i18n.accountSetupWizardExplanationMessage()) },
    )
}

private fun wizardStepAccessibility(
    viewModel: AccountSetupViewModel,
    step: AccountSetupWizardStep,
    i18n: I18nView
): WizardStep {
    return WizardStep(
        id = step.stepId,
        title = { i18n.commonAccessibility() },
        content = {
            val isHighContrast by viewModel.appearanceSettingsViewModel.isHighContrast.collectAsState()
            val isFocusHighlighting by viewModel.appearanceSettingsViewModel.isFocusHighlighting.collectAsState()
            Column {
                AppearanceSettingsSize(viewModel.appearanceSettingsViewModel)
                Spacer(Modifier.height(15.dp))
                Setting(
                    text = i18n.appearanceHighContrastHeading(),
                    explanation = i18n.appearanceHighContrastExplanation(),
                    value = isHighContrast
                ) {
                    viewModel.appearanceSettingsViewModel.toggleHighContrast()
                }
                Setting(
                    text = i18n.appearanceFocusHighlightingHeading(),
                    explanation = i18n.appearanceFocusHighlightingExplanation(),
                    value = isFocusHighlighting,
                ) {
                    viewModel.appearanceSettingsViewModel.toggleFocusHighlighting()
                }
            }
        }
    )
}

private fun wizardStepNotification(
    viewModel: AccountSetupViewModel,
    step: AccountSetupWizardStep,
    i18n: I18nView
): WizardStep {
    val notificationSettingsViewModel = viewModel.notificationSettingsViewModel
    return WizardStep(
        id = step.stepId,
        title = { i18n.commonNotifications() },
        content = {
            val enabledOnDevice = notificationSettingsViewModel.enabledForThisDevice.collectAsState().value
            Column {
                Setting(
                    text = i18n.notificationsSettingsEnabledForThisDevice(),
                    value = enabledOnDevice,
                    toggle = { notificationSettingsViewModel.toggleEnabledForThisDevice() })
                MiddleSpacer()
                PlatformNotificationSettings(notificationSettingsViewModel, enabledOnDevice)
                MiddleSpacer()
                PlatformNotificationAccountSettings(notificationSettingsViewModel, enabledOnDevice)
            }
        },
    )
}

private fun wizardStepConfirmation(
    viewModel: AccountSetupViewModel,
    step: AccountSetupWizardStep,
    i18n: I18nView
): WizardStep {
    return WizardStep(id = step.stepId, title = { i18n.accountSetupWizardFinishSetupTitle() }, content = {
        Text(i18n.accountSetupWizardFinishSetup())
    }, nextButton = {
        Custom {
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                onClick = { viewModel.closeAccountSetup() },
            ) {
                Text(i18n.commonConfirm())
            }
        }
    })
}

private fun wizardStepPrivacy(
    viewModel: AccountSetupViewModel,
    step: AccountSetupWizardStep,
    i18n: I18nView
): WizardStep {
    val privacySettingsViewModel = viewModel.privacySettingsViewModel
    return WizardStep(id = step.stepId, title = { i18n.privacyTitle() }, content = {
        val di = DI.current
        val publicPresence = privacySettingsViewModel.presenceIsPublic.collectAsState().value
        val publicTyping = privacySettingsViewModel.typingIsPublic.collectAsState().value
        val publicRead = privacySettingsViewModel.readMarkerIsPublic.collectAsState().value
        Column {
            Setting(
                text = i18n.privacyPresenceIsPublic(),
                explanation = i18n.privacyPresenceIsPublicExplanation(di.get<MatrixMessengerConfiguration>().appName),
                value = publicPresence,
                toggle = { privacySettingsViewModel.togglePresenceIsPublic() })
            Setting(
                text = i18n.privacyReadMarkerIsPublic(),
                explanation = i18n.privacyReadMarkerIsPublicExplanation(),
                value = publicRead,
                toggle = { privacySettingsViewModel.toggleReadMarkerIsPublic() })
            Setting(
                text = i18n.privacyTypingIsPublic(),
                explanation = i18n.privacyTypingIsPublicExplanation(),
                value = publicTyping,
                toggle = { privacySettingsViewModel.toggleTypingIsPublic() })
        }
    })
}

private fun wizardStepVerification(
    viewModel: AccountSetupViewModel,
    step: AccountSetupWizardStep,
    i18n: I18nView
): WizardStep {
    val completedVerification = viewModel.completedVerification
    return WizardStep(
        id = step.stepId,
        title = { i18n.deviceVerification() },
        content = {
            viewModel.startVerification()
        },
        nextButton = {
            Custom {
                val completedVerification = completedVerification.collectAsState().value
                if (completedVerification == true) {
                    viewModel.completedVerification.value = null
                    nextStep?.let { currentStepId.value = it }
                }
            }
        },
        backButton = {
            Custom {
                val completedVerification = completedVerification.collectAsState().value
                if (completedVerification == false) {
                    viewModel.completedVerification.value = null
                    previousStep?.let { currentStepId.value = it }
                }
            }
        }
    )
}
