package de.connect2x.trixnity.messenger.compose.view.verification.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.wizard.Wizard
import de.connect2x.trixnity.messenger.compose.view.common.wizard.WizardStep
import de.connect2x.trixnity.messenger.compose.view.form.LocalHiddenRegistrationForm
import de.connect2x.trixnity.messenger.compose.view.form.rememberHiddenRegistrationForm
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.verification.SelfVerificationWizardStep
import de.connect2x.trixnity.messenger.compose.view.verification.SelfVerificationWizardStepList
import de.connect2x.trixnity.messenger.viewmodel.verification.v2.SelfVerificationViewModel

interface SelfVerificationWizardView {
    @Composable fun create(selfVerificationViewModel: SelfVerificationViewModel)
}

@Composable
fun SelfVerificationWizard(selfVerificationViewModel: SelfVerificationViewModel) {
    DI.get<SelfVerificationWizardView>().create(selfVerificationViewModel)
}

class SelfVerificationWizardViewImpl : SelfVerificationWizardView {
    @Composable
    override fun create(selfVerificationViewModel: SelfVerificationViewModel) {
        val i18n = DI.get<I18nView>()
        val steps = DI.get<SelfVerificationWizardStepList>().steps

        val hiddenRegistrationForm = rememberHiddenRegistrationForm()
        val wizardSteps =
            remember(selfVerificationViewModel, i18n) {
                steps.mapNotNull {
                    when (it) {
                        is SelfVerificationWizardStep.V2.SelfVerificationWizardMethodSelection ->
                            selfVerificationWizardMethodSelectionStep(
                                selfVerificationViewModel,
                                SelfVerificationWizardStep.V2.SelfVerificationWizardMethodSelection,
                                i18n,
                            )

                        else -> null
                    }
                }
            }

        CompositionLocalProvider(LocalHiddenRegistrationForm provides hiddenRegistrationForm) {
            Wizard(wizardSteps = wizardSteps, wizardId = "SelfVerificationWizard")
        }
    }
}

private fun selfVerificationWizardMethodSelectionStep(
    viewModel: SelfVerificationViewModel,
    step: SelfVerificationWizardStep,
    i18n: I18nView,
): WizardStep {
    return WizardStep(
        id = step.stepId,
        title = { i18n.deviceVerification() },
        subTitle = { "${i18n.commonAccount()}: ${viewModel.userId.full}" },
        content = { SelfVerificationSteps(viewModel) },
    )
}
