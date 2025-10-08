package de.connect2x.trixnity.messenger.compose.view.profiles

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.AdaptiveDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.AdaptiveDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.AdaptiveDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.AdaptiveDialogWrapper
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.multi.ProfileManager
import kotlinx.coroutines.launch

// a poor man's way of navigation
val ShowProfileCreation =
    compositionLocalOf<MutableState<Boolean>> { error("There is no ShowProfileCreation defined as compositionLocal") }

interface ProfileSelectionView {
    @Composable
    fun create(profileManager: ProfileManager)
}

@Composable
fun ProfileSelection(profileManager: ProfileManager) {
    DI.get<ProfileSelectionView>().create(profileManager)
}

class ProfileSelectionViewImpl : ProfileSelectionView {
    @Composable
    override fun create(profileManager: ProfileManager) {
        val i18n = DI.get<I18nView>()
        val coroutineScope = rememberCoroutineScope()
        val profiles = profileManager.profiles.collectAsState().value
        val showProfileCreation = ShowProfileCreation.current

        AdaptiveDialogWrapper {
            AdaptiveDialogHeader {
                Text(i18n.selectProfileHeader())
            }
            AdaptiveDialogContent {
                profiles.forEach { (id, settings) ->
                    key(id) {
                        ListItem(
                            headlineContent = { Text(settings.base.displayName ?: i18n.commonUnknown()) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    i18n.login(),
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                coroutineScope.launch {
                                    profileManager.selectProfile(id)
                                }
                            }.buttonPointerModifier()
                        )
                    }
                }
            }
            AdaptiveDialogFooter {
                ThemedButton(
                    style = MaterialTheme.components.primaryButton,
                    onClick = {
                        showProfileCreation.value = true
                    },
                ) {
                    Text(i18n.selectProfileCreateInstead())
                }
            }
        }
    }
}
