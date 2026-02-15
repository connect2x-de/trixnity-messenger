package de.connect2x.trixnity.messenger.compose.view.profiles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.multi.ProfileCreationViewModel

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
        var profileName by profileCreationViewModel.profileName.collectAsTextFieldValueState()
        val error = profileCreationViewModel.error.collectAsState().value
        val canCreateProfile = profileCreationViewModel.canCreateProfile.collectAsState().value

        ThemedModalDialog(onFinish) {
            ModalDialogHeader {
                Text(i18n.createProfileHeader())
            }
            ModalDialogContent {
                Column {
                    Text(i18n.createProfileSelectName(), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.size(10.dp))
                    OutlinedTextField(
                        profileName,
                        { profileName = it },
                        modifier = Modifier.fillMaxWidth(),
                        isError = error != null,
                        maxLines = 1,
                    )
                    Spacer(Modifier.size(5.dp))
                    error?.let {
                        Spacer(Modifier.size(5.dp))
                        Text(color = MaterialTheme.colorScheme.error, text = it)
                    }
                }
            }
            ModalDialogFooter {
                ThemedButton(
                    style = MaterialTheme.components.commonButton,
                    onClick = { onFinish() },
                ) {
                    Text(i18n.actionCancel())
                }
                ThemedButton(
                    style = MaterialTheme.components.primaryButton,
                    onClick = {
                        profileCreationViewModel.createProfile()
                        onFinish()
                    },
                    enabled = canCreateProfile,
                ) {
                    Text(i18n.createProfileAction())
                }
            }
        }
    }
}
