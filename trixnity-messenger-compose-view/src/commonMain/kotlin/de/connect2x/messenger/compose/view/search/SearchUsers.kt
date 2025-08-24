package de.connect2x.messenger.compose.view.search

import androidx.compose.foundation.lazy.LazyListScope
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.UserSearchHandler

fun LazyListScope.SearchUsersLocally(searchHandler: UserSearchHandler, onUserClick: (Search.SearchUserElement) -> Unit, userSearch: UserSearchResultListView, searchResults: UserSearchResultListView.SearchResultState) {
    stickyHeader(key = "UserSearchField") {
        UserSearchField(searchHandler)
    }
    userSearch.createLazyComposables(this, searchResults) {
        onUserClick(it)
    }
}
