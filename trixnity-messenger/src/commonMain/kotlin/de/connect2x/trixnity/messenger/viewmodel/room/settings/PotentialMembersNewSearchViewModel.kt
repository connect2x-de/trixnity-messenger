package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.search.user.UserSearchContext
import de.connect2x.trixnity.messenger.search.user.UserSearchResult
import de.connect2x.trixnity.messenger.util.nonInvitableMemberIds
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.search.SearchViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.get

interface PotentialMembersNewSearchViewModel {
    val userSearchViewModel: UserSearchViewModel
    val selectedUsersNewSearch: StateFlow<List<UserSearchResult>>
}

class PotentialMembersNewSearchViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    roomId: RoomId,
    potentialMembersViewModel: PotentialMembersViewModel,
) :
    MatrixClientViewModelContext by viewModelContext,
    PotentialMembersViewModel by potentialMembersViewModel,
    PotentialMembersNewSearchViewModel {

    override val userSearchViewModel: UserSearchViewModel =
        ExcludeUsersSearchViewModel(
            delegate =
                get<UserSearchViewModelFactory>()
                    .create(
                        viewModelContext,
                        get<SearchViewModelFactory>()
                            .create(viewModelContext, UserSearchContext(activeAccount = matrixClient.userId)),
                    ),
            excludedUserIds = nonInvitableMemberIds(matrixClient, roomId),
            coroutineScope = coroutineScope,
        )
    override val selectedUsersNewSearch: StateFlow<List<UserSearchResult>> = userSearchViewModel.searchResultList
}

private class ExcludeUsersSearchViewModel(
    delegate: UserSearchViewModel,
    excludedUserIds: Flow<Set<UserId>>,
    coroutineScope: CoroutineScope,
) : UserSearchViewModel by delegate {
    override val searchResultList: StateFlow<List<UserSearchResult>> =
        combine(delegate.searchResultList, excludedUserIds, ::excludeUsers)
            .stateIn(
                coroutineScope,
                SharingStarted.WhileSubscribed(),
                excludeUsers(delegate.searchResultList.value, emptySet()),
            )

    override val noResultsFound: StateFlow<Boolean?> =
        combine(delegate.noResultsFound, delegate.searchResultList, searchResultList, ::noResultsFoundAfterExclusion)
            .stateIn(
                coroutineScope,
                SharingStarted.WhileSubscribed(),
                noResultsFoundAfterExclusion(
                    delegate.noResultsFound.value,
                    delegate.searchResultList.value,
                    searchResultList.value,
                ),
            )
}

private fun excludeUsers(searchResults: List<UserSearchResult>, excludedUserIds: Set<UserId>): List<UserSearchResult> {
    return searchResults.filterNot { it.userId in excludedUserIds }
}

private fun noResultsFoundAfterExclusion(
    noResultsFound: Boolean?,
    unfilteredResults: List<UserSearchResult>,
    filteredResults: List<UserSearchResult>,
): Boolean? {
    if (noResultsFound != false) return noResultsFound
    return unfilteredResults.isNotEmpty() && filteredResults.isEmpty()
}
