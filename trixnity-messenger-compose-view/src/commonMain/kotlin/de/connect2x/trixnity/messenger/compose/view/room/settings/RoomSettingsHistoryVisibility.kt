package de.connect2x.trixnity.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.RadioSetting
import de.connect2x.trixnity.messenger.compose.view.common.RadioSettingOption
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.i18n.getExplanation
import de.connect2x.trixnity.messenger.compose.view.i18n.getExplanationWhenEncrypted
import de.connect2x.trixnity.messenger.compose.view.i18n.getStateName
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItem
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel

interface RoomSettingsHistoryVisibilityView {
    @Composable fun create(roomSettingsViewModel: RoomSettingsViewModel)
}

@Composable
fun RoomSettingsHistoryVisibility(roomSettingsViewModel: RoomSettingsViewModel) {
    DI.get<RoomSettingsHistoryVisibilityView>().create(roomSettingsViewModel)
}

class RoomSettingsHistoryVisibilityViewImpl : RoomSettingsHistoryVisibilityView {
    @Composable
    override fun create(roomSettingsViewModel: RoomSettingsViewModel) {
        val i18n = DI.get<I18nView>()
        val roomSettingsHistoryVisibilityViewModel = roomSettingsViewModel.roomSettingsHistoryVisibilityViewModel
        val historyVisibility = roomSettingsHistoryVisibilityViewModel.roomHistoryVisibility.collectAsState().value
        val canChangeRoomHistoryVisibility =
            roomSettingsHistoryVisibilityViewModel.canChangeRoomHistoryVisibility.collectAsState().value
        val isHistoryVisibilityChanging =
            roomSettingsHistoryVisibilityViewModel.isHistoryVisibilityChanging.collectAsState().value
        val isEncrypted = roomSettingsViewModel.isEncrypted.collectAsState().value
        val visibilities =
            roomSettingsHistoryVisibilityViewModel.availableRoomHistoryVisibilities.collectAsState().value

        Column {
            ThemedListItem(
                headlineContent = { Text(i18n.chatHistoryVisibility(), style = MaterialTheme.typography.titleMedium) },
                style = MaterialTheme.components.settingsItem,
            )

            if (!canChangeRoomHistoryVisibility) {
                Tooltip(
                    tooltip = {
                        Text(
                            if (isEncrypted) historyVisibility.getExplanationWhenEncrypted(i18n)
                            else historyVisibility.getExplanation(i18n)
                        )
                    },
                    Modifier.semantics {
                        focused = false
                        text =
                            AnnotatedString(
                                i18n.chatHistoryVisibilitySettings() +
                                    ", " +
                                    historyVisibility.getStateName(i18n) +
                                    " " +
                                    i18n.selected()
                            )
                    },
                ) {
                    Text(
                        text = historyVisibility.getStateName(i18n),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
                return
            }

            if (isHistoryVisibilityChanging) {
                ThemedProgressIndicator(
                    modifier =
                        Modifier.semantics {
                            focused = false
                            text = AnnotatedString(i18n.chatHistoryVisibilitySettings() + ", " + i18n.loading())
                        },
                    style = MaterialTheme.components.extraSmallCircularProgressIndicator,
                )
                return
            }

            RadioSetting(
                title = {
                    Tooltip(
                        tooltip = {
                            Text(
                                if (isEncrypted) historyVisibility.getExplanationWhenEncrypted(i18n)
                                else historyVisibility.getExplanation(i18n)
                            )
                        },
                        modifier =
                            Modifier.semantics {
                                text =
                                    AnnotatedString(
                                        i18n.chatHistoryVisibilitySettings() +
                                            ", " +
                                            historyVisibility.getStateName(i18n) +
                                            " " +
                                            i18n.selected()
                                    )
                            },
                    ) {
                        Text(historyVisibility.getStateName(i18n), style = MaterialTheme.typography.titleSmall)
                    }
                },
                options =
                    visibilities?.associate {
                        it to
                            RadioSettingOption(
                                text = it.getStateName(i18n),
                                explanation =
                                    if (isEncrypted) it.getExplanationWhenEncrypted(i18n) else it.getExplanation(i18n),
                                enabled =
                                    roomSettingsHistoryVisibilityViewModel.historyVisibilityCanBeChangedTo(it) &&
                                        isHistoryVisibilityChanging.not(),
                            )
                    } ?: mapOf(),
                set = { roomSettingsHistoryVisibilityViewModel.changeRoomHistoryVisibility(it) },
                value = historyVisibility,
            )
        }
    }
}
