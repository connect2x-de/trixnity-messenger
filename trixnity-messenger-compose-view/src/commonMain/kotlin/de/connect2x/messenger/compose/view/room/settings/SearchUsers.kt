package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.LazyRovingFocusColumn
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.search.SearchResultState
import de.connect2x.messenger.compose.view.search.UserSearchResultListView
import de.connect2x.messenger.compose.view.search.collectUserSearchResult
import de.connect2x.messenger.compose.view.search.searchUsersLocally
import de.connect2x.messenger.compose.view.util.LocalRovingFocus
import de.connect2x.messenger.compose.view.util.RovingFocusContainer
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
        val selectedUsers = potentialMembersViewModel.searchHandler.selectedUsers.collectAsState()
        var references by remember { mutableStateOf(listOf<String>()) }
        LaunchedEffect(userSearchResults) {
            if (userSearchResults is SearchResultState.Results) {
                references = userSearchResults.users.map { it.userId.full }
                    .minus(selectedUsers.value.map { it.userId.full }.toSet())
            }
        }
        val defaultItem = references.firstOrNull()


        Box {
            RovingFocusContainer {
                val focusContainer = LocalRovingFocus.current

                LazyRovingFocusColumn(defaultItem, references, listState, focusContainer) {
                    searchUsersLocally(
                        potentialMembersViewModel.searchHandler,
                        {
                            onUserClick(it)
                        },
                        userSearchResults,
                        userSearchResultList,
                    )
                }
            }
            VerticalScrollbar(Modifier.fillMaxHeight().align(Alignment.CenterEnd), listState, false)
        }
    }
}
