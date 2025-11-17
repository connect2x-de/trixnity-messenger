package de.connect2x.messenger.compose.view.room.settings

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
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.RadioSetting
import de.connect2x.messenger.compose.view.common.RadioSettingOption
import de.connect2x.messenger.compose.view.common.Tooltip
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.i18n.getExplanation
import de.connect2x.messenger.compose.view.i18n.getStateName
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedListItem
import de.connect2x.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel

interface RoomSettingsJoinRulesView {
    @Composable
    fun create(roomSettingsViewModel: RoomSettingsViewModel)
}

@Composable
fun RoomSettingsJoinRules(roomSettingsViewModel: RoomSettingsViewModel) {
    DI.get<RoomSettingsJoinRulesView>().create(roomSettingsViewModel)
}

class RoomSettingsJoinRulesViewImpl : RoomSettingsJoinRulesView {
    @Composable
    override fun create(roomSettingsViewModel: RoomSettingsViewModel) {
        val canChangeJoinRule = roomSettingsViewModel.roomSettingsJoinRulesViewModel.canChangeJoinRule.collectAsState()
        val currentJoinRule = roomSettingsViewModel.roomSettingsJoinRulesViewModel.joinRule.collectAsState().value
        val joinRuleIsChanging =
            roomSettingsViewModel.roomSettingsJoinRulesViewModel.isJoinRuleChanging.collectAsState().value
        val joinRules =
            roomSettingsViewModel.roomSettingsJoinRulesViewModel.availableRoomJoinStates.collectAsState().value
        val i18n = DI.get<I18nView>()

        Column {
            ThemedListItem(
                headlineContent = {
                    Text(i18n.chatJoinRule(), style = MaterialTheme.typography.titleMedium)
                },
                style = MaterialTheme.components.settingsItem,
            )

            if (!canChangeJoinRule.value) {
                Tooltip(
                    tooltip = { Text(currentJoinRule.getExplanation(i18n)) },
                    Modifier.semantics {
                        text = AnnotatedString(i18n.chatJoinRuleSettings() + ", " + currentJoinRule.getStateName(i18n))
                    }
                ) {
                    Text(
                        text = currentJoinRule.getStateName(i18n),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                return
            }

            if (joinRuleIsChanging) {
                ThemedProgressIndicator(
                    modifier = Modifier.semantics {
                        focused = false
                        text = AnnotatedString(i18n.chatJoinRuleSettings() + ", " + i18n.loading())
                    }, style = MaterialTheme.components.extraSmallCircularProgressIndicator
                )
                return
            }

            RadioSetting(
                title = {
                    Tooltip(
                        tooltip = { Text(currentJoinRule.getExplanation(i18n)) },
                        Modifier.semantics {
                            text =
                                AnnotatedString(i18n.chatJoinRuleSettings() + ", " + currentJoinRule.getStateName(i18n) + " " + i18n.selected())
                        }) {
                        Text(currentJoinRule.getStateName(i18n), style = MaterialTheme.typography.titleSmall)
                    }
                },
                options = joinRules?.associate {
                    it to RadioSettingOption(
                        text = it.getStateName(i18n),
                        explanation = it.getExplanation(i18n),
                        enabled = joinRuleIsChanging.not()
                    )
                } ?: mapOf(),
                value = currentJoinRule,
                set = { roomSettingsViewModel.roomSettingsJoinRulesViewModel.changeJoinRule(it) }
            )
        }
    }

}
