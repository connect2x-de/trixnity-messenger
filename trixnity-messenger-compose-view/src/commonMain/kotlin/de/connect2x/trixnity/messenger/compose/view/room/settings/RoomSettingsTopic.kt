package de.connect2x.trixnity.messenger.compose.view.room.settings

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.richtext.RichTextColors
import de.connect2x.trixnity.messenger.compose.view.richtext.RichTextDisplay
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSelectionContainer
import de.connect2x.trixnity.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.compose.view.util.inputFocusNavigation
import de.connect2x.trixnity.messenger.util.UriCaller
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsTopicViewModel

interface RoomSettingsTopicView {
    @Composable fun create(roomSettingsTopicViewModel: RoomSettingsTopicViewModel)
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
        val canChangeRoomTopic = roomSettingsTopicViewModel.canChangeRoomTopic.collectAsState()
        val value = roomSettingsTopicViewModel.roomTopic.collectAsTextFieldValueState()
        val content = roomSettingsTopicViewModel.formattedRoomTopic.collectAsState()
        val uriCaller = DI.get<UriCaller>()

        val oldValue by remember(isEdit.value) { mutableStateOf(value.value.text) }

        if (!isEdit.value) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.align(Alignment.CenterVertically).weight(1f, fill = true)) {
                    if (value.value.text.isBlank() && canChangeRoomTopic.value) {
                        Text(
                            i18n.roomSettingsRoomTopicPlaceholder(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!value.value.text.isBlank()) {
                        ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                            ThemedSelectionContainer(MaterialTheme.components.selectionOnSurface) {
                                RichTextDisplay(
                                    document = content.value,
                                    mentions = roomSettingsTopicViewModel.mentionsInFormattedRoomTopic,
                                    colors = RichTextColors.default(linkColor = MaterialTheme.messengerColors.link),
                                    onCopy = null,
                                    onLinkClick = { uriCaller.invoke(it, true) },
                                    onMentionClick = roomSettingsTopicViewModel::openMention,
                                )
                            }
                        }
                    }
                }
                if (canChangeRoomTopic.value) {
                    Tooltip(i18n.editRoomTopic()) {
                        ThemedIconButton(
                            onClick = { roomSettingsTopicViewModel.roomTopic.startEdit() },
                            enabled = canChangeRoomTopic.value,
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = i18n.editRoomTopic())
                        }
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
                        enabled = oldValue != value.value.text,
                    ) {
                        Text(i18n.commonAcceptEdit())
                    }
                }
            }
        }
    }
}
