package de.connect2x.messenger.compose.view.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.messenger.compose.view.connecting.PasswordLogin
import de.connect2x.messenger.compose.view.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.viewmodel.connecting.PreviewPasswordLoginViewModel

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun PasswordLoginPreview() {
    InitMessengerPreview {
        PasswordLogin(PreviewPasswordLoginViewModel())
    }
}
