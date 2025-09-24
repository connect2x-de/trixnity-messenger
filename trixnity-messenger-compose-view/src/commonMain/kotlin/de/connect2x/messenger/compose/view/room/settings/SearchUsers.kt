package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.search.SearchResultState
import de.connect2x.messenger.compose.view.search.UserSearchResultListView
import de.connect2x.messenger.compose.view.search.collectUserSearchResult
import de.connect2x.messenger.compose.view.search.searchUsersLocally
import de.connect2x.messenger.compose.view.util.LocalRovingFocus
import de.connect2x.messenger.compose.view.util.RovingFocusContainer
import de.connect2x.messenger.compose.view.util.verticalRovingFocus
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
        val references = remember(userSearchResults) {
            derivedStateOf {
                (userSearchResults as? SearchResultState.Results)?.users?.map { it.userId.full }
                    ?.minus(selectedUsers.value.map { it.userId }.toSet())
            }
        }.value
        val defaultItem = references?.firstOrNull()

        Box {
            RovingFocusContainer {
                val focusContainer = LocalRovingFocus.current

                val focusModifier = if (references != null) Modifier.verticalRovingFocus(
                    default = defaultItem,
                    scroll = { item ->
                        val index = references.indexOf(item)
                        if (index != -1) {
                            listState.scrollToItem(index)
                        }
                    },
                    up = {
                        val currentItem = activeRef.value ?: defaultItem
                        val currentIndex = references.indexOf(currentItem)
                        val nextIndex = currentIndex.minus(1).coerceIn(references.indices)
                        references[nextIndex]
                    },
                    down = {
                        val currentItem = activeRef.value ?: defaultItem
                        val currentIndex = references.indexOf(currentItem)
                        val nextIndex = currentIndex.plus(1).coerceIn(references.indices)
                        references[nextIndex]
                    },
                ) else Modifier

                LazyColumn(
                    modifier = Modifier.then(focusModifier),
                    state = listState,
                ) {
                    searchUsersLocally(
                        potentialMembersViewModel.searchHandler,
                        {
                            onUserClick(it)
                            if (focusContainer != null) {
                                focusContainer.activeRef.value = null
                            }
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
