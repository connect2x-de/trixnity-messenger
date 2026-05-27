package de.connect2x.trixnity.messenger.compose.view.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.search.SearchUserViewModel

@Composable
fun SearchUserProviderToggles(searchUserViewModel: SearchUserViewModel) {
    val i18n = DI.get<I18nView>()
    val providerSearchEnabled by searchUserViewModel.providerSearchEnabled.collectAsState()
    val providerSearchCanBeEnabled by searchUserViewModel.providerSearchCanBeEnabled.collectAsState()

    if (searchUserViewModel.searchUserProviders.size > 1) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp), // FilterChips have a minimum height
        ) {
            searchUserViewModel.searchUserProviders.forEachIndexed { index, searchUserProvider ->
                Tooltip(i18n.searchUserProviderDeactivated(), enabled = providerSearchCanBeEnabled[index].not()) {
                    Box(Modifier.buttonPointerModifier(enabled = providerSearchEnabled[index])) {
                        SearchUserProviderToggleSelector(
                            searchUserProvider,
                            providerSearchCanBeEnabled[index],
                            providerSearchEnabled[index],
                        ) {
                            searchUserViewModel.setProvider(
                                searchUserProvider.providerId,
                                providerSearchEnabled[index].not(),
                            )
                        }
                    }
                }
            }
        }
    }
}
