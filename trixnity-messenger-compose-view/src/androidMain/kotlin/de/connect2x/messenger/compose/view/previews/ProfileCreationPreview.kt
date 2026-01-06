package de.connect2x.messenger.compose.view.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.messenger.compose.view.previews.util.InitMessengerPreview
import de.connect2x.messenger.compose.view.profiles.ProfileCreation
import de.connect2x.trixnity.messenger.multi.ProfileCreationViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun PasswordLoginPreview() {
    InitMessengerPreview {
        ProfileCreation(
            object : ProfileCreationViewModel {
                override val canCreateProfile: StateFlow<Boolean> = MutableStateFlow(true)
                override val error: StateFlow<String?> = MutableStateFlow(null)
                override val profileName = TextFieldViewModelImpl(1_000, "my profile")
                override fun createProfile() {
                }
            },
            onFinish = {}
        )
    }
}
