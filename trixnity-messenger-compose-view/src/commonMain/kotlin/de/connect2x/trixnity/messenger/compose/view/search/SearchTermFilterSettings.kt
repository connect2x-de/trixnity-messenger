package de.connect2x.trixnity.messenger.compose.view.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.VerySmallSpacer
import de.connect2x.trixnity.messenger.compose.view.common.modifier.focusHighlighting
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedInfoChip
import de.connect2x.trixnity.messenger.viewmodel.search.SearchUserViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchTermFilterSettings(searchUserViewModel: SearchUserViewModel) {
    val i18n = DI.get<I18nView>()

    val providerSettings by searchUserViewModel.providerSettingsList.collectAsState()

    var showFilters by remember { mutableStateOf(false) }
    val rotateState by animateFloatAsState(targetValue = if (showFilters) 180F else 0F)
    val interactionSource = remember { MutableInteractionSource() }

    if (
        searchUserViewModel.searchUserProviders.any { searchUserProvider -> searchUserProvider.settings.isNotEmpty() }
    ) {
        Card(
            modifier =
                Modifier.clickable(interactionSource, indication = null, onClick = { showFilters = showFilters.not() })
                    .focusHighlighting(interactionSource)
                    .buttonPointerModifier(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).buttonPointerModifier(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.FilterList, i18n.userSearchFilter())
                Spacer(Modifier.size(10.dp))
                val showChips = providerSettings.isNotEmpty() && showFilters.not()
                AnimatedVisibility(showChips.not()) {
                    Text(text = i18n.userSearchFilterOptions(), style = MaterialTheme.typography.titleSmall)
                }
                AnimatedVisibility(showChips, enter = fadeIn(), exit = fadeOut()) {
                    SmallSpacer()
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
                        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
                    ) {
                        providerSettings.forEach { providerSetting ->
                            ThemedInfoChip(
                                label = { Text(providerSetting, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }
                    SmallSpacer()
                }
                Box(Modifier.weight(1f, fill = true), contentAlignment = Alignment.CenterEnd) {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        i18n.userSearchSelectFilter(),
                        modifier = Modifier.rotate(rotateState),
                    )
                }
            }
            AnimatedVisibility(visible = showFilters) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    searchUserViewModel.providerSettings.values
                        .groupBy { searchSettingCombined -> searchSettingCombined.sourceDisplayNames }
                        .forEach { (sources, settings) ->
                            val settingsEnabled by settings[0].enabled.collectAsState()
                            Column(Modifier.padding(bottom = 10.dp)) {
                                if (settingsEnabled) {
                                    Text(sources.joinToString(), style = MaterialTheme.typography.bodyMediumEmphasized)
                                    VerySmallSpacer()
                                    settings.forEach { setting -> SearchSettingInputSelector(setting) }
                                } else {
                                    Tooltip(i18n.searchUserDisabledFilter()) {
                                        Text(
                                            sources.joinToString(),
                                            style =
                                                MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                                ),
                                        )
                                        SmallSpacer()
                                    }
                                }
                            }
                        }
                }
            }
        }
    }
}
