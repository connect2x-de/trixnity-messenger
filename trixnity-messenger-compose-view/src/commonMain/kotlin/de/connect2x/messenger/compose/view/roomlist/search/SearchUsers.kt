package de.connect2x.messenger.compose.view.roomlist.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
        shouldScroll: Boolean,
        onUserClick: (Search.SearchUserElement) -> Unit,
    )
}

@Composable
fun SearchUsers(
    createNewRoomViewModel: CreateNewRoomViewModel,
    shouldScroll: Boolean = true,
    onUserClick: (Search.SearchUserElement) -> Unit,
) {
    DI.get<SearchUsersView>().create(createNewRoomViewModel, shouldScroll, onUserClick)
}

class SearchUsersViewImpl : SearchUsersView {
    @Composable
    override fun create(
        createNewRoomViewModel: CreateNewRoomViewModel,
        shouldScroll: Boolean,
        onUserClick: (Search.SearchUserElement) -> Unit,
    ) {
        LazyColumn {
            SearchUsersLocally(createNewRoomViewModel.searchHandler, false, onUserClick)
        }
    }
}
