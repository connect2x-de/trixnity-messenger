package de.connect2x.trixnity.messenger.compose.view.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.trixnity.messenger.compose.view.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.compose.view.uia.UiaActionConfirmation
import de.connect2x.trixnity.messenger.compose.view.uia.UiaDummyStep
import de.connect2x.trixnity.messenger.compose.view.uia.UiaFallbackFlow
import de.connect2x.trixnity.messenger.compose.view.uia.UiaPasswordInput
import de.connect2x.trixnity.messenger.compose.view.uia.UiaRegistrationToken
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaActionConfirmationViewModelPreview
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaActionConfirmationViewModelPreview.PreviewMode.ERROR
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepDummyViewModelPreview
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepFallbackViewModelPreview
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepFallbackViewModelPreview.PreviewMode.AWAITING
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepFallbackViewModelPreview.PreviewMode.ERROR as FALLBACK_ERROR
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepPasswordViewModelPreview
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepPasswordViewModelPreview.PreviewMode.ERROR as PASSWORD_ERROR
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepPasswordViewModelPreview.PreviewMode.FILLED
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepPasswordViewModelPreview.PreviewMode.SUBMITTING
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepRegistrationTokenViewModelPreview

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun UiaUserConfirmationPreview() {
    InitMessengerPreview { UiaActionConfirmation(UiaActionConfirmationViewModelPreview()) }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun UiaUserConfirmationErrorPreview() {
    InitMessengerPreview { UiaActionConfirmation(UiaActionConfirmationViewModelPreview(ERROR)) }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun RegistrationTokenPreview() {
    InitMessengerPreview { UiaRegistrationToken(UiaStepRegistrationTokenViewModelPreview()) }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun PasswordInputPreview() {
    InitMessengerPreview { UiaPasswordInput(UiaStepPasswordViewModelPreview()) }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun PasswordInputPreviewFilledOut() {
    InitMessengerPreview { UiaPasswordInput(UiaStepPasswordViewModelPreview(FILLED)) }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun PasswordInputPreviewError() {
    InitMessengerPreview { UiaPasswordInput(UiaStepPasswordViewModelPreview(PASSWORD_ERROR)) }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun PasswordInputPreviewSubmitting() {
    InitMessengerPreview { UiaPasswordInput(UiaStepPasswordViewModelPreview(SUBMITTING)) }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun FallbackFlowPreview() {
    InitMessengerPreview { UiaFallbackFlow(UiaStepFallbackViewModelPreview()) }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun FallbackFlowPreviewAwaiting() {
    InitMessengerPreview { UiaFallbackFlow(UiaStepFallbackViewModelPreview(AWAITING)) }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun FallbackFlowPreviewError() {
    InitMessengerPreview { UiaFallbackFlow(UiaStepFallbackViewModelPreview(FALLBACK_ERROR)) }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun DummyFlowPreview() {
    InitMessengerPreview { UiaDummyStep(UiaStepDummyViewModelPreview()) }
}
