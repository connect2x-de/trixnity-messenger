package de.connect2x.trixnity.messenger.compose.view.search

import androidx.compose.foundation.lazy.LazyListScope
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.UserSearchHandler

fun LazyListScope.searchUsersLocally(
    searchHandler: UserSearchHandler,
    onUserClick: (Search.SearchUserElement) -> Unit,
    searchResults: SearchResultState,
    userSearchResultListView: UserSearchResultListView,
) {
    stickyHeader(key = "UserSearchField") {
        UserSearchField(searchHandler)
    }
    userSearchResultListView.create(this, searchResults) {
        onUserClick(it)
    }
}
