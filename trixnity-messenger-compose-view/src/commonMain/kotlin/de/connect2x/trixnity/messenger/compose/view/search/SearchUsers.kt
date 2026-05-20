package de.connect2x.trixnity.messenger.compose.view.search

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.MutableState
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.UserSearchHandler

fun LazyListScope.searchUsersLocally(
    searchHandler: UserSearchHandler,
    onUserClick: (Search.SearchUserElement) -> Unit,
    searchResults: SearchResultState,
    userSearchResultListView: UserSearchResultListView,
    focusedItem: MutableState<String?>,
) {
    stickyHeader(key = "UserSearchField") {
        UserSearchField(searchHandler)
    }
    userSearchResultListView.create(this, searchResults, focusedItem) {
        onUserClick(it)
    }
}
