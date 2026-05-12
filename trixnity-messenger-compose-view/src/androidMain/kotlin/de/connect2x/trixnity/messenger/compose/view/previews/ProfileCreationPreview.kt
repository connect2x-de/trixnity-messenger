package de.connect2x.trixnity.messenger.compose.view.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.trixnity.messenger.compose.view.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.compose.view.profiles.ProfileCreation
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun PasswordLoginPreview() {
    InitMessengerPreview {
        ProfileCreation(
            textFieldViewModel = TextFieldViewModelImpl(1_000, "my profile"),
            error = null,
            onFinish = {},
            onCreate = {},
        )
    }
}
