package de.connect2x.trixnity.messenger.compose.view.room.settings.addmembers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusItem
import de.connect2x.trixnity.messenger.compose.view.search.user.SearchTerm
import de.connect2x.trixnity.messenger.compose.view.search.user.UsersInGroup
import de.connect2x.trixnity.messenger.compose.view.search.user.searchResults
import de.connect2x.trixnity.messenger.compose.view.theme.components.LocalSurfaceStyle
import de.connect2x.trixnity.messenger.viewmodel.room.settings.AddMembersNewSearchViewModel

@Composable
fun SearchUsers(addMembersNewSearchViewModel: AddMembersNewSearchViewModel, aboveSearch: @Composable () -> Unit) {
    val searchUserViewModel = addMembersNewSearchViewModel.userSearchViewModel
    val searchResultList by searchUserViewModel.searchResultList.collectAsState()
    val noResultsFound by searchUserViewModel.noResultsFound.collectAsState()

    val listState = rememberLazyListState()
    val focusedItem: MutableState<String?> = remember { mutableStateOf(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize()
                .rovingFocusContainer(
                    listState = listState,
                    focusedItem = focusedItem,
                    ignoredKeys = listOf("aboveSearch"),
                ),
            listState,
        ) {
            item("aboveSearch") { aboveSearch() }
            stickyHeader("searchTerm") {
                val containerColor = LocalSurfaceStyle.current?.color
                Column(
                    (if (containerColor != null) Modifier.background(containerColor) else Modifier).then(
                        Modifier.rovingFocusItem( // FIXME Make sure every item that should be focusable has this
                            { focusedItem.value == "searchTerm" },
                            { focusedItem.value = "searchTerm" },
                        )
                    )
                ) {
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
                focusedItem = focusedItem,
            )
        }
        VerticalScrollbar(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            lazyListState = listState,
            reverseLayout = false,
        )
    }
}
