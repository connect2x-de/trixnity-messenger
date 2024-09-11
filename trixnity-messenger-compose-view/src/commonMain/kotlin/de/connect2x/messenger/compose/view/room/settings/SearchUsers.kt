package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.search.SearchUsersLocally
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PotentialMembersViewModel

interface SearchUsersView {
    @Composable
    fun create(
        potentialMembersViewModel: PotentialMembersViewModel,
        onUserClick: suspend (Search.SearchUserElement) -> Unit,
    )
}

@Composable
fun SearchUsers(
    potentialMembersViewModel: PotentialMembersViewModel,
    onUserClick: suspend (Search.SearchUserElement) -> Unit,
) {
    DI.get<SearchUsersView>().create(potentialMembersViewModel, onUserClick)
}

class SearchUsersViewImpl : SearchUsersView {
    @Composable
    override fun create(
        potentialMembersViewModel: PotentialMembersViewModel,
        onUserClick: suspend (Search.SearchUserElement) -> Unit,
    ) {
        Column {
            SearchUsersLocally(potentialMembersViewModel.searchHandler, onUserClick)
        }
    }
}
