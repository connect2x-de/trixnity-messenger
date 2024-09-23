package de.connect2x.messenger.compose.view.profiles

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.MessengerModal
import de.connect2x.messenger.compose.view.common.MessengerModalButtonRow
import de.connect2x.messenger.compose.view.common.MessengerModalContent
import de.connect2x.messenger.compose.view.common.collectAsStateForTextField
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.multi.ProfileCreationViewModel
import de.connect2x.trixnity.messenger.util.CloseApp

interface ProfileCreationView {
    @Composable
    fun create(profileCreationViewModel: ProfileCreationViewModel, onFinish: () -> Unit)
}

@Composable
fun ProfileCreation(profileCreationViewModel: ProfileCreationViewModel, onFinish: () -> Unit) {
    DI.get<ProfileCreationView>().create(profileCreationViewModel, onFinish)
}

class ProfileCreationViewImpl : ProfileCreationView {
    @Composable
    override fun create(profileCreationViewModel: ProfileCreationViewModel, onFinish: () -> Unit) {
        val i18n = DI.get<I18nView>()
        val profileName = profileCreationViewModel.profileName.collectAsStateForTextField().value
        val error = profileCreationViewModel.error.collectAsState().value
        val canCreateProfile = profileCreationViewModel.canCreateProfile.collectAsState().value

        MessengerModal(
            onDismiss = onFinish,
            title = i18n.createProfileHeader()
        ) {
            MessengerModalContent {
                Text(i18n.createProfileSelectName(), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(10.dp))
                OutlinedTextField(
                    profileName,
                    { profileCreationViewModel.profileName.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null,
                )
                Spacer(Modifier.size(5.dp))
                error?.let {
                    Spacer(Modifier.size(5.dp))
                    Text(color = MaterialTheme.colorScheme.error, text = it)
                }
            }
            MessengerModalButtonRow(
                button1 = {
                    OutlinedButton(
                        onClick = { onFinish() },
                        modifier = Modifier.buttonPointerModifier()
                    ) {
                        Text(i18n.commonCancel().capitalize(Locale.current))
                    }
                },
                button2 = {
                    Button(
                        onClick = {
                            profileCreationViewModel.createProfile()
                            onFinish()
                        },
                        enabled = canCreateProfile,
                        modifier = Modifier.buttonPointerModifier(),
                    ) {
                        Text(i18n.createProfileAction())
                    }
                }
            )
        }
    }
}
