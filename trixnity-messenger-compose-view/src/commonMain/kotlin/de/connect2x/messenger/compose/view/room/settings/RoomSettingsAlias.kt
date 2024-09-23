package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.MoreOptions
import de.connect2x.messenger.compose.view.common.TooltipIconButton
import de.connect2x.messenger.compose.view.common.collectAsStateForTextField
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsAliasViewModel
import net.folivo.trixnity.core.MatrixRegex
import net.folivo.trixnity.core.model.RoomAliasId


@Composable
fun RoomSettingsAlias(viewModel: RoomSettingsAliasViewModel) {
    val i18n = DI.current.get<I18nView>()
    val mainAlias = viewModel.mainAlias.collectAsState()
    val moreAliases = viewModel.moreAliases.collectAsState()
    val newAlias = viewModel.newAlias.collectAsStateForTextField()

    Column {
        Text(text = i18n.aliases(), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(10.dp))
        MoreOptions(i18n.manageAliases()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newAlias.value,
                    placeholder = { Text(i18n.newAlias()) },
                    onValueChange = {
                        if (it.isEmpty() || MatrixRegex.roomAlias.matchEntire("#$it:${viewModel.domain}") != null) {
                            viewModel.newAlias.value = it
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
            mainAlias.value?.let { alias ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    Text(alias, modifier = Modifier.weight(1f, false))
                    Row {
                        TooltipIconButton(
                            i18n.unmakeMainAlias(),
                            onClick = {
                                viewModel.changeMainAlias(null)
                            },
                        ) {
                            Icon(Icons.Default.Close, i18n.unmakeMainAlias())
                        }
                        TooltipIconButton(
                            i18n.deleteAlias(),
                            onClick = {
                                viewModel.changeMainAlias(null)
                                while (viewModel.isUpdating.value) {
                                }
                                viewModel.removeAlias(RoomAliasId(alias))
                            },
                        ) {
                            Icon(Icons.Default.Delete, i18n.deleteAlias())
                        }
                    }
                }
            }
            moreAliases.value.forEach { alias ->
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
                        TooltipIconButton(
                            i18n.makeMainAlias(),
                            onClick = {
                                viewModel.changeMainAlias(RoomAliasId(alias))
                            },
                        ) {
                            Icon(Icons.Default.Add, i18n.makeMainAlias())
                        }
                        TooltipIconButton(
                            i18n.deleteAlias(),
                            onClick = {
                                viewModel.removeAlias(RoomAliasId(alias))
                            },
                        ) {
                            Icon(Icons.Default.Delete, i18n.deleteAlias())
                        }
                    }
                }
            }
        }
    }
}
