package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.Avatar
import de.connect2x.messenger.compose.view.common.EditButton
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.common.FilePickerType.IMAGE_FILE
import de.connect2x.messenger.compose.view.common.FilePickerType.PHOTO_CAPTURE
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.collectAsStateForTextField
import de.connect2x.messenger.compose.view.common.icons.EditIcon
import de.connect2x.messenger.compose.view.common.icons.HelpIcon
import de.connect2x.messenger.compose.view.files.LoadFileDialog
import de.connect2x.messenger.compose.view.files.filterFilePickerOptionsByAvailability
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.settings.ProfileSingleViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.ProfileViewModel
import kotlinx.coroutines.delay

interface ProfileSettingsView {
    @Composable
    fun create(profileViewModel: ProfileViewModel)
}

@Composable
fun ProfileSettings(profileViewModel: ProfileViewModel) {
    DI.get<ProfileSettingsView>().create(profileViewModel)
}

class ProfileSettingsViewImpl : ProfileSettingsView {
    @Composable
    override fun create(profileViewModel: ProfileViewModel) {
        val openAvatarCutter = profileViewModel.openAvatarCutter.collectAsState().value
        if (openAvatarCutter != null) LoadFileDialog(
            filterFilePickerOptionsByAvailability(
                IMAGE_FILE,
                PHOTO_CAPTURE,
            ),
            { profileViewModel.openAvatarCutter(openAvatarCutter, it) },
            { profileViewModel.closeAvatarCutter() },
        )
        ProfileOverview(profileViewModel)
    }
}

@Composable
fun ProfileOverview(profileViewModel: ProfileViewModel) {
    val i18n = DI.get<I18nView>()
    val error = profileViewModel.error.collectAsState().value
    val profileSingleViewModels = profileViewModel.profileSingleViewModels.collectAsState().value
    val scroll = rememberScrollState()

    Box(Modifier.fillMaxSize()) {
        Column {
            Header(profileViewModel::close, i18n.profileTitle())
            error?.let { ErrorView(it) }

            Box {
                Box {
                    Column(Modifier.padding(10.dp).verticalScroll(scroll)) {
                        profileSingleViewModels.map { profileSingleViewModel ->
                            ProfileOfAccountCard(profileSingleViewModel, profileViewModel)
                        }
                    }
                    VerticalScrollbar(
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        scroll,
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileOfAccountCard(
    profileSingleViewModel: ProfileSingleViewModel,
    profileViewModel: ProfileViewModel
) {
    SettingsAccountCard(profileSingleViewModel.userId) {
        ProfileAvatar(profileSingleViewModel)
        Spacer(Modifier.size(10.dp))
        ProfileDisplayName(profileSingleViewModel, profileViewModel)
        Spacer(Modifier.size(10.dp))
        ProfileUserId(profileSingleViewModel)
    }
}

@Composable
fun ProfileAvatar(profileSingleViewModel: ProfileSingleViewModel) {
    val i18n = DI.get<I18nView>()
    val avatar = profileSingleViewModel.avatar.collectAsState().value
    val canChangeAvatar = profileSingleViewModel.canChangeAvatar.collectAsState().value
    val initials = profileSingleViewModel.initials.collectAsState().value

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        Box(Modifier.align(Alignment.Center)) {
            Avatar(avatar, initials, this@BoxWithConstraints.maxWidth.coerceAtMost(200.dp)) {
                Box(Modifier.align(Alignment.BottomEnd).padding(10.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.clip(CircleShape)
                    ) {
                        EditButton(
                            onClick = { profileSingleViewModel.openAvatarCutter.value = true },
                            enabled = canChangeAvatar,
                        )
                        { EditIcon(Icons.Default.PhotoCamera, i18n.profileAvatarChange()) }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDisplayName(profileSingleViewModel: ProfileSingleViewModel, profileViewModel: ProfileViewModel) {
    val i18n = DI.get<I18nView>()
    val displayName = profileSingleViewModel.displayName.collectAsState().value
    val editDisplayName = profileSingleViewModel.editDisplayName.collectAsStateForTextField().value
    val canChangeDisplayName = profileSingleViewModel.canChangeDisplayName.collectAsState().value

    val focusRequester = remember { FocusRequester() }
    val editMode = remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (editMode.value) {
            EditButton(
                onClick = {
                    profileViewModel.cancelEditDisplayName(profileSingleViewModel.userId)
                    editMode.value = false
                },
                enabled = canChangeDisplayName,
            ) {
                EditIcon(Icons.Default.Clear, i18n.commonCancel())
            }
            Spacer(Modifier.size(10.dp))
            OutlinedTextField(
                editDisplayName,
                { profileSingleViewModel.editDisplayName.value = it },
                Modifier
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown) {
                            when (it.key) {
                                Key.Enter -> {
                                    done(editMode, profileSingleViewModel, profileViewModel)
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
                keyboardActions = KeyboardActions(onDone = { done(editMode, profileSingleViewModel, profileViewModel) })
            )
            Spacer(Modifier.size(10.dp))
        } else {
            Column(Modifier.weight(1.0f, fill = true)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(i18n.profileYourName(), fontWeight = FontWeight.Bold)
                    HelpIcon(i18n.profileYourNameInfo())
                }
                Spacer(Modifier.size(5.dp))
                Text(displayName)
            }
        }
        if (editMode.value) {
            EditButton({ done(editMode, profileSingleViewModel, profileViewModel) })
            { EditIcon(Icons.Default.Check, i18n.commonAcceptEdit()) }
        } else {
            EditButton({ editMode.value = true }) {
                EditIcon(
                    Icons.Default.Edit,
                    i18n.commonEdit()
                )
            }
        }
    }
    LaunchedEffect(editMode.value) {
        if (editMode.value) {
            delay(200)
            focusRequester.requestFocus()
        }
    }
}

private fun done(
    editMode: MutableState<Boolean>,
    profileSingleViewModel: ProfileSingleViewModel,
    profileViewModel: ProfileViewModel,
) {
    editMode.value = false
    profileViewModel.saveDisplayName(profileSingleViewModel.userId)
}

@Composable
fun ProfileUserId(profileSingleViewModel: ProfileSingleViewModel) {
    val i18n = DI.get<I18nView>()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(i18n.profileUserName(), fontWeight = FontWeight.Bold)
            HelpIcon(i18n.profileUserNameInfo())
        }
        Spacer(Modifier.size(5.dp))
        Text(profileSingleViewModel.userId.full)
    }
}
