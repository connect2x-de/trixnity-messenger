package de.connect2x.trixnity.messenger.compose.view.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.trixnity.messenger.compose.view.connecting.RegisterNewAccount
import de.connect2x.trixnity.messenger.compose.view.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.viewmodel.connecting.PreviewRegisterMatrixAccountViewModel

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun RegisterNewAccountPreview() {
    InitMessengerPreview {
        val registerNewAccountViewModel = PreviewRegisterMatrixAccountViewModel()
        //        registerNewAccountViewModel.serverUrlValidation.update { ServerUrlValidation.None }
        //        registerNewAccountViewModel.registrationOptions.update { listOf(AuthenticationType.RegistrationToken)
        // }
        //        registerNewAccountViewModel.registrationState.update {
        // RegisterNewAccountViewModel.RegistrationState.Error("Oh no") }
        RegisterNewAccount(registerNewAccountViewModel)
    }
}
