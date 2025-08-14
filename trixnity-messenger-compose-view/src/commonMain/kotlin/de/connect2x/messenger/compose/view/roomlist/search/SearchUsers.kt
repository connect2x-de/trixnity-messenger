package de.connect2x.messenger.compose.view.roomlist.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.search.SearchUsersLocally
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewRoomViewModel

interface SearchUsersView {
    @Composable
    fun create(
        createNewRoomViewModel: CreateNewRoomViewModel,
        onUserClick: (Search.SearchUserElement) -> Unit,
    )
}

@Composable
fun SearchUsers(
    createNewRoomViewModel: CreateNewRoomViewModel,
    onUserClick: (Search.SearchUserElement) -> Unit,
) {
    DI.get<SearchUsersView>().create(createNewRoomViewModel, onUserClick)
}

class SearchUsersViewImpl : SearchUsersView {
    @Composable
    override fun create(
        createNewRoomViewModel: CreateNewRoomViewModel,
        onUserClick: (Search.SearchUserElement) -> Unit,
    ) {
        val listState = rememberLazyListState()
        Box {
            LazyColumn(state = listState) {
                SearchUsersLocally(createNewRoomViewModel.searchHandler, onUserClick)
            }
            VerticalScrollbar(Modifier.fillMaxHeight().align(Alignment.CenterEnd), listState, false)
        }
    }
}
