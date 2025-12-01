package de.connect2x.messenger.compose.view.roomlist.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatNewSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatViewModel
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchResult
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProviderId

// FIXME make extensible?

fun LazyListScope.searchResults(
    searchUserProviders: List<SearchUserProvider>,
    createNewChatViewModel: CreateNewChatViewModel,
    providerSearchActive: List<Boolean>,
    providerSearchSetActive: (SearchUserProviderId, Boolean) -> Unit,
    searchResultList: List<UserSearchResult>?,
) {
    if (createNewChatViewModel is CreateNewChatNewSearchViewModel) {
        if (searchResultList == null) {
            item("searchIn") {
                Text("Search in ... ") // FIXME every provider could contribute a location!
            }
        } else {
            searchOptions(searchUserProviders, providerSearchActive, providerSearchSetActive)
            searchResultList.forEachIndexed { index, searchResult ->
                item("${searchResult.id}-${index}") {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        SearchResultSelector(
                            userSearchResult = searchResult,
                            showOrigin = searchUserProviders.size > 1,
                            onClick = {
                                createNewChatViewModel.onUserClick(it)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.searchOptions(
    searchUserProviders: List<SearchUserProvider>,
    providerSearchActive: List<Boolean>,
    providerSearchSetActive: (SearchUserProviderId, Boolean) -> Unit,
) {
    if (searchUserProviders.size > 1) {
        item("searchOptions") {
            val searchUserProviderSettings = remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f, fill = true),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    searchUserProviders.forEachIndexed { index, searchUserProvider ->
                        SearchUserProviderToggleSelector(searchUserProvider, providerSearchActive[index]) {
                            providerSearchSetActive(searchUserProvider.providerId, providerSearchActive[index].not())
                        }
                    }
                }
                if (searchUserProviders.any { searchUserProvider -> searchUserProvider.settings.isNotEmpty() }) {
                    ThemedIconButton(onClick = { searchUserProviderSettings.value = true }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            }

            if (searchUserProviderSettings.value) {
                SearchUserProviderSettings(searchUserProviders) { searchUserProviderSettings.value = false }
            }
        }
    }
}
