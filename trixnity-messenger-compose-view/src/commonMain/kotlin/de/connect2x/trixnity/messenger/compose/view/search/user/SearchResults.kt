package de.connect2x.trixnity.messenger.compose.view.search.user

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.search.provider.SearchProvider
import de.connect2x.trixnity.messenger.search.user.UserSearchResult

fun LazyListScope.searchResults(
    searchProviders: List<SearchProvider<*, *>>,
    onUserClick: (UserSearchResult) -> Unit,
    searchResultList: List<UserSearchResult>,
    noResultsFound: Boolean?,
) {
    if (noResultsFound == true) {
        item(key = "NoResultsFound") {
            Box(
                Modifier.padding(horizontal = 10.dp, vertical = 20.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val i18n = DI.get<I18nView>()
                Text(i18n.searchUserNoResultsFound())
            }
        }
    } else {
        searchResultList.forEachIndexed { index, searchResult ->
            item("${searchResult.id}-${index}") {
                Box(Modifier.padding(horizontal = 10.dp)) {
                    SearchResultSelector(
                        userSearchResult = searchResult,
                        showOrigin = searchProviders.size > 1,
                        onClick = { onUserClick(it) },
                    )
                }
            }
        }
    }
}
