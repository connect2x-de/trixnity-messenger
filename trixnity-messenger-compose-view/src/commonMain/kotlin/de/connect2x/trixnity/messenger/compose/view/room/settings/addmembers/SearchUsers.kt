package de.connect2x.trixnity.messenger.compose.view.room.settings.addmembers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.LocalSurfaceStyle
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.search.SearchTerm
import de.connect2x.trixnity.messenger.compose.view.search.UsersInGroup
import de.connect2x.trixnity.messenger.compose.view.search.searchResults
import de.connect2x.trixnity.messenger.viewmodel.room.settings.AddMembersNewSearchViewModel

@Composable
fun SearchUsers(addMembersNewSearchViewModel: AddMembersNewSearchViewModel, aboveSearch: @Composable () -> Unit) {
    val searchUserViewModel = addMembersNewSearchViewModel.potentialMembersNewSearchViewModel.userSearchViewModel
    val searchResultList by searchUserViewModel.searchResultList.collectAsState()
    val noResultsFound by searchUserViewModel.noResultsFound.collectAsState()

    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().rovingFocusContainer(), listState) {
            item("aboveSearch") { aboveSearch() }
            stickyHeader("searchTerm") {
                val containerColor = LocalSurfaceStyle.current?.color
                Column(if (containerColor != null) Modifier.background(containerColor) else Modifier) {
                    UsersInGroup(addMembersNewSearchViewModel.groupUsersNewSearch) {
                        addMembersNewSearchViewModel.removeUserFromGroup(it)
                    }
                    SearchTerm(searchUserViewModel)
                }
            }
            searchResults(
                searchProviders = searchUserViewModel.searchProviders,
                onUserClick = addMembersNewSearchViewModel::onUserClick,
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
