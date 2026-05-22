package de.connect2x.trixnity.messenger.compose.view.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchResult
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider

// FIXME make extensible?

fun LazyListScope.searchResults(
    searchUserProviders: List<SearchUserProvider>,
    onUserClick: (UserSearchResult) -> Unit,
    searchResultList: List<UserSearchResult>?,
) {
    if (searchResultList == null) {
        item("searchIn") {
            Text("Search in ... ") // FIXME every provider could contribute a location!
        }
    } else {
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
