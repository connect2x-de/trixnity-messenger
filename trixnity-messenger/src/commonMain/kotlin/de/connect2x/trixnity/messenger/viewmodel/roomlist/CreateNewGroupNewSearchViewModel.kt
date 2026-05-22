package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.search.SearchUserViewModel
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface CreateNewGroupNewSearchViewModel : CreateNewGroupViewModel {
    val searchUserViewModel: SearchUserViewModel
    val groupUsersNewSearch: StateFlow<List<UserSearchResult>>

    fun onUserClick(user: UserSearchResult)

    fun removeUserFromList(user: UserSearchResult)

    fun removeUserFromGroup(user: UserSearchResult)

    fun addUserToList(user: UserSearchResult)
}

class CreateNewGroupNewSearchViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    createNewGroupViewModel: CreateNewGroupViewModel,
) :
    CreateNewGroupNewSearchViewModel,
    CreateNewGroupViewModel by createNewGroupViewModel,
    ViewModelContext by viewModelContext {
    override val searchUserViewModel: SearchUserViewModel =
        createNewGroupViewModel.createNewRoomViewModel.searchUserViewModel

    private val _groupUsersNewSearch = MutableStateFlow<List<UserSearchResult>>(emptyList())
    override val groupUsersNewSearch: StateFlow<List<UserSearchResult>> = _groupUsersNewSearch.asStateFlow()

    override fun onUserClick(user: UserSearchResult) {
        removeUserFromList(user)
    }

    override fun removeUserFromList(user: UserSearchResult) {
        _groupUsersNewSearch.value += user
        searchUserViewModel.filterUserSearchResult(user)
    }

    override fun removeUserFromGroup(user: UserSearchResult) {
        addUserToList(user)
    }

    override fun addUserToList(user: UserSearchResult) {
        _groupUsersNewSearch.value -= user
        searchUserViewModel.unfilterUserSearchResult(user)
    }
}
