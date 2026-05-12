package de.connect2x.trixnity.messenger.compose.view.roomlist.search

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider

@Composable
fun SearchUserProviderSettings(
    searchUserProviders: List<SearchUserProvider>,
    onDismiss: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val tabIndex = remember { mutableStateOf(0) }

    ThemedModalDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.heightIn(min = 200.dp),
    ) {
        ModalDialogHeader {
            Text("Search Settings") // FIXME i18n
        }
        ModalDialogContent {
            val searchUserProvidersWithFilters =
                searchUserProviders.filter { searchUserProvider -> searchUserProvider.settings.isNotEmpty() }
            if (searchUserProvidersWithFilters.size > 1) {
                TabRow(tabIndex.value) {
                    searchUserProvidersWithFilters.mapIndexed { index, searchUserProvider ->
                        Tab(
                            selected = tabIndex.value == index,
                            onClick = { tabIndex.value = index },
                            text = { Text(searchUserProvider.providerDisplayName) },
                        )
                    }
                }
            }
            searchUserProvidersWithFilters.getOrNull(tabIndex.value)?.let { searchUserProvider ->
                SearchUserProviderSettingsSelector(searchUserProvider)
            }
        }
        ModalDialogFooter {
            ThemedButton(
                style = MaterialTheme.components.commonButton,
                onClick = { onDismiss() },
            ) {
                Text(i18n.actionCancel())
            }
            ThemedButton(
                onClick = {
                    searchUserProviders[tabIndex.value].applySettings()
                    onDismiss()
                },
                style = MaterialTheme.components.primaryButton,
            ) {
                Text("Apply") // FIXME i18n
            }
        }
    }
}
