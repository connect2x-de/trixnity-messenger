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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
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
        val isEditable = roomSettingsNameViewModel.canChangeRoomName.collectAsState()
        val value = roomSettingsNameViewModel.roomName.collectAsTextFieldValueState()

        val editLabel = if (isEditable.value) i18n.commonEdit() else i18n.roomSettingsRoomNameCannotChange()

        if (!isEdit.value) {
            Row(verticalAlignment = Alignment.Top) {
                if (value.value.text.isBlank()) {
                    Text(
                        i18n.roomSettingsRoomNamePlaceholder(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically).weight(1f, true)
                    )
                } else {
                    Text(
                        value.value.text,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.align(Alignment.CenterVertically).weight(1f, true)
                    )
                }
                Tooltip({ Text(editLabel) }) {
                    ThemedIconButton(
                        onClick = { roomSettingsNameViewModel.roomName.startEdit() },
                        enabled = isEditable.value,
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = editLabel)
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
                    ) {
                        Text(i18n.commonAcceptEdit())
                    }
                }
            }
        }
    }
}
