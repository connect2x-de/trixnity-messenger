package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.search.UserSearchResultListView
import de.connect2x.messenger.compose.view.search.collectUserSearchResult
import de.connect2x.messenger.compose.view.search.searchUsersLocally
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
        val userSearchResultList = DI.get<UserSearchResultListView>()
        val userSearchResults = collectUserSearchResult(potentialMembersViewModel.searchHandler)

        Box {
            LazyColumn(Modifier.rovingFocusContainer(), listState) {
                searchUsersLocally(
                    potentialMembersViewModel.searchHandler,
                    { onUserClick(it) },
                    userSearchResults,
                    userSearchResultList,
                )
            }
            VerticalScrollbar(Modifier.fillMaxHeight().align(Alignment.CenterEnd), listState, false)
        }
    }
}
