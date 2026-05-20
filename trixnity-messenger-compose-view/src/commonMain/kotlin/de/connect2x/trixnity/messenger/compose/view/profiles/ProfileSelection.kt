package de.connect2x.trixnity.messenger.compose.view.profiles

import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusItem
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.AdaptiveDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.AdaptiveDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.AdaptiveDialogScrollContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.AdaptiveDialogWrapper
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedHorizontalDivider
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemButton
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
        val profiles = profileManager.profiles.collectAsState()
        val multiAccount = profileManager.isMultiProfileEnabled.collectAsState().value.let { it != null && it }
        val showProfileCreation = ShowProfileCreation.current
        val scrollState = rememberScrollState()

        var focusedItem by remember(profiles.value) { mutableStateOf(profiles.value.keys.firstOrNull()) }

        AdaptiveDialogWrapper {
            AdaptiveDialogHeader {
                Text(i18n.selectProfileHeader())
            }

            AdaptiveDialogScrollContent(
                scrollState = scrollState,
                modifier = Modifier.rovingFocusContainer()
            ) {
                profiles.value.entries.forEach { entry ->
                    ThemedListItemButton(
                        style = MaterialTheme.components.settingsItem,
                        modifier = Modifier.rovingFocusItem(
                            isFocused = focusedItem == entry.key,
                            onFocus = { focusedItem = entry.key }
                        ),
                        leadingContent = {
                            Icon(Icons.Default.AccountCircle, null)
                        },
                        headlineContent = {
                            Text(entry.value.base.displayName ?: i18n.commonUnknown())
                        },
                        onClick = {
                            coroutineScope.launch {
                                profileManager.selectProfile(entry.key)
                            }
                        }
                    )
                    ThemedHorizontalDivider()
                }
            }
            if (multiAccount) AdaptiveDialogFooter {
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
