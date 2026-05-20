package de.connect2x.trixnity.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.search.SearchResultState
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
        val userSearchResultList = DI.get<UserSearchResultListView>()
        val userSearchResults = collectUserSearchResult(potentialMembersViewModel.searchHandler)

        Box {
            val focusedItem = remember(userSearchResults) {
                mutableStateOf(
                    if (userSearchResults is SearchResultState.Results) {
                        userSearchResults.users.firstOrNull()?.userId?.full
                    } else {
                        null
                    }
                )
            }

            LazyColumn(
                Modifier.rovingFocusContainer(
                    listState = listState,
                    focusedItem = focusedItem
                ),
                listState
            ) {
                searchUsersLocally(
                    potentialMembersViewModel.searchHandler,
                    { onUserClick(it) },
                    userSearchResults,
                    userSearchResultList,
                    focusedItem
                )
            }
            VerticalScrollbar(Modifier.fillMaxHeight().align(Alignment.CenterEnd), listState, false)
        }
    }
}
