package de.connect2x.trixnity.messenger.compose.view.roomlist.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchResult
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProviderId

// FIXME make extensible?

fun LazyListScope.searchResults(
    searchUserProviders: List<SearchUserProvider>,
    onUserClick: (UserSearchResult) -> Unit,
    providerSearchActive: List<Boolean>,
    providerSearchSetActive: (SearchUserProviderId, Boolean) -> Unit,
    searchResultList: List<UserSearchResult>?,
) {
    if (searchResultList == null) {
        item("searchIn") {
            Text("Search in ... ") // FIXME every provider could contribute a location!
        }
    } else {
        searchOptions(searchUserProviders, providerSearchActive, providerSearchSetActive)
        searchResultList.forEachIndexed { index, searchResult ->
            item("${searchResult.id}-${index}") {
                Box(Modifier.padding(horizontal = 10.dp)) {
                    SearchResultSelector(
                        userSearchResult = searchResult,
                        showOrigin = searchUserProviders.size > 1,
                        onClick = { onUserClick(it) },
                    )
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
            Box(modifier = Modifier.padding(horizontal = 10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    searchUserProviders.forEachIndexed { index, searchUserProvider ->
                        SearchUserProviderToggleSelector(searchUserProvider, providerSearchActive[index]) {
                            providerSearchSetActive(searchUserProvider.providerId, providerSearchActive[index].not())
                        }
                    }
                }
            }
        }
    }
}
