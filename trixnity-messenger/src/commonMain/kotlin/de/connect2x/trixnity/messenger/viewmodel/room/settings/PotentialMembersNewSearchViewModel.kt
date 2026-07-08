package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.search.user.UserSearchContext
import de.connect2x.trixnity.messenger.search.user.UserSearchResult
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.search.SearchViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchViewModelFactory
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.get

interface PotentialMembersNewSearchViewModel {
    val userSearchViewModel: UserSearchViewModel
    val selectedUsersNewSearch: StateFlow<List<UserSearchResult>>
}

class PotentialMembersNewSearchViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    potentialMembersViewModel: PotentialMembersViewModel,
) :
    MatrixClientViewModelContext by viewModelContext,
    PotentialMembersViewModel by potentialMembersViewModel,
    PotentialMembersNewSearchViewModel {

    override val userSearchViewModel: UserSearchViewModel =
        get<UserSearchViewModelFactory>()
            .create(
                viewModelContext,
                get<SearchViewModelFactory>().create(viewModelContext) {
                    UserSearchContext(activeAccount = matrixClient.userId)
                },
            )
    override val selectedUsersNewSearch: StateFlow<List<UserSearchResult>> = userSearchViewModel.searchResultList
}
