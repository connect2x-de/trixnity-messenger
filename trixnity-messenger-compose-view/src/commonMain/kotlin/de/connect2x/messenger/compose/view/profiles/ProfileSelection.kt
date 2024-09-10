package de.connect2x.messenger.compose.view.profiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.MessengerModal
import de.connect2x.messenger.compose.view.common.MessengerModalButtonRow
import de.connect2x.messenger.compose.view.common.MessengerModalContent
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.multi.ProfileManager
import kotlinx.coroutines.launch

// a poor man's way of navigation
val ShowProfileCreation =
    compositionLocalOf<MutableState<Boolean>> { error("There is no ShowProfileCreation defined as compositionLocal") }

// FIXME
interface ProfileSelectionView {
    @Composable
    fun create(profileManager: ProfileManager, onCancel: () -> Unit)
}

@Composable
fun ProfileSelection(profileManager: ProfileManager, onCancel: () -> Unit) {
    DI.current.get<ProfileSelectionView>().create(profileManager, onCancel)
}

class ProfileSelectionViewImpl : ProfileSelectionView {
    @Composable
    override fun create(profileManager: ProfileManager, onCancel: () -> Unit) {
        val i18n = DI.current.get<I18nView>()
        val coroutineScope = rememberCoroutineScope()
        val profiles = profileManager.profiles.collectAsState().value
        val showProfileCreation = ShowProfileCreation.current

        MessengerModal(
            onDismiss = onCancel,
            title = i18n.selectProfileHeader(),
        ) {
            MessengerModalContent {
                Spacer(Modifier.size(10.dp))
                profiles.forEach { (id, settings) ->
                    ListItem(
                        headlineContent = { Text(settings.base.displayName ?: i18n.commonUnknown()) },
                        leadingContent = {
                            Icon(
                                Icons.Default.AccountCircle,
                                "Login",
                                Modifier.fillMaxHeight(),
                            )
                        },
                        modifier = Modifier.clickable {
                            coroutineScope.launch {
                                profileManager.selectProfile(id)
                            }
                        }.buttonPointerModifier()
                    )

                }
            }
            MessengerModalButtonRow(
                button1 = {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.buttonPointerModifier()
                    ) {
                        Text(i18n.commonCancel().capitalize(Locale.current))
                    }
                },
                button2 = {
                    Button(
                        onClick = {
                            showProfileCreation.value = true
                        },
                        modifier = Modifier.buttonPointerModifier(),
                    ) {
                        Text(i18n.selectProfileCreateInstead())
                    }
                }
            )
        }
    }
}
