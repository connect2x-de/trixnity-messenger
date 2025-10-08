package de.connect2x.trixnity.messenger.compose.view.roomlist.search

import androidx.compose.foundation.lazy.LazyListScope
import de.connect2x.trixnity.messenger.compose.view.search.SearchResultState
import de.connect2x.trixnity.messenger.compose.view.search.UserSearchResultListView
import de.connect2x.trixnity.messenger.compose.view.search.searchUsersLocally
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewRoomViewModel

interface SearchUsersView {
    // this function is no @Composable as it is used inside a LazyListScope
    fun create(
        createNewRoomViewModel: CreateNewRoomViewModel,
        onUserClick: (Search.SearchUserElement) -> Unit,
        userSearchResults: SearchResultState,
        userSearchResultListView: UserSearchResultListView,
        scope: LazyListScope
    )
}

class SearchUsersViewImpl : SearchUsersView {
    override fun create(
        createNewRoomViewModel: CreateNewRoomViewModel,
        onUserClick: (Search.SearchUserElement) -> Unit,
        userSearchResults: SearchResultState,
        userSearchResultListView: UserSearchResultListView,
        scope: LazyListScope,
    ) {
        with(scope) {
            searchUsersLocally(
                createNewRoomViewModel.searchHandler,
                onUserClick,
                userSearchResults,
                userSearchResultListView,
            )
        }
    }
}
