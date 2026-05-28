package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.search.SearchUserViewModel
import de.connect2x.trixnity.messenger.viewmodel.search.SearchUserViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchResult
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.get

interface PotentialMembersNewSearchViewModel {
    val searchUserViewModel: SearchUserViewModel
    val selectedUsersNewSearch: StateFlow<List<UserSearchResult>>
}

class PotentialMembersNewSearchViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    potentialMembersViewModel: PotentialMembersViewModel,
) :
    MatrixClientViewModelContext by viewModelContext,
    PotentialMembersViewModel by potentialMembersViewModel,
    PotentialMembersNewSearchViewModel {

    override val searchUserViewModel: SearchUserViewModel = get<SearchUserViewModelFactory>().create(viewModelContext)
    override val selectedUsersNewSearch: StateFlow<List<UserSearchResult>> = searchUserViewModel.searchResultList
}
