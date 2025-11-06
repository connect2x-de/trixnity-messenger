package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.IsFocusHighlighting
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
import de.connect2x.messenger.compose.view.theme.messengerFocusIndicator
import de.connect2x.messenger.compose.view.util.LocalRovingFocus
import de.connect2x.messenger.compose.view.util.LocalRovingFocusItem
import de.connect2x.messenger.compose.view.util.RovingFocusContainer
import de.connect2x.messenger.compose.view.util.RovingFocusItem
import de.connect2x.messenger.compose.view.util.inputFocusNavigation
import de.connect2x.messenger.compose.view.util.rovingFocusItem
import de.connect2x.messenger.compose.view.util.verticalRovingFocus
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountWithDevices
import de.connect2x.trixnity.messenger.viewmodel.settings.DeviceInfo
import de.connect2x.trixnity.messenger.viewmodel.settings.DevicesSettingsViewModel
import net.folivo.trixnity.core.MSC3814

interface DevicesSettingsView {
    @Composable
    fun create(devicesSettingsViewModel: DevicesSettingsViewModel)
}

@Composable
fun DevicesSettings(devicesSettingsViewModel: DevicesSettingsViewModel) {
    DI.get<DevicesSettingsView>().create(devicesSettingsViewModel)
}

class DevicesSettingsViewImpl : DevicesSettingsView {
    @Composable
    override fun create(devicesSettingsViewModel: DevicesSettingsViewModel) {
        val i18n = DI.get<I18nView>()
        val error = devicesSettingsViewModel.error.collectAsState()
        val scroll = rememberScrollState()
        Box(Modifier.fillMaxSize()) {
            Column {
                Header(devicesSettingsViewModel::back, i18n.devicesTitle())
                error.value?.let { ErrorView(it) }
                Box(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.verticalScroll(scroll).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AccountDevices(devicesSettingsViewModel)
                    }
                    VerticalScrollbar(
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight(), scroll
                    )
                }
            }
        }
    }
}

@Composable
fun AccountDevices(devicesSettingsViewModel: DevicesSettingsViewModel) {
    val devices = devicesSettingsViewModel.accountsWithDevices.collectAsState().value
    devices.map { accountWithDevices ->
        AccountWithDevicesList(accountWithDevices, devicesSettingsViewModel)
    }
}

@Composable
fun AccountWithDevicesList(
    accountWithDevices: AccountWithDevices,
    devicesSettingsViewModel: DevicesSettingsViewModel,
) {
    val i18n = DI.get<I18nView>()
    val isLoading by accountWithDevices.isLoading.collectAsState()
    val error by accountWithDevices.loadingError.collectAsState()
    val devicesInAccount = accountWithDevices.devicesInAccount.collectAsState().value

    val onRename: (DeviceInfo, String) -> Unit = remember(devicesSettingsViewModel, accountWithDevices) {
        { device, displayName ->
            devicesSettingsViewModel.setDisplayName(
                accountWithDevices.userId,
                device.deviceId,
                device.displayName.value,
                displayName,
            )
        }
    }

    val onVerify: (DeviceInfo) -> Unit = remember(devicesSettingsViewModel, accountWithDevices) {
        { device ->
            devicesSettingsViewModel.verify(accountWithDevices.userId, device.deviceId)
        }
    }

    val onDelete: (DeviceInfo) -> Unit = remember(devicesSettingsViewModel, accountWithDevices) {
        { device ->
            devicesSettingsViewModel.remove(accountWithDevices.userId, device.deviceId)
        }
    }

    if (devicesInAccount == null) {
        SettingsAccountCard(
            accountWithDevices.userId,
        ) {
            error?.let { ErrorView(it) }
            if (isLoading) {
                LoadingSpinner()
            }
        }
    } else {
        val defaultItem = devicesInAccount.thisDevice.deviceId
        val references = remember {
            derivedStateOf {
                buildList {
                    add(devicesInAccount.thisDevice.deviceId)
                    for (device in devicesInAccount.otherDevices) {
                        add(device.deviceId)
                    }
                }
            }
        }

        RovingFocusContainer {
            SettingsAccountCard(
                accountWithDevices.userId,
                modifier = Modifier.verticalRovingFocus(
                    default = defaultItem,
                    up = {
                        val currentItem = activeRef.value ?: defaultItem
                        val currentIndex = references.value.indexOf(currentItem)
                        val nextIndex = currentIndex.minus(1).coerceIn(references.value.indices)
                        references.value[nextIndex]
                    },
                    down = {
                        val currentItem = activeRef.value ?: defaultItem
                        val currentIndex = references.value.indexOf(currentItem)
                        val nextIndex = currentIndex.plus(1).coerceIn(references.value.indices)
                        references.value[nextIndex]
                    },
                )
            ) {
                error?.let { ErrorView(it) }
                if (isLoading) {
                    LoadingSpinner()
                } else {
                    ThemedListItem(
                        style = MaterialTheme.components.settingsItem,
                        headlineContent = {
                            Text(i18n.devicesThisDevice(), style = MaterialTheme.typography.titleMedium)
                        }
                    )
                    RovingFocusItem(devicesInAccount.thisDevice.deviceId, defaultItem) {
                        DeviceItem(
                            devicesInAccount.thisDevice,
                            onRename = onRename,
                            onVerify = onVerify,
                            onDelete = onDelete,
                        )
                    }
                    if (devicesInAccount.otherDevices.isNotEmpty()) {
                        ThemedListItem(
                            style = MaterialTheme.components.settingsItem,
                            headlineContent = {
                                Text(i18n.devicesOtherDevices(), style = MaterialTheme.typography.titleMedium)
                            }
                        )
                        for (device in devicesInAccount.otherDevices) {
                            RovingFocusItem(device.deviceId, defaultItem) {
                                DeviceItem(
                                    device,
                                    onRename = onRename,
                                    onVerify = onVerify,
                                    onDelete = onDelete,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(MSC3814::class)
@Composable
fun DeviceItem(
    device: DeviceInfo,
    modifier: Modifier = Modifier,
    onRename: (DeviceInfo, String) -> Unit,
    onVerify: (DeviceInfo) -> Unit,
    onDelete: (DeviceInfo) -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val displayName = device.displayName.collectAsState()
    val isVerified = device.isVerified.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val focusContainer = LocalRovingFocus.current
    val focusItem = LocalRovingFocusItem.current
    val focused = interactionSource.collectIsFocusedAsState()
    val focusedBorder =
        if (IsFocusHighlighting.current && focused.value) {
            Modifier.border(
                width = MaterialTheme.messengerFocusIndicator.borderWidth,
                color = MaterialTheme.colorScheme.onBackground,
            )
        } else Modifier

    val showOptions = remember { mutableStateOf(false) }
    val showRename = remember { mutableStateOf(false) }

    ThemedListItem(
        style = MaterialTheme.components.settingsItem,
        modifier = modifier
            .then(focusedBorder),
        leadingContent = {
            if (isVerified.value) {
                VerifiedIcon(VerificationLevel.DEVICE)
            } else {
                NotVerifiedIcon(VerificationLevel.DEVICE)
            }
        },
        headlineContent = {
            Text(displayName.value)
            if (device.isDehydrated) {
                Tooltip({ Text(i18n.dehydratedDevice()) }) {
                    Icon(
                        Icons.Default.RestoreFromTrash,
                        i18n.dehydratedDevice(),
                    )
                }
            }
        },
        supportingContent = { Text(device.lastSeenAt) },
        trailingContent = {
            Box {
                Tooltip({ Text(i18n.commonMore()) }) {
                    ThemedIconButton(
                        style = MaterialTheme.components.commonIconButton,
                        modifier = Modifier.rovingFocusItem(),
                        interactionSource = interactionSource,
                        onClick = {
                            showOptions.value = true
                            focusContainer?.selectItem(focusItem?.key, shouldFocus = true)
                        },
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
                    if (!isVerified.value) {
                        ThemedDropdownMenuItem(
                            text = { Text(i18n.commonVerify().capitalize(Locale.current)) },
                            leadingIcon = { Icon(Icons.Default.GppGood, null) },
                            onClick = {
                                onVerify(device)
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
                            onDelete(device)
                            showOptions.value = false
                        },
                    )
                }
            }
        },
    )

    if (showRename.value) {
        RenameDeviceDialog(
            device,
            {
                onRename(device, it)
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
    device: DeviceInfo,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val focusRequester = remember { FocusRequester() }
    val content = remember { mutableStateOf(TextFieldValue(device.displayName.value)) }

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
