package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.icons.EditIcon
import de.connect2x.trixnity.messenger.compose.view.common.icons.HelpIcon
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSelectableText
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSingleViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountsViewModel
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

interface AccountSingleSettingsView {
    @Composable
    fun create(
        accountSingleViewModel: AccountSingleViewModel,
        accountsViewModel:
            AccountsViewModel, // rather cumbersome, but without a message bus the viewmodels need to communicate in the
        // extensions, too
    )
}

@Composable
fun AccountSingleSettings(accountSingleViewModel: AccountSingleViewModel, accountsViewModel: AccountsViewModel) {
    DI.get<AccountSingleSettingsView>().create(accountSingleViewModel, accountsViewModel)
}

class AccountSingleSettingsViewImpl : AccountSingleSettingsView {
    @Composable
    override fun create(accountSingleViewModel: AccountSingleViewModel, accountsViewModel: AccountsViewModel) {
        val i18n = DI.get<I18nView>()
        SettingsAccountCard(accountSingleViewModel.userId) {
            AccountAvatar(accountSingleViewModel)
            Spacer(Modifier.size(10.dp))
            AccountDisplayName(accountSingleViewModel)
            Spacer(Modifier.size(10.dp))
            AccountUserId(accountSingleViewModel)
            Spacer(Modifier.size(10.dp))
            FlowRow {
                ThemedButton(
                    onClick = { accountSingleViewModel.resetSetup() },
                    content = {
                        Icon(Icons.Default.SettingsSuggest, null)
                        Spacer(Modifier.size(MaterialTheme.components.destructiveButton.iconSpacing))
                        Text(i18n.accountSetupWizardReset().capitalize(Locale.current))
                    },
                )
                Spacer(Modifier.size(10.dp))
                ThemedButton(
                    onClick = { accountSingleViewModel.logout() },
                    style = MaterialTheme.components.destructiveButton,
                    content = {
                        Icon(Icons.AutoMirrored.Filled.Logout, null)
                        Spacer(Modifier.size(MaterialTheme.components.destructiveButton.iconSpacing))
                        Text(i18n.accountsOverviewLogout())
                    },
                )
            }
        }
    }
}

@Composable
fun AccountAvatar(accountSingleViewModel: AccountSingleViewModel) {
    val i18n = DI.get<I18nView>()
    val avatar = accountSingleViewModel.avatar.collectAsState().value
    val canChangeAvatar = accountSingleViewModel.canChangeAvatar.collectAsState().value
    val canDeleteAvatar = accountSingleViewModel.canDeleteAvatar.collectAsState().value
    val hasAvatarUrl = accountSingleViewModel.hasAvatarUrl.collectAsState().value
    val initials = accountSingleViewModel.initials.collectAsState().value

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        Box(Modifier.align(Alignment.Center)) {
            ThemedUserAvatar(
                initials = initials,
                image = avatar,
                size = this@BoxWithConstraints.maxWidth.coerceAtMost(200.dp),
            ) {
                Box(Modifier.align(Alignment.BottomEnd).padding(10.dp)) {
                    Tooltip({ Text(i18n.profileAvatarChange()) }) {
                        ThemedIconButton(
                            enabled = canChangeAvatar,
                            style = MaterialTheme.components.secondaryIconButton,
                            onClick = { accountSingleViewModel.openAvatarCutter.value = true },
                        ) {
                            Icon(Icons.Default.PhotoCamera, i18n.profileAvatarChange())
                        }
                    }
                }
                if (hasAvatarUrl) {
                    Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                        Tooltip({
                            if (canDeleteAvatar) Text(i18n.profileAvatarDelete())
                            else Text(i18n.profileAvatarDeleteNoPermission())
                        }) {
                            ThemedIconButton(
                                enabled = canDeleteAvatar,
                                style = MaterialTheme.components.secondaryIconButton,
                                onClick = { accountSingleViewModel.deleteAvatar() },
                            ) {
                                Icon(Icons.Default.Delete, i18n.profileAvatarDelete())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountDisplayName(accountSingleViewModel: AccountSingleViewModel) {
    val i18n = DI.get<I18nView>()
    val displayName = accountSingleViewModel.displayName.collectAsState().value
    var editDisplayName by (accountSingleViewModel.editDisplayName as TextFieldViewModel).collectAsTextFieldValueState()
    val canChangeDisplayName = accountSingleViewModel.canChangeDisplayName.collectAsState().value

    val focusRequester = remember { FocusRequester() }
    val editMode = remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (editMode.value) {
            Tooltip({ Text(i18n.commonCancel()) }) {
                ThemedIconButton(
                    style = MaterialTheme.components.commonIconButton,
                    onClick = {
                        accountSingleViewModel.cancelEditDisplayName()
                        editMode.value = false
                    },
                ) {
                    EditIcon(Icons.Default.Clear, i18n.commonCancel())
                }
            }
            Spacer(Modifier.size(10.dp))
            OutlinedTextField(
                editDisplayName,
                { editDisplayName = it },
                Modifier.onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown) {
                            when (it.key) {
                                Key.Enter -> {
                                    done(editMode, accountSingleViewModel)
                                    true
                                }

                                else -> false
                            }
                        } else {
                            false
                        }
                    }
                    .focusRequester(focusRequester)
                    .weight(1.0f, fill = true),
                label = { Text(i18n.profileYourName(), fontWeight = FontWeight.Bold) },
                singleLine = true,
                maxLines = 1,
                keyboardActions = KeyboardActions(onDone = { done(editMode, accountSingleViewModel) }),
            )
            Spacer(Modifier.size(10.dp))
        } else {
            Column(Modifier.weight(1.0f, fill = true)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(i18n.profileYourName(), fontWeight = FontWeight.Bold)
                    HelpIcon(i18n.profileYourNameInfo())
                }
                Spacer(Modifier.size(5.dp))
                ThemedSelectableText(displayName, MaterialTheme.components.selectionOnSurface)
            }
        }
        if (editMode.value) {
            Tooltip({ Text(i18n.commonAcceptEdit()) }) {
                ThemedIconButton(
                    style = MaterialTheme.components.commonIconButton,
                    onClick = { done(editMode, accountSingleViewModel) },
                ) {
                    EditIcon(Icons.Default.Check, i18n.commonAcceptEdit())
                }
            }
        } else {
            Tooltip({ Text(i18n.commonEdit()) }) {
                ThemedIconButton(
                    enabled = canChangeDisplayName,
                    style = MaterialTheme.components.commonIconButton,
                    onClick = { editMode.value = true },
                ) {
                    EditIcon(Icons.Default.Edit, i18n.commonEdit())
                }
            }
        }
    }
    LaunchedEffect(editMode.value) {
        if (editMode.value) {
            delay(200.milliseconds)
            focusRequester.requestFocus()
        }
    }
}

private fun done(editMode: MutableState<Boolean>, accountSingleViewModel: AccountSingleViewModel) {
    editMode.value = false
    accountSingleViewModel.saveDisplayName()
}

@Composable
fun AccountUserId(accountSingleViewModel: AccountSingleViewModel) {
    val i18n = DI.get<I18nView>()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(i18n.profileUserName(), fontWeight = FontWeight.Bold)
            HelpIcon(i18n.profileUserNameInfo())
        }
        Spacer(Modifier.size(5.dp))
        ThemedSelectableText(accountSingleViewModel.userId.full, MaterialTheme.components.selectionOnSurface)
    }
}
