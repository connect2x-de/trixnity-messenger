package de.connect2x.trixnity.messenger.compose.view.search.user

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
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchViewModel

@Composable
fun SearchProviderToggles(userSearchViewModel: UserSearchViewModel) {
    val i18n = DI.get<I18nView>()
    val providerSearchEnabled by userSearchViewModel.searchProviderEnabled.collectAsState()
    val providerSearchCanBeEnabled by userSearchViewModel.searchProviderCanBeEnabled.collectAsState()

    if (userSearchViewModel.searchProviders.size > 1) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp), // FilterChips have a minimum height
        ) {
            userSearchViewModel.searchProviders.forEachIndexed { index, searchUserProvider ->
                Tooltip(i18n.searchUserProviderDeactivated(), enabled = providerSearchCanBeEnabled[index].not()) {
                    Box(Modifier.buttonPointerModifier(enabled = providerSearchEnabled[index])) {
                        SearchProviderToggleSelector(
                            searchUserProvider,
                            providerSearchCanBeEnabled[index],
                            providerSearchEnabled[index],
                        ) {
                            userSearchViewModel.setProvider(searchUserProvider.key, providerSearchEnabled[index].not())
                        }
                    }
                }
            }
        }
    }
}
