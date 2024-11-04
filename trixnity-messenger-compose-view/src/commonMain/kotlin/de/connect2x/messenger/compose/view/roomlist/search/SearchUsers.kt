package de.connect2x.messenger.compose.view.roomlist.search

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.search.SearchUsersLocally
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewRoomViewModel

interface SearchUsersView {
    @Composable
    fun create(
        createNewRoomViewModel: CreateNewRoomViewModel,
        onUserClick: suspend (Search.SearchUserElement) -> Unit,
    )
}

@Composable
fun SearchUsers(
    createNewRoomViewModel: CreateNewRoomViewModel,
    onUserClick: suspend (Search.SearchUserElement) -> Unit,
) {
    DI.get<SearchUsersView>().create(createNewRoomViewModel, onUserClick)
}

class SearchUsersViewImpl : SearchUsersView {
    @Composable
    override fun create(
        createNewRoomViewModel: CreateNewRoomViewModel,
        onUserClick: suspend (Search.SearchUserElement) -> Unit,
    ) {
        Column {
            SearchUsersLocally(createNewRoomViewModel.searchHandler, onUserClick)
        }
    }
}
