package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.icons.EditIcon
import de.connect2x.messenger.compose.view.common.icons.NotVerifiedIcon
import de.connect2x.messenger.compose.view.common.icons.VerificationLevel
import de.connect2x.messenger.compose.view.common.icons.VerifiedIcon
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountWithDevices
import de.connect2x.trixnity.messenger.viewmodel.settings.DeviceInfo
import de.connect2x.trixnity.messenger.viewmodel.settings.DevicesSettingsViewModel
import kotlinx.coroutines.delay
import net.folivo.trixnity.core.model.UserId

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
                    Column(Modifier.padding(10.dp).verticalScroll(scroll)) {
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
        Spacer(Modifier.size(10.dp))
    }
}

@Composable
fun AccountWithDevicesList(
    accountWithDevices: AccountWithDevices,
    devicesSettingsViewModel: DevicesSettingsViewModel,
) {
    val isLoading by accountWithDevices.isLoading.collectAsState()
    val error by accountWithDevices.loadingError.collectAsState()
    val devicesInAccount = accountWithDevices.devicesInAccount.collectAsState().value
    SettingsAccountCard(accountWithDevices.userId) {
        error?.let { ErrorView(it) }
        if (isLoading) {
            LoadingSpinner()
        } else if (devicesInAccount != null) {
            ThisDevice(accountWithDevices.userId, devicesInAccount.thisDevice, devicesSettingsViewModel)
            Spacer(Modifier.size(20.dp))
            OtherDevices(accountWithDevices.userId, devicesInAccount.otherDevices, devicesSettingsViewModel)
        }
    }
}

@Composable
fun ThisDevice(userId: UserId, device: DeviceInfo, devicesSettingsViewModel: DevicesSettingsViewModel) {
    val i18n = DI.get<I18nView>()
    val displayName = device.displayName.collectAsState().value
    val editDeviceDisplayName = remember { mutableStateOf(false) }
    val editedDisplayName = remember {
        mutableStateOf(
            TextFieldValue(
                text = displayName,
                selection = TextRange(index = displayName.length),
            )
        )
    }
    Column(Modifier.padding(10.dp)) {
        Text(i18n.devicesThisDevice(), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.size(10.dp))
        DeviceRow(userId, device, devicesSettingsViewModel, editDeviceDisplayName, editedDisplayName) {
            Row(Modifier.padding(vertical = 8.dp)) {
                if (editDeviceDisplayName.value) {
                    Spacer(Modifier.size(10.dp))
                    Tooltip({Text(i18n.commonDone())}) {
                        ThemedIconButton(
                            style = MaterialTheme.components.commonIconButton,
                            onClick = {
                                editDeviceDisplayName.value = editDeviceDisplayName.value.not()
                                devicesSettingsViewModel.setDisplayName(
                                    userId = userId,
                                    deviceId = device.deviceId,
                                    oldDisplayName = displayName,
                                    newDisplayName = editedDisplayName.value.text,
                                )
                            },
                            modifier = Modifier.alignByBaseline(),
                        ) {
                            EditIcon(Icons.Default.Done, i18n.commonDone())
                        }
                    }
                } else {
                    Spacer(Modifier.size(20.dp))
                    Tooltip({Text(i18n.commonRename())}) {
                        ThemedIconButton(
                            style = MaterialTheme.components.commonIconButton,
                            onClick = {
                                editDeviceDisplayName.value = editDeviceDisplayName.value.not()
                            },
                            modifier = Modifier.alignByBaseline(),
                        ) {
                            EditIcon(Icons.Default.Edit, i18n.commonRename())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OtherDevices(
    userId: UserId,
    otherDevices: List<DeviceInfo>,
    devicesSettingsViewModel: DevicesSettingsViewModel
) {
    val i18n = DI.get<I18nView>()
    if (otherDevices.isNotEmpty()) {
        Column(Modifier.padding(10.dp)) {
            Text(i18n.devicesOtherDevices(), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.size(10.dp))
            otherDevices.mapIndexed { index, device ->
                if (index > 0) {
                    Spacer(Modifier.size(20.dp))
                    HorizontalDivider(Modifier.fillMaxWidth().width(1.dp))
                    Spacer(Modifier.size(20.dp))
                }
                OtherDevice(userId, device, devicesSettingsViewModel)
            }
        }
    }
}

@Composable
fun OtherDevice(userId: UserId, device: DeviceInfo, devicesSettingsViewModel: DevicesSettingsViewModel) {
    val i18n = DI.get<I18nView>()
    val displayName = device.displayName.collectAsState().value
    val isVerified = device.isVerified.collectAsState().value
    val showOptions = remember { mutableStateOf(false) }
    val editDeviceDisplayName = remember { mutableStateOf(false) }
    val editedDisplayName = remember {
        mutableStateOf(
            TextFieldValue(
                text = displayName,
                selection = TextRange(index = displayName.length),
            )
        )
    }
    DeviceRow(userId, device, devicesSettingsViewModel, editDeviceDisplayName, editedDisplayName) {
        Row(Modifier.padding(vertical = 8.dp)) {
            if (editDeviceDisplayName.value) {
                Spacer(Modifier.size(10.dp))
                Tooltip({Text(i18n.commonDone())}) {
                    ThemedIconButton(
                        style = MaterialTheme.components.commonIconButton,
                        onClick = {
                            editDeviceDisplayName.value = editDeviceDisplayName.value.not()
                            devicesSettingsViewModel.setDisplayName(
                                userId,
                                device.deviceId,
                                displayName,
                                editedDisplayName.value.text
                            )
                        },
                    ) {
                        EditIcon(Icons.Default.Done, i18n.commonDone())
                    }
                }
            } else {
                Tooltip({Text(i18n.commonMore())}) {
                    ThemedIconButton(
                        style = MaterialTheme.components.commonIconButton,
                        onClick = { showOptions.value = showOptions.value.not() },
                    ) {
                        Spacer(Modifier.size(20.dp))
                        EditIcon(Icons.Default.MoreVert, i18n.commonMore())
                        DropdownMenu(
                            expanded = showOptions.value,
                            modifier = Modifier.defaultMinSize(minWidth = 200.dp)
                                .background(MaterialTheme.colorScheme.background),
                            onDismissRequest = { showOptions.value = showOptions.value.not() },
                        ) {
                            DeviceDropdownItem(
                                i18n.commonRename().capitalize(Locale.current),
                                onClick = {
                                    editDeviceDisplayName.value = editDeviceDisplayName.value.not()
                                    showOptions.value = false
                                },
                                icon = Icons.Default.Edit
                            )
                            if (isVerified.not()) {
                                DeviceDropdownItem(
                                    i18n.commonVerify().capitalize(Locale.current),
                                    onClick = {
                                        devicesSettingsViewModel.verify(userId, device.deviceId)
                                        showOptions.value = false
                                    },
                                    icon = Icons.Default.GppGood,
                                )
                            }
                            DeviceDropdownItem(
                                i18n.commonRemove().capitalize(Locale.current),
                                onClick = {
                                    devicesSettingsViewModel.remove(userId, device.deviceId)
                                    showOptions.value = false
                                },
                                icon = Icons.Default.DeleteForever,
                                MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceRow(
    userId: UserId,
    device: DeviceInfo,
    devicesSettingsViewModel: DevicesSettingsViewModel,
    editDeviceDisplayName: MutableState<Boolean>,
    editedDisplayName: MutableState<TextFieldValue>,
    editActions: @Composable () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val displayName = device.displayName.collectAsState().value
    val isVerified = device.isVerified.collectAsState().value
    val focusRequester = remember { FocusRequester() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (editDeviceDisplayName.value) {
            Tooltip({Text(i18n.commonCancel())}) {
                ThemedIconButton(
                    style = MaterialTheme.components.commonIconButton,
                    onClick = {
                        editDeviceDisplayName.value = editDeviceDisplayName.value.not()
                        editedDisplayName.value = TextFieldValue(
                            text = displayName,
                            selection = TextRange(index = displayName.length)
                        )
                    },
                ) {
                    EditIcon(Icons.Default.Clear, i18n.commonCancel())
                }
            }
            Spacer(Modifier.size(10.dp))
        } else {
            if (isVerified) {
                VerifiedIcon(VerificationLevel.DEVICE)
            } else {
                NotVerifiedIcon(VerificationLevel.DEVICE)
            }
            Spacer(Modifier.size(20.dp))
        }
        if (editDeviceDisplayName.value) {
            OutlinedTextField(
                editedDisplayName.value,
                onValueChange = { editedDisplayName.value = it },
                Modifier.onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown) {
                        when (it.key) {
                            Key.Enter -> {
                                editDeviceDisplayName.value = editDeviceDisplayName.value.not()
                                devicesSettingsViewModel.setDisplayName(
                                    userId = userId,
                                    deviceId = device.deviceId,
                                    oldDisplayName = displayName,
                                    newDisplayName = editedDisplayName.value.text
                                )
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
                singleLine = true,
                maxLines = 1,
            )
        } else {
            Column(Modifier.weight(1.0f, fill = true)) {
                Text(displayName)
                Spacer(Modifier.size(5.dp))
                Text(device.lastSeenAt, fontSize = 10.sp)
            }
        }
        editActions()
    }
    LaunchedEffect(editDeviceDisplayName.value) {
        if (editDeviceDisplayName.value) {
            delay(200)
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun DeviceDropdownItem(
    title: String,
    onClick: () -> Unit,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, title, tint = iconColor)
                Spacer(Modifier.size(10.dp))
                Text(title, color = MaterialTheme.colorScheme.onBackground)
            }
        },
        onClick = onClick,
        modifier = Modifier.buttonPointerModifier(),
        contentPadding = PaddingValues(horizontal = 10.dp),
    )
}
