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
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.i18n.getExplanation
import de.connect2x.messenger.compose.view.i18n.getStateName
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsJoinRulesViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent

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
        val currentJoinRule = roomSettingsViewModel.roomSettingsJoinRulesViewModel.joinRule.collectAsState()
        val joinRuleIsChanging =
            roomSettingsViewModel.roomSettingsJoinRulesViewModel.isJoinRuleChanging.collectAsState()
        val i18n = DI.get<I18nView>()

        Column {
            Text(i18n.chatJoinRule(), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(10.dp))
            if (!canChangeJoinRule.value) {
                Tooltip(tooltip = { TooltipText { currentJoinRule.value.getExplanation(i18n) } }) {
                    Text(
                        text = currentJoinRule.value.getStateName(i18n),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else {
                MoreOptions(
                    title = {
                        if (joinRuleIsChanging.value) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Tooltip(tooltip = { TooltipText { currentJoinRule.value.getExplanation(i18n) } }) {
                                Text(currentJoinRule.value.getStateName(i18n))
                            }
                        }
                    },
                    content = { JoinRuleOptions(roomSettingsViewModel.roomSettingsJoinRulesViewModel) }
                )
            }
        }
    }
}

@Composable
fun JoinRuleOption(
    joinRule: JoinRulesEventContent.JoinRule,
    joinRulesViewModel: RoomSettingsJoinRulesViewModel
) {
    val i18n = DI.get<I18nView>()
    val currentJoinRule = joinRulesViewModel.joinRule.collectAsState().value
    val joinRuleIsChanging =
        joinRulesViewModel.isJoinRuleChanging.collectAsState().value
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .clickable {
                if (!joinRuleIsChanging) {
                    joinRulesViewModel.changeJoinRule(joinRule)
                }
            }
            .buttonPointerModifier(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HelpIcon(joinRule.getExplanation(i18n))
        Text(joinRule.getStateName(i18n), modifier = Modifier.weight(1.0f, fill = true))
        RadioButton(
            selected = currentJoinRule == joinRule,
            onClick = { joinRulesViewModel.changeJoinRule(joinRule) },
            enabled = !joinRuleIsChanging,
        )
    }
}

@Composable
fun ColumnScope.JoinRuleOptions(joinRulesViewModel: RoomSettingsJoinRulesViewModel) {
    val availableJoinRules = joinRulesViewModel.availableRoomJoinStates.collectAsState().value
    if (availableJoinRules != null) {
        for (joinRule in availableJoinRules) {
            JoinRuleOption(joinRule, joinRulesViewModel)
        }
    }
}
