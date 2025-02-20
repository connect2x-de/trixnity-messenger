package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.MoreOptions
import de.connect2x.messenger.compose.view.common.TooltipIconButton
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.common.gesturesDisabled
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsAliasViewModel
import net.folivo.trixnity.core.MatrixRegex
import net.folivo.trixnity.core.model.RoomAliasId


@Composable
fun RoomSettingsAlias(viewModel: RoomSettingsAliasViewModel) {
    val i18n = DI.current.get<I18nView>()
    val mainAlias = viewModel.mainAlias.collectAsState().value
    val moreAliases = viewModel.moreAliases.collectAsState().value
    var newAlias by viewModel.newAlias.collectAsTextFieldValueState()
    val isUpdating = viewModel.isUpdating.collectAsState().value
    val canChangeRoomAlias = viewModel.canChangeRoomAlias.collectAsState().value

    Column {
        Text(text = i18n.aliases().capitalize(Locale.current), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(10.dp))
        MoreOptions(if (canChangeRoomAlias) i18n.manageAliases() else i18n.showAliases()) {
            Box(Modifier.fillMaxSize()) {
                Column {
                    if (canChangeRoomAlias) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newAlias,
                                placeholder = { Text(i18n.newAlias()) },
                                onValueChange = {
                                    if (it.text.isEmpty() || MatrixRegex.roomAlias.matchEntire("#${it.text}:${viewModel.domain}") != null) {
                                        newAlias = it
                                    }
                                },
                                label = { Text(i18n.newAlias()) },
                                modifier = Modifier.weight(1.0f, fill = true).fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.surfaceTint,
                                ),
                                prefix = { Text("#") },
                                suffix = { Text(":${viewModel.domain}") }
                            )
                            TooltipIconButton(
                                i18n.addAlias(),
                                onClick = {
                                    viewModel.addNewAlias(onlyLocalpart = true)
                                },
                            ) {
                                Icon(Icons.Default.Add, i18n.addAlias())
                            }
                        }
                        Spacer(Modifier.size(10.dp))
                    }
                    mainAlias?.let { alias ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically
                        ) {
                            Text(alias, modifier = Modifier.weight(1f, false))
                            Row {
                                if (canChangeRoomAlias) {
                                    TooltipIconButton(
                                        i18n.unmakeMainAlias(),
                                        onClick = {
                                            viewModel.changeMainAlias(null)
                                        },
                                    ) {
                                        Icon(Icons.Default.Star, i18n.unmakeMainAlias())
                                    }
                                    TooltipIconButton(
                                        i18n.deleteAlias(),
                                        onClick = {
                                            viewModel.removeMainAlias(RoomAliasId(alias))
                                        },
                                    ) {
                                        Icon(Icons.Default.Delete, i18n.deleteAlias())
                                    }
                                } else {
                                    Box(
                                        Modifier
                                            .minimumInteractiveComponentSize()
                                            .size(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Tooltip(
                                            tooltip = { TooltipText(i18n.mainAlias()) }
                                        ) {
                                            Icon(Icons.Default.Star, i18n.mainAlias())
                                        }
                                    }
                                }
                            }
                        }
                    }
                    moreAliases.forEach { alias ->
                        Spacer(Modifier.size(10.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically
                        ) {
                            Text(alias, modifier = Modifier.weight(1f, false))
                            Row {
                                if (canChangeRoomAlias) {
                                    TooltipIconButton(
                                        i18n.makeMainAlias(),
                                        onClick = {
                                            viewModel.changeMainAlias(RoomAliasId(alias))
                                        },
                                    ) {
                                        Icon(Icons.Default.StarOutline, i18n.makeMainAlias())
                                    }
                                    TooltipIconButton(
                                        i18n.deleteAlias(),
                                        onClick = {
                                            viewModel.removeAlias(RoomAliasId(alias))
                                        },
                                    ) {
                                        Icon(Icons.Default.Delete, i18n.deleteAlias())
                                    }
                                } else {
                                    Box(
                                        Modifier
                                            .minimumInteractiveComponentSize()
                                            .size(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Tooltip(
                                            tooltip = { TooltipText(i18n.mainAlias()) }
                                        ) {
                                            Icon(Icons.Default.StarOutline, i18n.alias())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (isUpdating) {
                    Box(Modifier.fillMaxSize().gesturesDisabled(), contentAlignment = Alignment.Center) {
                        LoadingSpinner()
                    }
                }
            }
        }
    }
}
