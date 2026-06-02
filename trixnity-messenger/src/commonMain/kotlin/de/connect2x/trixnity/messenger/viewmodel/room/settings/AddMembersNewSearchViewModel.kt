package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AddMembersNewSearchViewModel : AddMembersViewModel {
    val potentialMembersNewSearchViewModel: PotentialMembersNewSearchViewModel
    val groupUsersNewSearch: StateFlow<List<UserSearchResult>>

    fun onUserClick(user: UserSearchResult)

    fun removeUserFromList(user: UserSearchResult)

    fun removeUserFromGroup(user: UserSearchResult)

    fun addUserToList(user: UserSearchResult)
}

class AddMembersNewSearchViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    addMembersViewModel: AddMembersViewModel,
    override val potentialMembersNewSearchViewModel: PotentialMembersNewSearchViewModel,
) :
    MatrixClientViewModelContext by viewModelContext,
    AddMembersViewModel by addMembersViewModel,
    AddMembersNewSearchViewModel {

    private val _groupUsersNewSearch = MutableStateFlow<List<UserSearchResult>>(emptyList())
    override val groupUsersNewSearch: StateFlow<List<UserSearchResult>> = _groupUsersNewSearch.asStateFlow()

    override fun onUserClick(user: UserSearchResult) {
        removeUserFromList(user)
    }

    override fun removeUserFromList(user: UserSearchResult) {
        _groupUsersNewSearch.value += user
        potentialMembersNewSearchViewModel.userSearchViewModel.filterUserSearchResult(user)
    }

    override fun removeUserFromGroup(user: UserSearchResult) {
        addUserToList(user)
    }

    override fun addUserToList(user: UserSearchResult) {
        _groupUsersNewSearch.value -= user
        potentialMembersNewSearchViewModel.userSearchViewModel.unfilterUserSearchResult(user)
    }
}
