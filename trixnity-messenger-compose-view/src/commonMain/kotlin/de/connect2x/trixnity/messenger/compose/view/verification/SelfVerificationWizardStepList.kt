package de.connect2x.trixnity.messenger.compose.view.verification

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration

open class SelfVerificationWizardStep(val stepId: String) {
    data object SelfVerificationWizardHelp : SelfVerificationWizardStep("SELF_VERIFICATION_WIZARD_HELP")

    data object SelfVerificationWizardMethods : SelfVerificationWizardStep("SELF_VERIFICATION_WIZARD_METHODS")

    data object SelfVerificationWizardRecoveryKey : SelfVerificationWizardStep("SELF_VERIFICATION_WIZARD_RECOVERY_KEY")

    data object SelfVerificationWizardPassphrase : SelfVerificationWizardStep("SELF_VERIFICATION_WIZARD_PASSPHRASE")

    data object SelfVerificationWizardResetRecoveryKeyConfirmation :
        SelfVerificationWizardStep("SELF_VERIFICATION_WIZARD_RESET_RECOVERY_KEY_CONFIRM")

    data object SelfVerificationWizardVerificationConfirmation :
        SelfVerificationWizardStep("SELF_VERIFICATION_WIZARD_VERIFICATION_CONFIRMATION")

    // --- version 2 of the self verification wizard ---
    data object V2 {
        data object SelfVerificationWizardMethodSelection :
            SelfVerificationWizardStep("SELF_VERIFICATION_WIZARD_V2_METHOD_SELECTION")
    }
}

interface SelfVerificationWizardStepList {
    val steps: List<SelfVerificationWizardStep>
}

class SelfVerificationWizardStepListImpl(features: MatrixMessengerConfiguration.Features) :
    SelfVerificationWizardStepList {
    override val steps: List<SelfVerificationWizardStep> =
        if (features.enableNewAccountWizard) {
            listOf(SelfVerificationWizardStep.V2.SelfVerificationWizardMethodSelection)
        } else {
            listOf(
                SelfVerificationWizardStep.SelfVerificationWizardHelp,
                SelfVerificationWizardStep.SelfVerificationWizardMethods,
                SelfVerificationWizardStep.SelfVerificationWizardRecoveryKey,
                SelfVerificationWizardStep.SelfVerificationWizardPassphrase,
                SelfVerificationWizardStep.SelfVerificationWizardResetRecoveryKeyConfirmation,
                SelfVerificationWizardStep.SelfVerificationWizardVerificationConfirmation,
            )
        }
}
