package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.search.SearchUsersLocally
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PotentialMembersViewModel

interface SearchUsersSettingsView {
    @Composable
    fun create(
        potentialMembersViewModel: PotentialMembersViewModel,
        shouldScroll: Boolean,
        onUserClick: (Search.SearchUserElement) -> Unit,
    )
}

@Composable
fun SearchUsersSettings(
    potentialMembersViewModel: PotentialMembersViewModel,
    shouldScroll: Boolean = true,
    onUserClick: (Search.SearchUserElement) -> Unit,
) {
    DI.get<SearchUsersSettingsView>().create(potentialMembersViewModel, shouldScroll, onUserClick)
}

class SearchUsersSettingsViewImpl : SearchUsersSettingsView {
    @Composable
    override fun create(
        potentialMembersViewModel: PotentialMembersViewModel,
        shouldScroll: Boolean,
        onUserClick: (Search.SearchUserElement) -> Unit,
    ) {
        Column {
            SearchUsersLocally(potentialMembersViewModel.searchHandler, shouldScroll, onUserClick)
        }
    }
}
