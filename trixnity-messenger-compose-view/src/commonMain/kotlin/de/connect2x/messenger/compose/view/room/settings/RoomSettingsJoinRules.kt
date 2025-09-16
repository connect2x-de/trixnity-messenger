package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.RadioSetting
import de.connect2x.messenger.compose.view.common.RadioSettingOption
import de.connect2x.messenger.compose.view.common.Tooltip
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.i18n.getExplanation
import de.connect2x.messenger.compose.view.i18n.getStateName
import de.connect2x.messenger.compose.view.theme.components
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
            Text(i18n.chatJoinRule(), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(10.dp))
            if (!canChangeJoinRule.value) {
                Tooltip(tooltip = { TooltipText { currentJoinRule.getExplanation(i18n) } }) {
                    Text(
                        text = currentJoinRule.getStateName(i18n),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else {
                RadioSetting(
                    title = {
                        if (joinRuleIsChanging) {
                            ThemedProgressIndicator(style = MaterialTheme.components.extraSmallCircularProgressIndicator)
                        } else {
                            Tooltip(tooltip = { TooltipText { currentJoinRule.getExplanation(i18n) } }) {
                                Text(currentJoinRule.getStateName(i18n), style = MaterialTheme.typography.titleSmall)
                            }
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
}
