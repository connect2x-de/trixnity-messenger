package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.MoreOptions
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.common.icons.HelpIcon
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.i18n.getExplanation
import de.connect2x.messenger.compose.view.i18n.getExplanationWhenEncrypted
import de.connect2x.messenger.compose.view.i18n.getStateName
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsHistoryVisibilityViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent

interface RoomSettingsHistoryVisibilityView {
    @Composable
    fun create(roomSettingsViewModel: RoomSettingsViewModel)
}

@Composable
fun RoomSettingsHistoryVisibility(roomSettingsViewModel: RoomSettingsViewModel) {
    DI.current.get<RoomSettingsHistoryVisibilityView>().create(roomSettingsViewModel)
}

class RoomSettingsHistoryVisibilityViewImpl : RoomSettingsHistoryVisibilityView {
    @Composable
    override fun create(roomSettingsViewModel: RoomSettingsViewModel) {
        val i18n = DI.current.get<I18nView>()
        val historyVisibility =
            roomSettingsViewModel.roomSettingsHistoryVisibilityViewModel.roomHistoryVisibility.collectAsState().value
        val canChangeRoomHistoryVisibility =
            roomSettingsViewModel.roomSettingsHistoryVisibilityViewModel.canChangeRoomHistoryVisibility.collectAsState().value
        val isHistoryVisibilityChanging =
            roomSettingsViewModel.roomSettingsHistoryVisibilityViewModel.isHistoryVisibilityChanging.collectAsState().value
        val isEncrypted = roomSettingsViewModel.isEncrypted.collectAsState().value

        Column {
            Text(text = i18n.chatHistoryVisibility(), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(10.dp))
            Column {
                if (!canChangeRoomHistoryVisibility) {
                    Tooltip(tooltip = {
                        TooltipText {
                            if (isEncrypted) historyVisibility.getExplanationWhenEncrypted(i18n) else historyVisibility.getExplanation(
                                i18n
                            )
                        }
                    }) {
                        Text(
                            text = historyVisibility.getStateName(i18n),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                } else {
                    MoreOptions(
                        title = {
                            if (isHistoryVisibilityChanging) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Tooltip(tooltip = {
                                    TooltipText {
                                        if (isEncrypted) historyVisibility.getExplanationWhenEncrypted(
                                            i18n
                                        ) else historyVisibility.getExplanation(i18n)
                                    }
                                }) {
                                    Text(historyVisibility.getStateName(i18n))
                                }
                            }
                        },
                        content = {
                            VisibilityOptions(
                                roomSettingsViewModel.roomSettingsHistoryVisibilityViewModel,
                                roomSettingsViewModel
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VisibilityOption(
    visibility: HistoryVisibilityEventContent.HistoryVisibility,
    roomSettingsHistoryVisibilityViewModel: RoomSettingsHistoryVisibilityViewModel,
    roomSettingsViewModel: RoomSettingsViewModel
) {
    val i18n = DI.current.get<I18nView>()
    val historyVisibility = roomSettingsHistoryVisibilityViewModel.roomHistoryVisibility.collectAsState().value
    val isHistoryVisibilityChanging =
        roomSettingsHistoryVisibilityViewModel.isHistoryVisibilityChanging.collectAsState().value
    val isEncrypted = roomSettingsViewModel.isEncrypted.collectAsState().value
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .clickable(enabled = roomSettingsHistoryVisibilityViewModel.historyVisibilityCanBeChangedTo(visibility)) {
                if (isHistoryVisibilityChanging.not()) {
                    roomSettingsHistoryVisibilityViewModel.changeRoomHistoryVisibility(visibility)
                }
            }
            .buttonPointerModifier(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HelpIcon(if (isEncrypted) visibility.getExplanationWhenEncrypted(i18n) else visibility.getExplanation(i18n))
        Text(visibility.getStateName(i18n), modifier = Modifier.weight(1.0f, fill = true))
        RadioButton(
            selected = historyVisibility == visibility,
            onClick = { roomSettingsHistoryVisibilityViewModel.changeRoomHistoryVisibility(visibility) },
            enabled = (isHistoryVisibilityChanging.not() && roomSettingsHistoryVisibilityViewModel.historyVisibilityCanBeChangedTo(
                visibility
            )),
        )
    }
}

@Composable
fun ColumnScope.VisibilityOptions(
    roomSettingsHistoryVisibilityViewModel: RoomSettingsHistoryVisibilityViewModel,
    roomSettingsViewModel: RoomSettingsViewModel
) {
    val availableVisibilities =
        roomSettingsHistoryVisibilityViewModel.availableRoomHistoryVisibilities.collectAsState().value ?: return
    for (visibility in availableVisibilities) {
        VisibilityOption(visibility, roomSettingsHistoryVisibilityViewModel, roomSettingsViewModel)
    }
}