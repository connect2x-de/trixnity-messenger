package de.connect2x.trixnity.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.search.UserSearchResultListView
import de.connect2x.trixnity.messenger.compose.view.search.collectUserSearchResult
import de.connect2x.trixnity.messenger.compose.view.search.searchUsersLocally
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PotentialMembersViewModel

interface SearchUsersSettingsView {
    @Composable
    fun create(
        potentialMembersViewModel: PotentialMembersViewModel,
        onUserClick: (Search.SearchUserElement) -> Unit,
    )
}

@Composable
fun SearchUsersSettings(
    potentialMembersViewModel: PotentialMembersViewModel,
    onUserClick: (Search.SearchUserElement) -> Unit,
) {
    DI.get<SearchUsersSettingsView>().create(potentialMembersViewModel, onUserClick)
}

class SearchUsersSettingsViewImpl : SearchUsersSettingsView {
    @Composable
    override fun create(
        potentialMembersViewModel: PotentialMembersViewModel,
        onUserClick: (Search.SearchUserElement) -> Unit,
    ) {
        val listState = rememberLazyListState()
        val singletonFocusRequester: FocusRequester = remember { FocusRequester() }
        val coroutineScope = rememberCoroutineScope()
        val userSearchResultList = DI.get<UserSearchResultListView>()
        val userSearchResults = collectUserSearchResult(potentialMembersViewModel.searchHandler)

        Box {
            LazyColumn(
                Modifier.rovingFocusContainer(
                    coroutineScope = coroutineScope,
                    singletonFocusRequester = singletonFocusRequester,
                    isFocusedItemVisible = { false },
                    scrollToFocusedItem = { listState.scrollToItem(0) }),
                listState
            ) {
                searchUsersLocally(
                    potentialMembersViewModel.searchHandler,
                    { onUserClick(it) },
                    userSearchResults,
                    userSearchResultList,
                    singletonFocusRequester
                )
            }
            VerticalScrollbar(Modifier.fillMaxHeight().align(Alignment.CenterEnd), listState, false)
        }
    }
}
