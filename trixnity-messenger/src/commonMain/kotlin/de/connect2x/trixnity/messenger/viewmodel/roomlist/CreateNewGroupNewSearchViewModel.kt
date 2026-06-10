package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.trixnity.messenger.search.user.UserSearchResult
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Create a new group, using the new search.
 *
 * **Attention**: Be aware to not use properties or methods from [CreateNewGroupViewModel] that reference
 * [de.connect2x.trixnity.messenger.util.Search.SearchUserElement]. The replacements of those are using
 * [UserSearchResult].
 */
interface CreateNewGroupNewSearchViewModel : CreateNewGroupViewModel {
    val userSearchViewModel: UserSearchViewModel
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
    override val userSearchViewModel: UserSearchViewModel =
        createNewGroupViewModel.createNewRoomViewModel.userSearchViewModel

    private val _groupUsersNewSearch = MutableStateFlow<List<UserSearchResult>>(emptyList())
    override val groupUsersNewSearch: StateFlow<List<UserSearchResult>> = _groupUsersNewSearch.asStateFlow()

    override fun onUserClick(user: UserSearchResult) {
        removeUserFromList(user)
    }

    override fun removeUserFromList(user: UserSearchResult) {
        _groupUsersNewSearch.value += user
        userSearchViewModel.filterNotSearchResult(user)
    }

    override fun removeUserFromGroup(user: UserSearchResult) {
        addUserToList(user)
    }

    override fun addUserToList(user: UserSearchResult) {
        _groupUsersNewSearch.value -= user
        userSearchViewModel.unfilterNotSearchResult(user)
    }
}
