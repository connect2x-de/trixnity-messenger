package de.connect2x.trixnity.messenger.compose.view.settings

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
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.common.Wizard
import de.connect2x.trixnity.messenger.compose.view.common.WizardNavigationButton.Custom
import de.connect2x.trixnity.messenger.compose.view.common.WizardStep
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemSwitch
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

class AdditionalAccountSetupWizardStepImpl : AdditionalAccountSetupWizardStep {
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
    Wizard(wizardSteps, true)
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
                ThemedListItemSwitch(
                    style = MaterialTheme.components.settingsItem,
                    headlineContent = { Text(i18n.appearanceHighContrastHeading()) },
                    supportingContent = { Text(i18n.appearanceHighContrastExplanation()) },
                    selected = isHighContrast,
                    onChange = { viewModel.appearanceSettingsViewModel.toggleHighContrast() },
                )
                ThemedListItemSwitch(
                    style = MaterialTheme.components.settingsItem,
                    headlineContent = { Text(i18n.appearanceFocusHighlightingHeading()) },
                    supportingContent = { Text(i18n.appearanceFocusHighlightingExplanation()) },
                    selected = isFocusHighlighting,
                    onChange = { viewModel.appearanceSettingsViewModel.toggleFocusHighlighting() },
                )
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
            Column {
                DeviceNotificationSettings(notificationSettingsViewModel)
                SmallSpacer()
                AccountNotificationSettings(notificationSettingsViewModel)
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
        val redactWarningEnabled = privacySettingsViewModel.redactionWarningIsEnabled.collectAsState().value
        Column {
            ThemedListItemSwitch(
                style = MaterialTheme.components.settingsItem,
                headlineContent = { Text(i18n.privacyPresenceIsPublic()) },
                supportingContent = { Text(i18n.privacyPresenceIsPublicExplanation(di.get<MatrixMessengerConfiguration>().appName)) },
                selected = publicPresence,
                onChange = { privacySettingsViewModel.togglePresenceIsPublic() },
            )
            ThemedListItemSwitch(
                style = MaterialTheme.components.settingsItem,
                headlineContent = { Text(i18n.privacyReadMarkerIsPublic()) },
                supportingContent = { Text(i18n.privacyReadMarkerIsPublicExplanation()) },
                selected = publicRead,
                onChange = { privacySettingsViewModel.toggleReadMarkerIsPublic() },
            )
            ThemedListItemSwitch(
                style = MaterialTheme.components.settingsItem,
                headlineContent = { Text(i18n.privacyTypingIsPublic()) },
                supportingContent = { Text(i18n.privacyTypingIsPublicExplanation()) },
                selected = publicTyping,
                onChange = { privacySettingsViewModel.toggleTypingIsPublic() },
            )
            ThemedListItemSwitch(
                style = MaterialTheme.components.settingsItem,
                headlineContent = { Text(i18n.redactionWarningSettingTitle()) },
                supportingContent = { Text(i18n.redactionWarningSettingDescription()) },
                selected = redactWarningEnabled,
                onChange = { privacySettingsViewModel.toggleRedactionWarningIsEnabled() },
            )
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
