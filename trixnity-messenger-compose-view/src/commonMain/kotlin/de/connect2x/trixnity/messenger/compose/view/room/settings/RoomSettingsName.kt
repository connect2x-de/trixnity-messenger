package de.connect2x.trixnity.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.util.inputFocusNavigation
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsNameViewModel

interface RoomSettingsNameView {
    @Composable
    fun create(roomSettingsNameViewModel: RoomSettingsNameViewModel)
}

@Composable
fun RoomSettingsName(roomSettingsNameViewModel: RoomSettingsNameViewModel) {
    DI.get<RoomSettingsNameView>().create(roomSettingsNameViewModel)
}

class RoomSettingsNameViewImpl : RoomSettingsNameView {
    @Composable
    override fun create(roomSettingsNameViewModel: RoomSettingsNameViewModel) {
        val i18n = DI.get<I18nView>()
        val isEdit = roomSettingsNameViewModel.roomName.isEdit.collectAsState()
        val canChangeRoomName = roomSettingsNameViewModel.canChangeRoomName.collectAsState().value
        val value = roomSettingsNameViewModel.roomName.collectAsTextFieldValueState()

        val oldValue by remember(isEdit.value) { mutableStateOf(value.value.text) }

        if (!isEdit.value) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    value.value.text.ifBlank { i18n.roomSettingsRoomNamePlaceholder() },
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (canChangeRoomName) Tooltip(i18n.editRoomName()) {
                    ThemedIconButton(
                        onClick = { roomSettingsNameViewModel.roomName.startEdit() },
                        Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = i18n.editRoomName())
                    }
                }
            }
        } else {
            Column {
                OutlinedTextField(
                    value = value.value,
                    onValueChange = { value.value = it },
                    label = { Text(text = i18n.roomSettingsRoomName()) },
                    modifier = Modifier.inputFocusNavigation().fillMaxWidth(),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
                ) {
                    ThemedButton(
                        style = MaterialTheme.components.destructiveButton,
                        onClick = { roomSettingsNameViewModel.roomName.cancelEdit() },
                    ) {
                        Text(i18n.actionCancel())
                    }
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        onClick = { roomSettingsNameViewModel.roomName.approveEdit() },
                        enabled = oldValue != value.value.text,
                    ) {
                        Text(i18n.commonAcceptEdit())
                    }
                }
            }
        }
    }
}
