package de.connect2x.messenger.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.messenger.compose.view.connecting.RegisterNewAccount
import de.connect2x.messenger.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.viewmodel.connecting.PreviewRegisterNewAccountViewModel


@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun RegisterNewAccountPreview() {
    InitMessengerPreview {
        val registerNewAccountViewModel = PreviewRegisterNewAccountViewModel()
//        registerNewAccountViewModel.serverUrlValidation.update { ServerUrlValidation.None }
//        registerNewAccountViewModel.registrationOptions.update { listOf(AuthenticationType.RegistrationToken) }
//        registerNewAccountViewModel.registrationState.update { RegisterNewAccountViewModel.RegistrationState.Error("Oh no") }
        RegisterNewAccount(registerNewAccountViewModel)
    }
}
