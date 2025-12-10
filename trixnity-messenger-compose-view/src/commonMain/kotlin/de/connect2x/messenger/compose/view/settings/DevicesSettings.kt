package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.Tooltip
import de.connect2x.messenger.compose.view.common.icons.EditIcon
import de.connect2x.messenger.compose.view.common.icons.NotVerifiedIcon
import de.connect2x.messenger.compose.view.common.icons.VerificationLevel
import de.connect2x.messenger.compose.view.common.icons.VerifiedIcon
import de.connect2x.messenger.compose.view.common.modifier.focusHighlighting
import de.connect2x.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.messenger.compose.view.common.modifier.rovingFocusItem
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedDropdownMenu
import de.connect2x.messenger.compose.view.theme.components.ThemedDropdownMenuItem
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.theme.components.ThemedListItem
import de.connect2x.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.messenger.compose.view.util.inputFocusNavigation
import de.connect2x.trixnity.messenger.viewmodel.settings.DeviceSettingsAllAccountsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.DeviceSettingsSingleAccountViewModel
import net.folivo.trixnity.core.MSC3814

interface DeviceSettingsView {
    @Composable
    fun create(devicesSettingsViewModel: DeviceSettingsAllAccountsViewModel)
}

@Composable
fun DeviceSettings(devicesSettingsViewModel: DeviceSettingsAllAccountsViewModel) {
    DI.get<DeviceSettingsView>().create(devicesSettingsViewModel)
}

class DeviceSettingsViewImpl : DeviceSettingsView {
    @Composable
    override fun create(devicesSettingsViewModel: DeviceSettingsAllAccountsViewModel) {
        val i18n = DI.get<I18nView>()
        val notificationSettings = devicesSettingsViewModel.deviceSettings
        val scroll = rememberScrollState()

        Column(Modifier.fillMaxSize()) {
            Header(devicesSettingsViewModel::back, i18n.devicesTitle().capitalize(Locale.current))

            Box {
                Column(Modifier.padding(10.dp).verticalScroll(scroll)) {
                    notificationSettings.map { deviceSettingsSingleAccount ->
                        DeviceSettingsSingleAccount(deviceSettingsSingleAccount)
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

@Composable
fun DeviceSettingsSingleAccount(viewModel: DeviceSettingsSingleAccountViewModel) {
    val isLoading by viewModel.isLoading.collectAsState()
    val devices = viewModel.devices.collectAsState().value
    val error by viewModel.error.collectAsState()

    if (isLoading || devices == null) {
        SettingsAccountCard(viewModel.account) {
            error?.let { ErrorView(it) }
            if (isLoading) {
                LoadingSpinner()
            }
        }
    } else {
        val thisDevice = devices.find { it.isThisDevice }
        val otherDevices = devices.filter { it.isThisDevice.not() }
        var focusedItem by remember { mutableStateOf(thisDevice?.deviceId) }

        SettingsAccountCard(viewModel.account, modifier = Modifier.rovingFocusContainer()) {
            val i18n = DI.get<I18nView>()
            error?.let { ErrorView(it) }
            if (isLoading || thisDevice == null) {
                LoadingSpinner()
            } else {
                ThemedListItem(
                    style = MaterialTheme.components.settingsItem,
                    headlineContent = {
                        Text(i18n.devicesThisDevice(), style = MaterialTheme.typography.titleMedium)
                    }
                )

                DeviceItem(
                    viewModel = viewModel,
                    device = thisDevice,
                    isFocused = focusedItem == thisDevice.deviceId,
                    onFocus = { focusedItem = thisDevice.deviceId },
                )

                if (otherDevices.isNotEmpty()) {
                    ThemedListItem(
                        style = MaterialTheme.components.settingsItem,
                        headlineContent = {
                            Text(i18n.devicesOtherDevices(), style = MaterialTheme.typography.titleMedium)
                        }
                    )
                    for (device in otherDevices) {
                        DeviceItem(
                            viewModel = viewModel,
                            device = device,
                            isFocused = focusedItem == device.deviceId,
                            onFocus = { focusedItem = device.deviceId },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(MSC3814::class)
@Composable
fun DeviceItem(
    viewModel: DeviceSettingsSingleAccountViewModel,
    device: DeviceSettingsSingleAccountViewModel.DeviceInfo,
    isFocused: Boolean,
    onFocus: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val displayName = device.displayName
    val isVerified = device.isVerified

    val showOptions = remember { mutableStateOf(false) }
    val showRename = remember { mutableStateOf(false) }

    Box(Modifier.semantics { contentDescription = "${displayName}, ${device.lastSeenAt}" }) {
        ThemedListItem(
            style = MaterialTheme.components.settingsItem,
            modifier = Modifier.rovingFocusItem(isFocused, onFocus).focusHighlighting(),
            leadingContent = {
                if (isVerified) {
                    VerifiedIcon(VerificationLevel.DEVICE)
                } else {
                    NotVerifiedIcon(VerificationLevel.DEVICE)
                }
            },
            headlineContent = {
                Text(displayName)
                if (device.isDehydrated) {
                    Tooltip({ Text(i18n.dehydratedDevice()) }) {
                        Icon(
                            Icons.Default.RestoreFromTrash,
                            i18n.dehydratedDevice(),
                        )
                    }
                }
            },
            supportingContent = { Text(device.deviceId + " - " + device.lastSeenAt) },
            trailingContent = {
                Box {
                    Tooltip({ Text(i18n.commonMore()) }) {
                        val interactionSource = remember { MutableInteractionSource() }
                        ThemedIconButton(
                            style = MaterialTheme.components.commonIconButton,
                            interactionSource = interactionSource,
                            onClick = { showOptions.value = true },
                        ) {
                            EditIcon(Icons.Default.MoreVert, i18n.commonMore())
                        }
                    }
                    ThemedDropdownMenu(
                        expanded = showOptions.value,
                        onDismissRequest = { showOptions.value = false },
                    ) {
                        ThemedDropdownMenuItem(
                            text = { Text(i18n.commonRename().capitalize(Locale.current)) },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = {
                                showRename.value = true
                                showOptions.value = false
                            },
                        )
                        if (!isVerified) {
                            ThemedDropdownMenuItem(
                                text = { Text(i18n.commonVerify().capitalize(Locale.current)) },
                                leadingIcon = { Icon(Icons.Default.GppGood, null) },
                                onClick = {
                                    viewModel.verify(device.deviceId)
                                    showOptions.value = false
                                },
                            )
                        }
                        ThemedDropdownMenuItem(
                            text = { Text(i18n.commonRemove().capitalize(Locale.current)) },
                            leadingIcon = {
                                Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = {
                                viewModel.remove(device.deviceId)
                                showOptions.value = false
                            },
                        )
                    }
                }
            },
        )
    }

    if (showRename.value) {
        RenameDeviceDialog(
            device,
            {
                viewModel.setDisplayName(device.deviceId, it)
                showRename.value = false
            },
            onDismiss = {
                showRename.value = false
            }
        )
    }
}

@Composable
private fun RenameDeviceDialog(
    device: DeviceSettingsSingleAccountViewModel.DeviceInfo,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val focusRequester = remember { FocusRequester() }
    val content = remember { mutableStateOf(TextFieldValue(device.displayName)) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ThemedModalDialog(onDismiss) {
        ModalDialogHeader {
            Text(i18n.devicesRenameDevice())
        }
        ModalDialogContent {
            OutlinedTextField(
                value = content.value,
                onValueChange = { content.value = it },
                label = { Text(i18n.devicesDeviceName()) },
                maxLines = 1,
                singleLine = true,
                modifier = Modifier.inputFocusNavigation()
                    .focusRequester(focusRequester)
                    .fillMaxWidth(),
            )
        }
        ModalDialogFooter {
            ThemedButton(
                style = MaterialTheme.components.commonButton,
                onClick = onDismiss,
            ) {
                Text(i18n.actionCancel())
            }
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                onClick = { onRename(content.value.text) }
            ) {
                Text(i18n.commonRename())
            }
        }
    }
}
