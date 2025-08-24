package de.connect2x.messenger.compose.view.roomlist.search

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.search.SearchUsersLocally
import de.connect2x.messenger.compose.view.search.UserSearchResultListView
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewRoomViewModel

interface SearchUsersView {
    fun create(
        createNewRoomViewModel: CreateNewRoomViewModel,
        onUserClick: (Search.SearchUserElement) -> Unit,
        userSearch: UserSearchResultListView,
        userSearchResults: UserSearchResultListView.SearchResultState,
        scope: LazyListScope
    )
}

@Composable
fun SearchUsers(
    createNewRoomViewModel: CreateNewRoomViewModel,
    onUserClick: (Search.SearchUserElement) -> Unit,
    userSearch: UserSearchResultListView,
    userSearchResults: UserSearchResultListView.SearchResultState,
    scope: LazyListScope
) {
    DI.get<SearchUsersView>().create(createNewRoomViewModel, onUserClick, userSearch, userSearchResults, scope)
}

class SearchUsersViewImpl : SearchUsersView {
    override fun create(
        createNewRoomViewModel: CreateNewRoomViewModel,
        onUserClick: (Search.SearchUserElement) -> Unit,
        userSearch: UserSearchResultListView,
        userSearchResults: UserSearchResultListView.SearchResultState,
        scope: LazyListScope
    ) {
        with(scope) {
            SearchUsersLocally(
                createNewRoomViewModel.searchHandler,
                onUserClick,
                userSearch,
                userSearchResults
            )
        }
    }
}
