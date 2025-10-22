package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.messenger.compose.view.common.Tooltip
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.richtext.RichTextColors
import de.connect2x.messenger.compose.view.richtext.RichTextDisplay
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.theme.components.ThemedSelectionContainer
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.messenger.compose.view.util.inputFocusNavigation
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsTopicViewModel

interface RoomSettingsTopicView {
    @Composable
    fun create(roomSettingsTopicViewModel: RoomSettingsTopicViewModel)
}

@Composable
fun RoomSettingsTopic(roomSettingsTopicViewModel: RoomSettingsTopicViewModel) {
    DI.get<RoomSettingsTopicView>().create(roomSettingsTopicViewModel)
}

class RoomSettingsTopicViewImpl : RoomSettingsTopicView {
    @Composable
    override fun create(roomSettingsTopicViewModel: RoomSettingsTopicViewModel) {
        val i18n = DI.get<I18nView>()
        val isEdit = roomSettingsTopicViewModel.roomTopic.isEdit.collectAsState()
        val isEditable = roomSettingsTopicViewModel.canChangeRoomTopic.collectAsState()
        val value = roomSettingsTopicViewModel.roomTopic.collectAsTextFieldValueState()
        val content = roomSettingsTopicViewModel.formattedRoomTopic.collectAsState()

        val editLabel = if (isEditable.value) i18n.commonEdit() else i18n.roomSettingsRoomTopicCannotChange()

        if (!isEdit.value) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.align(Alignment.CenterVertically).weight(1f, fill = true)) {
                    if (value.value.text.isBlank()) {
                        Text(
                            i18n.roomSettingsRoomTopicPlaceholder(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val uriHandler = LocalUriHandler.current

                        ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                            ThemedSelectionContainer(MaterialTheme.components.selectionOnSurface) {
                                RichTextDisplay(
                                    content.value,
                                    colors = RichTextColors.default(linkColor = MaterialTheme.messengerColors.link),
                                    onLinkClick = { uriHandler.openUri(it) },
                                )
                            }
                        }
                    }
                }
                Tooltip({ Text(editLabel) }) {
                    ThemedIconButton(
                        onClick = { roomSettingsTopicViewModel.roomTopic.startEdit() },
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
                    label = { Text(text = i18n.roomSettingsRoomTopic()) },
                    modifier = Modifier.inputFocusNavigation().fillMaxWidth(),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
                ) {
                    ThemedButton(
                        style = MaterialTheme.components.destructiveButton,
                        onClick = { roomSettingsTopicViewModel.roomTopic.cancelEdit() },
                    ) {
                        Text(i18n.actionCancel())
                    }
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        onClick = { roomSettingsTopicViewModel.roomTopic.approveEdit() },
                    ) {
                        Text(i18n.commonAcceptEdit())
                    }
                }
            }
        }
    }
}
