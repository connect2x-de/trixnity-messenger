package de.connect2x.trixnity.messenger.compose.view.search

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.focus.FocusRequester
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.UserSearchHandler

fun LazyListScope.searchUsersLocally(
    searchHandler: UserSearchHandler,
    onUserClick: (Search.SearchUserElement) -> Unit,
    searchResults: SearchResultState,
    userSearchResultListView: UserSearchResultListView,
    singletonFocusRequester: FocusRequester
) {
    stickyHeader(key = "UserSearchField") {
        UserSearchField(searchHandler)
    }
    userSearchResultListView.create(this, searchResults, singletonFocusRequester) {
        onUserClick(it)
    }
}
