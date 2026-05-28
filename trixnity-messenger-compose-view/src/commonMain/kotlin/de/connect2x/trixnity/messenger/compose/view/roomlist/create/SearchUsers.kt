package de.connect2x.trixnity.messenger.compose.view.roomlist.create

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.search.SearchTerm
import de.connect2x.trixnity.messenger.compose.view.search.searchResults
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatNewSearchViewModel

@Composable
fun SearchUsers(createNewChatViewModel: CreateNewChatNewSearchViewModel) {
    val listState = rememberLazyListState()

    val searchResultList by createNewChatViewModel.searchUserViewModel.searchResultList.collectAsState()
    val noResultsFound by createNewChatViewModel.searchUserViewModel.noResultsFound.collectAsState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState) {
            item(key = "AddOrSearchGroup") {
                AddOrSearchGroup(createNewChatViewModel)
                HorizontalDivider(Modifier.fillMaxWidth().width(1.dp))
            }
            stickyHeader("searchTerm") { SearchTerm(createNewChatViewModel.searchUserViewModel) }
            searchResults(
                searchUserProviders = createNewChatViewModel.searchUserViewModel.searchUserProviders,
                onUserClick = createNewChatViewModel::onUserClick,
                searchResultList = searchResultList,
                noResultsFound = noResultsFound,
            )
        }
        VerticalScrollbar(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            lazyListState = listState,
            reverseLayout = false,
        )
    }
}
